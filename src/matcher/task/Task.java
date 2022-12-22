package matcher.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;

public class Task<T> implements Runnable {
	public Task(String id, Function<DoubleConsumer, T> action) {
		this.id = id;

		setAction(action);
	}

	protected void setAction(Function<DoubleConsumer, T> action) {
		this.action = action;
	}

	/**
	 * Every time this task's progress changes, the double consumer gets invoked.
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
	 * Gets called once this task is finished. This doesn't guarantee a specific state,
	 * it can be cancelled, errored or finished successfully.
	 */
	public void addOnFinish(Runnable onFinish) {
		this.onFinishListeners.add(onFinish);
	}

	/**
	 * Add IDs of other tasks which must be executed before this task can be started.
	 */
	public void addBlockedBy(String... blockingTaskIds) {
		this.blockingTaskIds.addAll(Arrays.asList(blockingTaskIds));
	}

	void setParent(TaskGroup<?> parent) {
		this.parent = parent;
	}

	/**
	 * Queues this task for execution.
	 * If called on a TaskGroup's child, executes it directly.
	 */
	@Override
	public void run() {
		state = TaskState.QUEUED;

		if (parent == null) {
			TaskManager.queue(this);
		} else {
			runNow();
		}
	}

	void runNow() {
		state = TaskState.RUNNING;

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
		state = TaskState.CANCELING;
		onCancelListeners.forEach(listener -> listener.run());
		onFinish();
	}

	protected void onProgressChange(double progress) {
		this.progress = progress;
		progressListeners.forEach(listener -> listener.accept(progress));
	}

	protected void onError(Throwable error) {
		state = TaskState.ERRORED;
		onErrorListeners.forEach(listener -> listener.accept(error));
		onFinish();
	}

	protected void onSuccess() {
		state = TaskState.SUCCEEDED;
		onSuccessListeners.forEach(listener -> listener.accept(result));
		onFinish();
	}

	protected void onFinish() {
		onFinishListeners.forEach(listener -> listener.run());
	}

	public String getId() {
		return id;
	}

	public TaskGroup<?> getParent() {
		return parent;
	}

	public double getProgress() {
		return progress;
	}

	public TaskState getState() {
		return state;
	}

	public boolean isBlockedBy(String taskId) {
		return blockingTaskIds.contains(taskId);
	}

	protected final String id;
	private Function<DoubleConsumer, T> action;
	private T result;
	protected TaskGroup<?> parent;
	protected double progress = -1;
	protected TaskState state = TaskState.CREATED;
	protected List<Runnable> onCancelListeners = new ArrayList<>(2);
	protected List<Consumer<T>> onSuccessListeners = new ArrayList<>(2);
	protected List<Consumer<Throwable>> onErrorListeners = new ArrayList<>(2);
	protected List<Runnable> onFinishListeners = new ArrayList<>(2);
	protected List<Consumer<Double>> progressListeners = new ArrayList<>(2);
	protected List<String> blockingTaskIds = new ArrayList<>(4);
}
