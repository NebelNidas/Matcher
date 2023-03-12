package matcher.jobs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class JobManager {
	public static void registerEventListener(BiConsumer<Job<?>, JobManagerEvent> listener) {
		eventListeners.add(listener);
	}

	public static void queue(Job<?> task) {
		task.addOnFinish(() -> onTaskEnded(task));
		queuedTasks.add(task);
		onTaskQueued(task);
	}

	private static void onTaskQueued(Job<?> task) {
		eventListeners.forEach(listener -> listener.accept(task, JobManagerEvent.TASK_QUEUED));
		tryLaunchNext();
	}

	private static void onTaskStarted(Job<?> task) {
		eventListeners.forEach(listener -> listener.accept(task, JobManagerEvent.JOB_STARTED));
	}

	private static void onTaskEnded(Job<?> task) {
		new Thread(() -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignored
			}

			// Ensure thread hasn't been restarted
			if (task.getState() != JobState.QUEUED
					&& task.getState() != JobState.RUNNING) {
				eventListeners.forEach(listener -> listener.accept(task, JobManagerEvent.JOB_ENDED));
				runningTasks.remove(task);
				tryLaunchNext();
			}
		}).run();
	}

	private static void tryLaunchNext() {
		for (Job<?> queuedTask : queuedTasks) {
			boolean blocked = false;

			for (Job<?> runningTask : runningTasks) {
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

	public static List<Job<?>> getRunningTasks() {
		return runningTasks;
	}

	public enum JobManagerEvent {
		TASK_QUEUED,
		JOB_STARTED,
		JOB_ENDED
	}

	private static final ExecutorService threadPool = Executors.newCachedThreadPool();
	private static List<BiConsumer<Job<?>, JobManagerEvent>> eventListeners = new ArrayList<>(2);
	private static List<Job<?>> queuedTasks = new LinkedList<>();
	private static List<Job<?>> runningTasks = new LinkedList<>();
}
