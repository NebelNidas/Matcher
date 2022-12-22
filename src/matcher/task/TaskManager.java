package matcher.task;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class TaskManager {
	public static void registerEventListener(BiConsumer<Task<?>, TaskManagerEvent> listener) {
		eventListeners.add(listener);
	}

	public static void queue(Task<?> task) {
		task.addOnFinish(() -> onTaskEnded(task));
		queuedTasks.add(task);
		onTaskQueued(task);
	}

	private static void onTaskQueued(Task<?> task) {
		eventListeners.forEach(listener -> listener.accept(task, TaskManagerEvent.TASK_QUEUED));
		tryLaunchNext();
	}

	private static void onTaskStarted(Task<?> task) {
		eventListeners.forEach(listener -> listener.accept(task, TaskManagerEvent.TASK_STARTED));
	}

	private static void onTaskEnded(Task<?> task) {
		new Thread(() -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignored
			}

			// Ensure thread hasn't been restarted
			if (task.getState() != TaskState.QUEUED
					&& task.getState() != TaskState.RUNNING) {
				eventListeners.forEach(listener -> listener.accept(task, TaskManagerEvent.TASK_ENDED));
				runningTasks.remove(task);
				tryLaunchNext();
			}
		}).run();
	}

	private static void tryLaunchNext() {
		for (Task<?> queuedTask : queuedTasks) {
			boolean blocked = false;

			for (Task<?> runningTask : runningTasks) {
				if (queuedTask.isBlockedBy(runningTask.getId())) {
					blocked = true;
					break;
				}
			}

			if (!blocked) {
				queuedTasks.remove(queuedTask);
				runningTasks.add(queuedTask);
				onTaskStarted(queuedTask);
				threadPool.submit(() -> queuedTask.runNow());
			}
		}
	}

	public static void shutdown() {
		threadPool.shutdown();
	}

	public static List<Task<?>> getRunningTasks() {
		return runningTasks;
	}

	public enum TaskManagerEvent {
		TASK_QUEUED,
		TASK_STARTED,
		TASK_ENDED
	}

	private static final ExecutorService threadPool = Executors.newCachedThreadPool();
	private static List<BiConsumer<Task<?>, TaskManagerEvent>> eventListeners = new ArrayList<>(2);
	private static List<Task<?>> queuedTasks = new LinkedList<>();
	private static List<Task<?>> runningTasks = new LinkedList<>();
}
