package job4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import matcher.Util;

public class JobManager {
	private static final JobManager INSTANCE = new JobManager();
	public static final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

	static {
		threadPool.setKeepAliveTime(60L, TimeUnit.SECONDS);
		threadPool.allowCoreThreadTimeOut(true);
	}

	public static synchronized JobManager get() {
		return INSTANCE;
	}

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
	synchronized void queue(Job<?> job) {
		for (Job<?> runningJob : this.runningJobs) {
			if (runningJob.getThread() == Thread.currentThread()) {
				// An already running job indirectly started another job.
				// Neither one declared the correct hierarchy (they don't know each other),
				// nevertheless one job indirectly parents the other one.
				// Now we're declaring the correct hierarchy ourselves.
				runningJob.addSubJob(job, false);
				job.run();
				return;
			}
		}

		if (job.getSettings().isCancelPreviousJobsWithSameId()) {
			for (Job<?> queuedJob : this.queuedJobs) {
				if (queuedJob.getCategory() == job.getCategory()
						&& queuedJob.getId().equals(job.getId())) {
					queuedJob.cancel();
				}
			}

			for (Job<?> runningJob : this.runningJobs) {
				if (runningJob.getCategory() == job.getCategory()
						&& runningJob.getId().equals(job.getId())) {
					runningJob.cancel();
				}
			}
		}

		this.queuedJobs.add(job);

		job.addCompletionListener((result, error) -> onJobFinished(job));
		notifyEventListeners(job, JobManagerEvent.JOB_QUEUED);
		tryLaunchNext();
	}

	private void onJobFinished(Job<?> job) {
		notifyEventListeners(job, JobManagerEvent.JOB_FINISHED);
		this.runningJobs.remove(job);
		tryLaunchNext();
	}

	private synchronized void tryLaunchNext() {
		for (Job<?> queuedJob : this.queuedJobs) {
			boolean blocked = false;

			for (Job<?> runningJob : this.runningJobs) {
				if (queuedJob.isBlockedBy(runningJob.getCategory())) {
					blocked = true;
					break;
				}
			}

			if (!blocked) {
				this.queuedJobs.remove(queuedJob);
				this.runningJobs.add(queuedJob);
				notifyEventListeners(queuedJob, JobManagerEvent.JOB_STARTED);

				Thread wrapper = new Thread(() -> {
					try {
						threadPool.submit(() -> queuedJob.runNow()).get(queuedJob.getSettings().getTimeout(), TimeUnit.SECONDS);
					} catch (Exception e) {
						if (e instanceof TimeoutException) {
							queuedJob.cancel();
						} else {
							throw new RuntimeException(String.format("Exception encountered in job %s:\n%s",
									queuedJob.getId(), Util.getStacktrace(e)));
						}
					}
				});
				wrapper.setName(queuedJob.getId() + " wrapper thread");
				wrapper.start();
			}
		}
	}

	/**
	 * {@return an unmodifiable view of the queued jobs list}.
	 */
	public List<Job<?>> getQueuedJobs() {
		return Collections.unmodifiableList(this.queuedJobs);
	}

	/**
	 * {@return an unmodifiable view of the running jobs list}.
	 */
	public List<Job<?>> getRunningJobs() {
		return Collections.unmodifiableList(this.runningJobs);
	}

	public void shutdown() {
		this.queuedJobs.clear();

		synchronized (this.runningJobs) {
			this.runningJobs.forEach(job -> job.cancel());
		}

		threadPool.shutdownNow();
	}

	public enum JobManagerEvent {
		JOB_QUEUED,
		JOB_STARTED,
		JOB_FINISHED
	}
}
