package matcher.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public class JobManager {
	private static final JobManager INSTANCE = new JobManager();

	public static synchronized JobManager get() {
		return INSTANCE;
	}

	private static final ExecutorService threadPool = Executors.newCachedThreadPool();
	private List<BiConsumer<Job<?>, JobManagerEvent>> eventListeners = Collections.synchronizedList(new ArrayList<>());
	private List<Job<?>> queuedJobs = Collections.synchronizedList(new LinkedList<>());
	private List<Job<?>> runningJobs = Collections.synchronizedList(new LinkedList<>());

	public void registerEventListener(BiConsumer<Job<?>, JobManagerEvent> listener) {
		this.eventListeners.add(listener);
	}

	private void notifyEventListeners(Job<?> job, JobManagerEvent event) {
		synchronized (this.eventListeners) {
			this.eventListeners.forEach(listener -> listener.accept(job, event));
		}
	}

	/**
	 * Queues the job for execution.
	 */
	void queue(Job<?> job) {
		job.addCompletionListener((result, error) -> onJobFinished(job));
		this.queuedJobs.add(job);
		notifyEventListeners(job, JobManagerEvent.JOB_QUEUED);
		tryLaunchNext();
	}

	private void onJobFinished(Job<?> job) {
		notifyEventListeners(job, JobManagerEvent.JOB_FINISHED);
		this.runningJobs.remove(job);
		tryLaunchNext();
	}

	private void tryLaunchNext() {
		for (Job<?> queuedJob : this.queuedJobs) {
			boolean blocked = false;

			for (Job<?> runningJob : this.runningJobs) {
				if (queuedJob.isBlockedBy(runningJob.getId())) {
					blocked = true;
					break;
				}
			}

			if (!blocked) {
				this.queuedJobs.remove(queuedJob);
				this.runningJobs.add(queuedJob);
				notifyEventListeners(queuedJob, JobManagerEvent.JOB_STARTED);
				threadPool.submit(() -> queuedJob.runNow());
			}
		}
	}

	public List<Job<?>> getRunningJobs() {
		return this.runningJobs;
	}

	public void shutdown() {
		this.queuedJobs.clear();
		this.runningJobs.forEach(job -> job.cancel());
		threadPool.shutdownNow();
	}

	public enum JobManagerEvent {
		JOB_QUEUED,
		JOB_STARTED,
		JOB_FINISHED
	}
}
