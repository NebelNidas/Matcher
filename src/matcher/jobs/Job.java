package matcher.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;

public class Job<T> implements Runnable {
	public Job(String id, Function<DoubleConsumer, T> action) {
		this.id = id;
		setAction(action);
	}

	protected void setAction(Function<DoubleConsumer, T> action) {
		this.action = action;
	}

	/**
	 * Every time this job's progress changes, the double consumer gets invoked.
	 * Progress is a value between -∞ and 1, where negative values indicate an uncertain runtime.
	 */
	public void addProgressListener(Consumer<Double> progressListener) {
		progressListeners.add(progressListener);
	}

	public void addOnCancel(Runnable onCancel) {
		this.onCancelListeners.add(onCancel);
	}

	public void addOnError(Consumer<Throwable> onError) {
		this.onErrorListeners.add(onError);
	}

	public void addOnSuccess(Consumer<T> onSuccess) {
		this.onSuccessListeners.add(onSuccess);
	}

	/**
	 * Gets called once this job is finished. This doesn't guarantee a specific state,
	 * it can be cancelled, errored or finished successfully.
	 */
	public void addOnFinish(Runnable onFinish) {
		this.onFinishListeners.add(onFinish);
	}

	/**
	 * Add IDs of other jobs which must be executed before this job can be started.
	 */
	public void addBlockedBy(String... blockingJobIds) {
		this.blockingJobIds.addAll(Arrays.asList(blockingJobIds));
	}

	protected void andThen(Job<?> job) {

	}

	void setParent(JobGroup<?> parent) {
		this.parent = parent;
	}

	/**
	 * Queues this job for execution.
	 * If called on a JobGroup's child, executes it directly.
	 */
	@Override
	public void run() {
		state = JobState.QUEUED;

		if (parent == null) {
			JobManager.queue(this);
		} else {
			runNow();
		}
	}

	void runNow() {
		state = JobState.RUNNING;

		if (action != null) {
			try {
				result = action.apply((progress) -> onProgressChange(progress));
			} catch (Exception e) {
				onError(e);
				return;
			}
		}

		onSuccess();
	}

	public void cancel() {
		onCancel();
	}

	protected void onCancel() {
		state = JobState.CANCELING;
		onCancelListeners.forEach(listener -> listener.run());
		onFinish();
	}

	protected void onProgressChange(double progress) {
		this.progress = progress;
		progressListeners.forEach(listener -> listener.accept(progress));
	}

	protected void onError(Throwable error) {
		state = JobState.ERRORED;
		onErrorListeners.forEach(listener -> listener.accept(error));
		onFinish();
	}

	protected void onSuccess() {
		state = JobState.SUCCEEDED;
		onSuccessListeners.forEach(listener -> listener.accept(result));
		onFinish();
	}

	protected void onFinish() {
		onFinishListeners.forEach(listener -> listener.run());
	}

	public String getId() {
		return id;
	}

	public JobGroup<?> getParent() {
		return parent;
	}

	public double getProgress() {
		return progress;
	}

	public JobState getState() {
		return state;
	}

	public boolean isBlockedBy(String jobId) {
		return blockingJobIds.contains(jobId);
	}

	protected final String id;
	private Function<DoubleConsumer, T> action;
	private T result;
	protected JobGroup<?> parent;
	protected double progress = -1;
	protected JobState state = JobState.CREATED;
	protected List<Runnable> onCancelListeners = new ArrayList<>(2);
	protected List<Consumer<T>> onSuccessListeners = new ArrayList<>(2);
	protected List<Consumer<Throwable>> onErrorListeners = new ArrayList<>(2);
	protected List<Runnable> onFinishListeners = new ArrayList<>(2);
	protected List<Consumer<Double>> progressListeners = new ArrayList<>(2);
	protected List<String> blockingJobIds = new ArrayList<>(4);
}
