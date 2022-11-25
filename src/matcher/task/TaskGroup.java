package matcher.task;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TaskGroup<T> extends Task<T> {
	public TaskGroup(String id, Supplier<T> resultSupplier) {
		super(id, null);

		setReturnValueSupplier(resultSupplier);
	}

	protected void setReturnValueSupplier(Supplier<T> supplier) {
		setAction((progress) -> supplier.get());
	}

	public void addSubTask(Task<?> subTask, boolean cancelsParentWhenCanceled) {
		subTask.setParent(this);
		subTask.addProgressListener((childProgress) -> onChildProgressChange(subTask, childProgress));
		subTask.addOnFinish(() -> onChildFinish());
		subTasks.add(subTask);

		if (cancelsParentWhenCanceled) {
			subTask.addOnCancel(() -> this.cancel());
		}

		progress *= Math.max(0.5, (subTasks.size() - 1)) / subTasks.size();
		onProgressChange(progress);

		// If we are running or were running previously, start the subtask directly
		if (state.ordinal() >= TaskState.RUNNING.ordinal()) {
			subTask.run();
		}
	}

	protected void onChildProgressChange(Task<?> childTask, double childProgress) {
		progress += childProgress / subTasks.size();

		onProgressChange(progress);
	}

	protected void onChildFinish() {
		boolean finished = subTasks.stream()
				.filter(subTask -> subTask.getState() == TaskState.CANCELED
						|| subTask.getState() == TaskState.ERRORED
						|| subTask.getState() == TaskState.SUCCEEDED)
				.count() == subTasks.size();

		if (finished) {
			state = TaskState.SUCCEEDED;
			onFinish();
		}
	}

	@Override
	public void run() {
		TaskManager.queue(this);
	}

	@Override
	public void cancel() {
		subTasks.forEach(task -> task.cancel());
	}

	public List<Task<?>> getSubTasks() {
		return subTasks;
	}

	@Override
	public boolean isBlockedBy(String taskId) {
		boolean blocked = super.isBlockedBy(taskId);

		if (blocked) return true;

		return subTasks.stream()
				.filter(task -> task.isBlockedBy(taskId))
				.findAny()
				.isPresent();
	}

	protected boolean orderIndependent;
	protected List<Task<?>> subTasks = new ArrayList<>(2);
}
