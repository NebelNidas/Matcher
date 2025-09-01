package job4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobManager {
	private static final JobManager INSTANCE = new JobManager();
	private static final Logger LOGGER = LoggerFactory.getLogger(JobManager.class);
	private static final ThreadPoolExecutor JOB_EXECUTING_THREAD_POOL;
	private static final ThreadPoolExecutor JOB_AWAIT_THREAD_POOL;

	static {
		int nThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
		JOB_EXECUTING_THREAD_POOL = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1));
		JOB_EXECUTING_THREAD_POOL.setKeepAliveTime(60L, TimeUnit.SECONDS);
		JOB_EXECUTING_THREAD_POOL.allowCoreThreadTimeOut(true);
	}

	public static JobManager get() {
		return INSTANCE;
	}

	private final List<BiConsumer<Job<?>, JobManagerEvent>> eventListeners = Collections.synchronizedList(new ArrayList<>());
	private final List<Job<?>> queuedJobs = Collections.synchronizedList(new LinkedList<>());
	private final List<Job<?>> runningJobs = Collections.synchronizedList(new LinkedList<>());
	private volatile boolean shuttingDown;

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
	void queue(Job<?> job, boolean awaitTermination) {
		fixHierarchyIfNecessary(job);

		if (job.parent != null) {
			assert job.getParent().getThread() == Thread.currentThread();
			job.state = JobState.QUEUED;
			job.runOnCurrentThread();
			return;
		}

		if (job.getSettings().isCancelPreviousJobsWithSameId()) {
			synchronized (queuedJobs) {
				for (Job<?> queuedJob : queuedJobs) {
					if (queuedJob.getCategory() == job.getCategory() && queuedJob.getId().equals(job.getId())) {
						queuedJob.cancel(BuiltinJobCancellationReasons.NEWER_JOB_WITH_SAME_ID);
						queuedJobs.remove(queuedJob);
					}
				}
			}

			synchronized (runningJobs) {
				for (Job<?> runningJob : runningJobs) {
					if (runningJob.getCategory() == job.getCategory() && runningJob.getId().equals(job.getId())) {
						runningJob.cancel(BuiltinJobCancellationReasons.NEWER_JOB_WITH_SAME_ID);
					}
				}
			}
		}

		synchronized (queuedJobs) {
			queuedJobs.add(job);
		}

		job.addFinishListener((result, error) -> onJobFinished(job));

		if (awaitTermination) {
			Thread thread = Thread.currentThread();
			job.addFinishListener((result, error) -> thread.notifyAll());
		}

		job.state = JobState.QUEUED;
		notifyEventListeners(job, JobManagerEvent.JOB_QUEUED);
		tryLaunchNext();

		if (awaitTermination) {
			while (!job.getState().isFinished()) {
				try {
					wait();
				} catch (InterruptedException e) {
					job.cancel(BuiltinJobCancellationReasons.INTERRUPTED);
				}
			}
		}
	}

	/**
	 * If the job has no parent, tries to find a running job in the current thread
	 * and adds the new job as a subjob to it.
	 */
	private void fixHierarchyIfNecessary(Job<?> job) {
		if (job.getParent() != null) {
			return;
		}

		synchronized (runningJobs) {
			for (Job<?> runningJob : runningJobs) {
				if (runningJob.getThread() == Thread.currentThread()) {
					// An already running job indirectly started another job.
					// Neither one declared the correct hierarchy (they don't know each other),
					// nevertheless one job indirectly parents the other one.
					// Now we're declaring the correct hierarchy ourselves.
					runningJob.addSubJob(job, false);
					return;
				}
			}
		}
	}

	private void onJobFinished(Job<?> job) {
		notifyEventListeners(job, JobManagerEvent.JOB_FINISHED);

		synchronized (runningJobs) {
			runningJobs.remove(job);
		}

		tryLaunchNext();
	}

	private void tryLaunchNext() {
		if (queuedJobs.isEmpty() || !JOB_EXECUTING_THREAD_POOL.getQueue().isEmpty()) {
			return;
		}

		synchronized (queuedJobs) {
			Iterator<Job<?>> queuedJobsIterator = queuedJobs.iterator();

			queuedJobsLoop:
			while (queuedJobsIterator.hasNext()) {
				Job<?> queuedJob = queuedJobsIterator.next();

				synchronized (runningJobs) {
					List<Job<?>> jobsToCheckAgainst = new ArrayList<>();

					for (Job<?> runningJob : runningJobs) {
						jobsToCheckAgainst.add(runningJob);
						jobsToCheckAgainst.addAll(runningJob.getSubJobs(true));
					}

					for (Job<?> jobToCheckAgainst : jobsToCheckAgainst) {
						if (queuedJob.isBlockedBy(jobToCheckAgainst.getCategory())) {
							continue queuedJobsLoop;
						}
					}
				}

				queuedJobsIterator.remove();
				runningJobs.add(queuedJob);
				notifyEventListeners(queuedJob, JobManagerEvent.JOB_STARTED);
				Future<?> future = JOB_EXECUTING_THREAD_POOL.submit(queuedJob::runOnCurrentThread);

				Thread wrapper = new Thread(() -> {
					try {
						future.get(queuedJob.getSettings().getTimeout(), TimeUnit.SECONDS);
					} catch (Exception e) {
						if (e instanceof TimeoutException) {
							queuedJob.cancel(BuiltinJobCancellationReasons.TIMEOUT);
						} else if (!shuttingDown) {
							throw new RuntimeException(String.format("An exception has been encountered in wrapper thread for job '%s'", queuedJob.getId()), e);
						}
					}
				});
				wrapper.setName(queuedJob.getId() + " wrapper thread");
				wrapper.start();

				if (!JOB_EXECUTING_THREAD_POOL.getQueue().isEmpty()) {
					return;
				}
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

	/**
	 * @param id the job in question's ID
	 * @param recursive whether all running jobs' subjobs should be checked too
	 */
	public boolean isJobRunning(String id, boolean recursive) {
		List<Job<?>> jobs = List.copyOf(this.runningJobs);

		return recursive
				? jobs.stream().anyMatch((job -> job.getId().equals(id) || job.hasSubJob(id, true)))
				: jobs.stream().anyMatch((job -> job.getId().equals(id)));
	}

	public int getMaxJobExecutorThreads() {
		return JOB_EXECUTING_THREAD_POOL.getMaximumPoolSize();
	}

	public void setMaxJobExecutorThreads(int newSize) {
		int oldSize = JOB_EXECUTING_THREAD_POOL.getMaximumPoolSize();

		if (newSize < oldSize) {
			JobManager.JOB_EXECUTING_THREAD_POOL.setCorePoolSize(newSize);
			JobManager.JOB_EXECUTING_THREAD_POOL.setMaximumPoolSize(newSize);
		} else if (newSize > oldSize) {
			JobManager.JOB_EXECUTING_THREAD_POOL.setMaximumPoolSize(newSize);
			JobManager.JOB_EXECUTING_THREAD_POOL.setCorePoolSize(newSize);
		}
	}

	public void shutdown() {
		if (shuttingDown) return;

		shuttingDown = true;
		this.queuedJobs.clear();

		synchronized (this.runningJobs) {
			this.runningJobs.forEach(job -> job.cancel(BuiltinJobCancellationReasons.SHUTDOWN));
		}

		JOB_EXECUTING_THREAD_POOL.shutdownNow();
	}

	public boolean isShuttingDown() {
		return shuttingDown;
	}

	public enum JobManagerEvent {
		JOB_QUEUED,
		JOB_STARTED,
		JOB_FINISHED
	}
}
