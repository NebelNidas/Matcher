package job4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import job4j.JobSettings.MutableJobSettings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import matcher.Util;

public abstract class Job<T> implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(Job.class);
	static final ExecutorService TIMEOUT_THREAD_POOL = Executors.newCachedThreadPool();
	private final String id;
	private final JobCategory category;
	private final MutableJobSettings settings = new MutableJobSettings();
	private volatile T result;
	private volatile Throwable error;
	private final List<Job<?>> subJobs = Collections.synchronizedList(new ArrayList<>());
	private volatile Thread thread;
	protected volatile Job<?> parent;
	protected volatile double ownProgress = 0;
	protected volatile double overallProgress = 0;
	protected volatile JobState state = JobState.CREATED;
	protected final List<Consumer<Job<?>>> subJobAddedListeners = Collections.synchronizedList(new ArrayList<>());
	protected final List<DoubleConsumer> progressListeners = Collections.synchronizedList(new ArrayList<>());
	protected final List<Runnable> cancelListeners = Collections.synchronizedList(new ArrayList<>());
	protected final List<BiConsumer<Optional<T>, Optional<Throwable>>> finishListeners = Collections.synchronizedList(new ArrayList<>());
	protected final List<JobCategory> blockingJobCategories = Collections.synchronizedList(new ArrayList<>());

	public Job(JobCategory category) {
		this(category, null);
	}

	public Job(JobCategory category, String idAppendix) {
		this.category = category;
		this.id = category.getId() + (idAppendix == null ? "" : ":" + idAppendix);

		changeDefaultSettings(settings);
	}


	//======================================================================================
	// Overridable methods
	//======================================================================================

	/**
	 * Override this method to modify the job's default settings.
	 * Make changes directly on the passed {@link #settings} object.
	 */
	protected void changeDefaultSettings(MutableJobSettings settings) {}

	/**
	 * Override this method to register any subjobs known ahead of time.
	 * Compared to the dynamic {@link #addSubJob} this improves the UX
	 * by letting the users know which tasks are going to be ran ahead of time
	 * and giving more accurate progress reports.
	 */
	protected void registerSubJobs() {}

	/**
	 * The main task this job shall execute. Progress is reported on a
	 * scale from -INF to +1. If this job is only used as an empty shell
	 * for hosting subjobs, the progressReceiver doesn't have to be invoked,
	 * then this job's overall progress is automatically calculated
	 * from the individual subjobs' progresses.
	 */
	protected abstract T execute(DoubleConsumer progressReceiver);


	//======================================================================================
	// Listener registration
	//======================================================================================

	/**
	 * Every time a subjob is registered, the listener gets invoked with the
	 * newly added job instance.
	 */
	public void addSubJobAddedListener(Consumer<Job<?>> listener) {
		if (state.isFinished()) {
			throw new RuntimeException("Can't add subjobAddedListener to a job which has already finished!");
		}

		subJobAddedListeners.add(listener);
	}

	/**
	 * Every time this job's progress changes, the double consumer gets invoked.
	 * Progress is a value between -INF and 1, where negative values indicate an uncertain runtime.
	 */
	public void addProgressListener(DoubleConsumer listener) {
		progressListeners.add(listener);

		if (state.isFinished()) {
			listener.accept(overallProgress);
		}
	}

	/**
	 * Gets called on job cancellation. The job hasn't finished at this point in time yet,
	 * it can still run for an indefinite amount of time until it eventually does or does not
	 * react to the event.
	 */
	public void addCancelListener(Runnable listener) {
		if (state.isFinished()) {
			throw new RuntimeException("Can't add cancelListener to a job which has already finished!");
		}

		cancelListeners.add(listener);
	}

	/**
	 * Gets called once the job is finished. No specific state is guaranteed,
	 * it has to be checked manually.
	 * Passes the job's computed result (may be missing or incomplete if canceled/errored early),
	 * and, if errored, the encountered exception. Errors' stacktraces are printed automatically,
	 * so it doesn't have to be done manually each time.
	 */
	public void addFinishListener(BiConsumer<Optional<T>, Optional<Throwable>> listener) {
		finishListeners.add(listener);

		if (state.isFinished()) {
			listener.accept(Optional.ofNullable(result), Optional.ofNullable(error));
		}
	}


	//======================================================================================
	// User-definable configuration
	//======================================================================================

	/**
	 * Add IDs of other jobs which must be finished first.
	 */
	public void addBlockedBy(JobCategory... blockingJobCategories) {
		this.blockingJobCategories.addAll(Arrays.asList(blockingJobCategories));
	}


	//======================================================================================
	// Hierarchy modification
	//======================================================================================

	/**
	 * Dynamically add subjobs. Override {@link #registerSubJobs}
	 * to register any subjobs known ahead of time.
	 */
	public void addSubJob(Job<?> subJob, boolean cancelsParentWhenCanceledOrErrored) {
		if (hasParentJobInHierarchy(subJob)) {
			throw new IllegalArgumentException("Can't add a subjob which is already a parent job!");
		}

		subJob.setParent(this);
		subJob.addProgressListener(this::onSubJobProgressChange);
		subJobs.add(subJob);

		if (cancelsParentWhenCanceledOrErrored) {
			subJob.addCancelListener(() -> cancel(BuiltinJobCancellationReasons.PARENT_CANCELLATION));
			subJob.addFinishListener((subJobResult, subJobError) -> {
				subJobError.ifPresent(this::onError);
			});
		}

		List.copyOf(subJobAddedListeners).forEach((listener) -> listener.accept(subJob));
	}

	/**
	 * Parents are considered effectively final, so don't ever call this method
	 * while the job is already running. It is only exposed for situations
	 * where jobs indirectly start other jobs, so that the latter ones can
	 * be turned into direct children of the caller jobs.
	 */
	private void setParent(Job<?> parent) {
		if (containsSubJob(parent, true)) {
			throw new IllegalArgumentException("Can't set an already added subjob as parent job!");
		}

		if (state.compareTo(JobState.RUNNING) >= 0) {
			throw new UnsupportedOperationException("Can't change job's parent after already having been started");
		}

		this.parent = parent;
	}


	//======================================================================================
	// Lifecycle
	//======================================================================================

	/**
	 * Queues the job for execution, and, if it is a subjob, runs it on the current thread.
	 */
	public void run() {
		if (state.compareTo(JobState.CREATED) > 0
				&& state != JobState.CANCELING
				&& state != JobState.CANCELED) {
			throw new RuntimeException("Can't run a job which is already " + state + "!");
		}

		JobManager.get().queue(this);
	}

	public void runAsync() {
		if (state.compareTo(JobState.CREATED) > 0
				&& state != JobState.CANCELING
				&& state != JobState.CANCELED) {
			throw new RuntimeException("Can't run a job which is already " + state + "!");
		}

		new Thread(this, "AsyncJobRunner-" + id).start();
	}

	/**
	 * Queues the job for execution, waits for it to get scheduled,
	 * executes the job and then returns the result and/or error.
	 */
	public JobResult<T> runAndAwait() {
		return runAndAwait(0, null, false);
	}

	/**
	 * Queues the job for execution, waits for it to get scheduled,
	 * executes the job and then returns the result and/or error.
	 *
	 * <p>If a timeout is specified, it overwrites the job settings' timeout.
	 *
	 * @param timeout The maximum time to wait for the job to finish. Use 0 for infinite.
	 * @param unit The time unit of the timeout argument. May be null if timeout is 0.
	 * @param cancelOnTimeout Whether to cancel the job when the timeout is reached.
	 * @return The job result if it finished in time, null if the timeout was reached and cancelOnTimeout is false.
	 */
	public @Nullable JobResult<T> runAndAwait(long timeout, TimeUnit unit, boolean cancelOnTimeout) {
		if (state.compareTo(JobState.CREATED) > 0
				&& state != JobState.CANCELING
				&& state != JobState.CANCELED) {
			throw new RuntimeException("Can't run a job which is already " + state + "!");
		}

		if (timeout >= 0 && cancelOnTimeout) {
			settings.setTimeoutNanos(timeout == 0 ? 0 : unit.toNanos(timeout));
		}

		if (state == JobState.CREATED) {
			JobManager.get().queue(this);
		}

		if (!state.isFinished() && timeout > 0 && !cancelOnTimeout) {
			long timeoutNanos = unit.toNanos(timeout);
			long startTime = System.nanoTime();
			long remainingTime = timeoutNanos;

			do {
				LockSupport.parkNanos(this, remainingTime);
				long elapsed = System.nanoTime() - startTime;
				remainingTime = timeoutNanos - elapsed;
			} while (remainingTime > 0 && state == JobState.RUNNING);

			return null;
		}

		return await();
	}

	public JobResult<T> await() {
		Object monitor = new Object();

		if (!state.isFinished()) {
			Thread thread = Thread.currentThread();
			addFinishListener((result, error) -> {
				synchronized (monitor) {
					monitor.notifyAll();
				}
			});
		}

		while (!state.isFinished()) {
			try {
				synchronized (monitor) {
					monitor.wait();
				}
			} catch (InterruptedException e) {
				cancel(BuiltinJobCancellationReasons.INTERRUPTED);
			}
		}

		return new JobResult<>(result, error);
	}

	void runOnCurrentThread() {
		if (state.compareTo(JobState.QUEUED) > 0
				&& state != JobState.CANCELING
				&& state != JobState.CANCELED) {
			throw new RuntimeException("Can't run a job which is already " + state + "!");
		}

		assert state == JobState.QUEUED;

		thread = Thread.currentThread();
		state = JobState.RUNNING;
		registerSubJobs();

		if (settings.getTimeoutNanos() > 0) {
			TIMEOUT_THREAD_POOL.submit(() -> {
				long timeoutNanos = settings.getTimeoutNanos();
				long startTime = System.nanoTime();
				long remainingTime = timeoutNanos;

				do {
					LockSupport.parkNanos(this, remainingTime);
					long elapsed = System.nanoTime() - startTime;
					remainingTime = timeoutNanos - elapsed;
				} while (remainingTime > 0 && state == JobState.RUNNING);

				if (state == JobState.RUNNING) {
					cancel(BuiltinJobCancellationReasons.TIMEOUT);
				}
			});
		}

		try {
			result = execute(this::onOwnProgressChange);
		} catch (Exception e) {
			onError(e);
		}

		switch (state) {
			case RUNNING:
				onSuccess();
				break;
			case CANCELING:
				onCanceled();
				break;
			case ERRORED:
				break;
			default:
				throw new IllegalStateException("Job finished running but isn't in a valid state!");
		}
	}

	private void onOwnProgressChange(double progress) {
		validateProgress(progress);

		if (progress < 1f - Util.floatError && Math.abs(progress - ownProgress) < 0.005) {
			// Avoid time consuming computations for
			// unnoticeable progress deltas (<0.5%)
			return;
		}

		ownProgress = progress;
		onProgressChange();
	}

	private void onSubJobProgressChange(double progress) {
		validateProgress(progress);
		onProgressChange();
	}

	protected void validateProgress(double progress) {
		if (progress > 1f + Util.floatError) {
			throw new IllegalArgumentException("Progress has to be a value between -INF and 1, but was %s!".formatted(progress));
		}
	}

	protected void onProgressChange() {
		double progress = 0;
		List<Double> progresses;

		if (ownProgress < 0 - Util.floatError || ownProgress > 0 + Util.floatError) {
			// Own progress has been set. This overrides the automatic
			// progress calculation dependent on subjob progress.
			progresses = List.of(ownProgress);
		} else {
			// Don't use own progress if it's never been set.
			// This happens if the current job is only used as an
			// empty shell for hosting subjobs.
			progresses = new ArrayList<>(subJobs.size());

			for (Job<?> job : List.copyOf(subJobs)) {
				progresses.add(job.getProgress());
			}
		}

		for (double value : progresses) {
			if (value < 0) {
				progress = -1;
				break;
			} else {
				validateProgress(value);
				progress += value / progresses.size();
			}
		}

		this.overallProgress = Math.min(1.0, progress);
		List.copyOf(progressListeners).forEach(listener -> listener.accept(this.overallProgress));
	}

	public boolean cancel(String reason) {
		if (state != JobState.CANCELING && !state.isFinished()) {
			onCancel();
			return true;
		}

		return false;
	}

	protected void onCancel() {
		JobState previousState = state;
		state = JobState.CANCELING;

		List.copyOf(cancelListeners).forEach(Runnable::run);
		List.copyOf(subJobs).forEach((subJob) -> subJob.cancel(BuiltinJobCancellationReasons.PARENT_CANCELLATION));

		if (previousState.compareTo(JobState.RUNNING) < 0) {
			onCanceled();
		}
	}

	protected void onCanceled() {
		state = JobState.CANCELED;
		onFinish();
	}

	protected void onError(Throwable error) {
		this.state = JobState.ERRORED;
		this.error = error;

		if (settings.isPrintStackTraceOnError() && !JobManager.get().isShuttingDown()) {
			LOGGER.error("An exception has been encountered in job '{}'", id, error);
		}

		List.copyOf(subJobs).forEach((subJob) -> subJob.cancel(BuiltinJobCancellationReasons.PARENT_ERROR));

		onFinish();
	}

	protected void onSuccess() {
		state = JobState.SUCCEEDED;
		onFinish();
	}

	protected void onFinish() {
		onOwnProgressChange(1);

		List.copyOf(finishListeners).forEach(listener -> listener.accept(Optional.ofNullable(result), Optional.ofNullable(error)));
	}


	//======================================================================================
	// Getters & Checkers
	//======================================================================================

	Thread getThread() {
		return thread;
	}

	public String getId() {
		return id;
	}

	public JobCategory getCategory() {
		return category;
	}

	public Job<?> getParent() {
		return parent;
	}

	public double getProgress() {
		return overallProgress;
	}

	public JobState getState() {
		return state;
	}

	public JobSettings getSettings() {
		return settings.getImmutable();
	}

	/**
	 * {@return an unmodifiable list of subjobs}.
	 */
	public List<Job<?>> getSubJobs(boolean recursive) {
		if (!recursive) {
			return Collections.unmodifiableList(subJobs);
		}

		List<Job<?>> subjobs = List.copyOf(subJobs);
		List<Job<?>> subjobsRecursive = new ArrayList<>(subjobs);

		for (Job<?> subjob : subjobs) {
			subjobsRecursive.addAll(subjob.getSubJobs(true));
		}

		return Collections.unmodifiableList(subjobsRecursive);
	}

	public boolean hasSubJob(String id, boolean recursive) {
		List<Job<?>> subjobs = List.copyOf(subJobs);
		boolean hasSubJob = false;

		for (Job<?> subjob : subjobs) {
			if (subjob.getId().equals(id)) {
				hasSubJob = true;
				break;
			}
		}

		if (!recursive) return hasSubJob;

		for (Job<?> subjob : subjobs) {
			if (subjob.hasSubJob(id, true)) {
				hasSubJob = true;
				break;
			}
		}

		return hasSubJob;
	}

	/**
	 * Checks if this job or any of its subjobs are
	 * blocked by the passed job category.
	 */
	public boolean isBlockedBy(JobCategory category) {
		boolean blocked = blockingJobCategories.contains(category);

		if (blocked) return true;

		blocked = List.copyOf(blockingJobCategories).stream()
				.anyMatch(category::hasParent);

		if (blocked) return true;

		return List.copyOf(subJobs).stream()
				.anyMatch(job -> job.isBlockedBy(category));
	}

	public boolean containsSubJob(Job<?> subJob, boolean recursive) {
		boolean contains = subJobs.contains(subJob);

		if (contains || !recursive) return contains;

		return List.copyOf(subJobs).stream()
				.anyMatch(nestedSubJob -> nestedSubJob.containsSubJob(subJob, true));
	}

	public boolean hasParentJobInHierarchy(Job<?> job) {
		if (parent == null) return false;

		return job == parent || parent.hasParentJobInHierarchy(job);
	}


	//======================================================================================
	// Conversions
	//======================================================================================

	public interface JobFuture<V> extends Future<V> {
		Job<V> getUnderlyingJob();
	}

	public JobFuture<T> asFuture() {
		Job<T> job = this;

		return new JobFuture<T>() {
			@Override
			public Job<T> getUnderlyingJob() {
				return job;
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return job.cancel(BuiltinJobCancellationReasons.UNSPECIFIED);
			}

			@Override
			public boolean isCancelled() {
				return job.getState() == JobState.CANCELED;
			}

			@Override
			public boolean isDone() {
				return job.getState().isFinished();
			}

			@Override
			public T get() throws InterruptedException, ExecutionException {
				job.runAndAwait();

				if (job.error == null) {
					return job.result;
				} else if (job.error instanceof InterruptedException) {
					throw (InterruptedException) error;
				} else if (job.error instanceof ExecutionException) {
					throw (ExecutionException) error;
				} else {
					throw new ExecutionException(error);
				}
			}

			@Override
			public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				job.settings.setTimeoutNanos(unit.toSeconds(timeout));
				job.runAndAwait();

				if (job.error == null) {
					return job.result;
				} else if (job.error instanceof InterruptedException) {
					throw (InterruptedException) error;
				} else if (job.error instanceof ExecutionException) {
					throw (ExecutionException) error;
				} else if (job.error instanceof TimeoutException) {
					throw (TimeoutException) error;
				} else {
					throw new ExecutionException(error);
				}
			}
		};
	}

	@Override
	public String toString() {
		return "Job{id='" + id + "', category=" + category + ", state=" + state + "}";
	}
}
