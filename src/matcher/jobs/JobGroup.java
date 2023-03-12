package matcher.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Group of jobs that are executed synchronously.
 */
public class JobGroup<T> extends Job<T> {
	public JobGroup(String id, Supplier<T> resultSupplier) {
		super(id, null);

		setReturnValueSupplier(resultSupplier);
	}

	/**
	 * Sets a return value supplier, if not already done so via the constructor.
	 */
	protected void setReturnValueSupplier(Supplier<T> supplier) {
		setAction((progress) -> supplier.get());
	}

	/**
	 * Adds a job to this job group. Subjobs are executed in the order they've been added!
	 */
	public void addSubJob(Job<?> subJob, boolean cancelsParentWhenCanceled) {
		subJob.setParent(this);
		subJob.addProgressListener((childProgress) -> onSubJobProgressChange(subJob, childProgress));
		subJob.addOnFinish(() -> onChildFinish());
		subJobs.add(subJob);

		if (cancelsParentWhenCanceled) {
			subJob.addOnCancel(() -> this.cancel());
		}

		// progress *= Math.max(0.5, (subJobs.size() - 1)) / subJobs.size();
		for (Job<?> job : subJobs) {
			if (job.getProgress() < 0) {
				progress = -1;
				break;
			} else {
				progress += job.getProgress() / subJobs.size();
			}
		}

		onProgressChange(progress);
	}

	protected void onSubJobProgressChange(Job<?> subJob, double childProgress) {
		System.out.println(childProgress);
		progress += childProgress / subJobs.size();

		onProgressChange(progress);
	}

	protected void onChildFinish() {
		boolean finished = subJobs.stream()
				.filter(subJob -> subJob.getState() == JobState.CANCELED
						|| subJob.getState() == JobState.ERRORED
						|| subJob.getState() == JobState.SUCCEEDED)
				.count() == subJobs.size();

		if (finished) {
			state = JobState.SUCCEEDED;
			onFinish();
		}
	}

	@Override
	public void run() {
		JobManager.queue(this);
	}

	@Override
	public void cancel() {
		subJobs.forEach(job -> job.cancel());
	}

	public List<Job<?>> getSubJobs() {
		return subJobs;
	}

	@Override
	public boolean isBlockedBy(String jobId) {
		boolean blocked = super.isBlockedBy(jobId);

		if (blocked) return true;

		return subJobs.stream()
				.filter(job -> job.isBlockedBy(jobId))
				.findAny()
				.isPresent();
	}

	protected boolean orderIndependent;
	protected List<Job<?>> subJobs = new ArrayList<>(2);
}
