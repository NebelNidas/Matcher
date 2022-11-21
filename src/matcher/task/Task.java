package matcher.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public class Task implements Runnable {
	public Task(String text, Consumer<DoubleConsumer> task) {
		this.text = text;
		this.task = task;
	}

	public void addOnCancel(Runnable onCancel) {
		this.onCancelListeners.add(onCancel);
	}

	public void addOnError(Consumer<Throwable> onError) {
		this.onErrorListeners.add(onError);
	}

	public void addOnSuccess(Runnable onSuccess) {
		this.onSuccessListeners.add(onSuccess);
	}

	public void addProgressListener(Consumer<Double> progressListener) {
		progressListeners.add(progressListener);
	}

	public void addChildTask(Task childTask) {
		childTask.setParent(this);
		childTasks.add(childTask);
	}

	private void setParent(Task parent) {
		this.parent = parent;
	}

	public void setBlockedBy(TaskType... blockingTypes) {
		blockingTaskTypes.addAll(Arrays.asList(blockingTypes));
	}

	public void cancel() {
		onCancelListeners.forEach(listener -> listener.run());;
	}

	private void onProgressChange(double progress) {
		progressListeners.forEach(listener -> listener.accept(progress));
	}

	@Override
	public void run() {
		TaskManager.queue(this);
	}

	void runNow() {
		task.accept((progress) -> onProgressChange(progress));
		childTasks.forEach(task -> TaskManager.queue(task));
	}

	public String getText() {
		return text;
	}

	public List<Task> getChildTasks() {
		return childTasks;
	}

	public Task getParent() {
		return parent;
	}

	public boolean isBlockedBy(TaskType taskType) {
		return blockingTaskTypes.contains(taskType);
	}

	enum TaskType {
		INIT_PROJECT,
		MATCHING_BLOCKING
	}

	private final String text;
	private final Consumer<DoubleConsumer> task;
	private List<Runnable> onCancelListeners = new ArrayList<>(2);
	private List<Runnable> onSuccessListeners = new ArrayList<>(2);
	private List<Consumer<Throwable>> onErrorListeners = new ArrayList<>(2);
	private List<Consumer<Double>> progressListeners = new ArrayList<>(2);
	private List<TaskType> blockingTaskTypes = new ArrayList<>();
	private List<Task> childTasks = new ArrayList<>(2);
	private Task parent;
}
