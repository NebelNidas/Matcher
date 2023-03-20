package job4j;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public abstract class Job<T> implements Runnable {
	private final String id;
	private volatile T result;
	private volatile Throwable error;
	private volatile boolean printStackTraceOnError = true;
	private volatile List<Job<?>> subJobs = Collections.synchronizedList(new ArrayList<>());
	private volatile Thread currentThread;
	protected volatile Job<?> parent;
	protected volatile double ownProgress = 0;
	protected volatile double overallProgress = 0;
	protected volatile JobState state = JobState.CREATED;
	protected volatile List<Consumer<Job<?>>> subJobAddedListeners = Collections.synchronizedList(new ArrayList<>());
	protected volatile List<DoubleConsumer> progressListeners = Collections.synchronizedList(new ArrayList<>());
	protected volatile List<Runnable> cancelListeners = Collections.synchronizedList(new ArrayList<>());
	protected volatile List<BiConsumer<Optional<T>, Optional<Throwable>>> completionListeners = Collections.synchronizedList(new ArrayList<>());
	protected volatile List<String> blockingJobIds = Collections.synchronizedList(new ArrayList<>());

	public Job(String id) {
		this.id = id;
	}

	/**
	 * Override this method to register any subjobs known ahead of time.
	 * Compared to the dynamic {@code addSubJob} this improves the UX
	 * by letting the users know which tasks are going to be ran ahead of time
	 * and giving more accurate progress reports.
	 */
	protected void registerSubJobs() {};

	/**
	 * The main task this job shall execute. Progress is reported on a
	 * scale from -INF to +1. If this job is only used as an empty shell
	 * for hosting subjobs, the progressReceiver doesn't have to be invoked,
	 * then this job's overall progress is automatically calculated
	 * from the individual subjobs' progresses.
	 */
	protected abstract T execute(DoubleConsumer progressReceiver);

	/**
	 * Every time a subjob is registered, the listener gets invoked with the
	 * newly added job instance.
	 */
	public void addSubJobAddedListener(Consumer<Job<?>> listener) {
		this.subJobAddedListeners.add(listener);
	}

	/**
	 * Every time this job's progress changes, the double consumer gets invoked.
	 * Progress is a value between -INF and 1, where negative values indicate an uncertain runtime.
	 */
	public void addProgressListener(DoubleConsumer listener) {
		this.progressListeners.add(listener);
	}

	/**
	 * Gets called on job cancellation. The job hasn't completed at this point in time yet,
	 * it can still run for an indefinite amount of time until it eventually does or does not
	 * react to the event.
	 */
	public void addCancelListener(Runnable listener) {
		this.cancelListeners.add(listener);
	}

	/**
	 * Gets called once the job is finished. No specific state is guaranteed,
	 * it has to be checked manually.
	 * Passes the job's computed result (may be missing or incomplete if canceled/errored early),
	 * and, if errored, the encountered exception. Errors' stacktraces are printed automatically,
	 * so it doesn't have to be done manually each time.
	 */
	public void addCompletionListener(BiConsumer<Optional<T>, Optional<Throwable>> listener) {
		this.completionListeners.add(listener);
	}

	/**
	 * Add IDs of other jobs which must be completed first.
	 */
	public void addBlockedBy(String... blockingJobIds) {
		this.blockingJobIds.addAll(Arrays.asList(blockingJobIds));
	}

	/**
	 * Parents are considered effectively final, so don't ever call this method
	 * while the job is already running. It is only exposed for situations
	 * where jobs indirectly start other jobs, so that the JobManager can
	 * group the latter ones as children of the caller jobs.
	 */
	private void setParent(Job<?> parent) {
		if (containsSubJob(parent, true)) {
			throw new IllegalArgumentException("Can't set an already added subjob as parent job!");
		}

		if (this.state.compareTo(JobState.RUNNING) >= 0) {
			throw new UnsupportedOperationException("Can't change job's parent after already having been started");
		}

		this.parent = parent;
	}

	/**
	 * Dynamically add subjobs. Please consider overriding {@code registerSubJobs}
	 * to register any subjobs known ahead of time!
	 */
	public void addSubJob(Job<?> subJob, boolean cancelsParentWhenCanceled) {
		if (hasParentJobInHierarchy(subJob)) {
			throw new IllegalArgumentException("Can't add a subjob which is already a parent job!");
		}

		subJob.setParent(this);
		subJob.addProgressListener(this::onSubJobProgressChange);
		this.subJobs.add(subJob);

		if (cancelsParentWhenCanceled) {
			subJob.addCancelListener(() -> cancel());
		}

		synchronized (this.subJobAddedListeners) {
			this.subJobAddedListeners.forEach((listener) -> listener.accept(subJob));
		}
	}

	protected void validateProgress(double progress) {
		if (progress >= 1.001) {
			throw new IllegalArgumentException("Progress has to be a value between -INF and 1!");
		}
	}

	private void onOwnProgressChange(double progress) {
		validateProgress(progress);

		if (Math.abs(progress - ownProgress) < 0.005) {
			// Avoid time consuming computations for
			// unnoticeable progress deltas
			return;
		}

		this.ownProgress = progress;
		onProgressChange();
	}

	private void onSubJobProgressChange(double progress) {
		validateProgress(progress);
		onProgressChange();
	}

	protected void onProgressChange() {
		double progress = 0;
		List<Double> progresses = new ArrayList<>(subJobs.size() + 1);

		synchronized (this.subJobs) {
			for (Job<?> job : this.subJobs) {
				progresses.add(job.getProgress());
			}
		}

		if (ownProgress	> 0) {
			// Don't use own progress if it's never been set.
			// This happens if the current job is only used as an
			// empty shell for hosting subjobs.
			progresses.add(ownProgress);
		}

		for (double value : progresses) {
			if (value < 0) {
				progress = -1;
				break;
			} else {
				if (value >= 1.001) {
					throw new IllegalArgumentException("Progress has to be a value between -INF and 1!");
				}

				progress += value / progresses.size();
			}
		}

		this.overallProgress = Math.min(1.0, progress);

		synchronized (this.progressListeners) {
			this.progressListeners.forEach(listener -> listener.accept(this.overallProgress));
		}
	}

	/**
	 * {@return an unmodifiable view of the subjob list}.
	 */
	public List<Job<?>> getSubJobs() {
		return Collections.unmodifiableList(this.subJobs);
	}

	public void dontPrintStacktraceOnError() {
		printStackTraceOnError = false;
	}

	/**
	 * Queues the job for execution.
	 * If called on a subjob, executes it directly.
	 */
	public void run() {
		if (this.state.compareTo(JobState.QUEUED) > 0) {
			// Already running/finished
			return;
		}

		this.state = JobState.QUEUED;

		if (this.parent == null) {
			// This job is an orphan / top-level job.
			// It will be executed on its own thread,
			// managed by the JobManager.
			JobManager.get().queue(this);
		} else {
			// This is a subjob. Subjobs get executed
			// synchronously directly on the parent thread.
			runNow();
		}
	}

	/**
	 * Queues the job for execution, waits for it to get scheduled,
	 * executes the job and then returns the result and/or error.
	 * This is basically the synchronous version of registering a
	 * CompletionListener.
	 */
	public SimpleEntry<Optional<T>, Optional<Throwable>> runAndAwait() {
		if (this.state.compareTo(JobState.QUEUED) > 0) {
			// Already running/finished
			return new SimpleEntry<>(Optional.ofNullable(null), Optional.ofNullable(null));
		}

		run();

		while (!this.state.isFinished()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// ignored
			}
		}

		return new SimpleEntry<>(Optional.ofNullable(result), Optional.ofNullable(error));
	}

	void runNow() {
		if (this.state.compareTo(JobState.QUEUED) > 0) {
			// Already running/finished
			return;
		}

		currentThread = Thread.currentThread();
		this.state = JobState.RUNNING;
		registerSubJobs();

		try {
			this.result = execute(this::onOwnProgressChange);
		} catch (Exception e) {
			onError(e);
			return;
		}

		switch (this.state) {
			case CANCELING:
				onCanceled();
				break;
			case RUNNING:
				onSuccess();
				break;
			default:
				throw new IllegalStateException("Job finished running but isn't in a valid state!");
		}
	}

	Thread getThread() {
		return currentThread;
	}

	public void cancel() {
		if (this.state != JobState.CANCELING && !this.state.isFinished()) {
			onCancel();
		}
	}

	protected void onCancel() {
		JobState previousState = this.state;
		this.state = JobState.CANCELING;

		synchronized (this.cancelListeners) {
			this.cancelListeners.forEach(listener -> listener.run());
		}

		synchronized (this.subJobs) {
			this.subJobs.forEach(job -> job.cancel());
		}

		if (previousState.compareTo(JobState.RUNNING) < 0) {
			onCanceled();
		}
	}

	protected void onCanceled() {
		this.state = JobState.CANCELED;
		onFinish();
	}

	protected void onError(Throwable error) {
		state = JobState.ERRORED;
		this.error = error;

		if (printStackTraceOnError) {
			this.error.printStackTrace();
		}

		onFinish();
	}

	protected void onSuccess() {
		this.state = JobState.SUCCEEDED;
		onFinish();
	}

	protected void onFinish() {
		onOwnProgressChange(1);

		synchronized (this.completionListeners) {
			this.completionListeners.forEach(listener -> listener.accept(Optional.ofNullable(result), Optional.ofNullable(error)));
		}
	}

	public String getId() {
		return this.id;
	}

	public Job<?> getParent() {
		return this.parent;
	}

	public double getProgress() {
		return this.overallProgress;
	}

	public JobState getState() {
		return this.state;
	}

	public boolean isBlockedBy(String jobId) {
		boolean blocked = this.blockingJobIds.contains(jobId);

		if (blocked) return true;

		synchronized (this.subJobs) {
			return this.subJobs.parallelStream()
					.filter(job -> job.isBlockedBy(jobId))
					.findAny()
					.isPresent();
		}
	}

	boolean containsSubJob(Job<?> subJob, boolean recursive) {
		boolean contains = this.subJobs.contains(subJob);

		if (contains || !recursive) return contains;

		synchronized (this.subJobs) {
			return subJobs.parallelStream()
					.filter(nestedSubJob -> nestedSubJob.containsSubJob(subJob, true))
					.findAny()
					.isPresent();
		}
	}

	boolean hasParentJobInHierarchy(Job<?> job) {
		if (parent == null) return false;

		return job == parent || parent.hasParentJobInHierarchy(job);
	}
}
