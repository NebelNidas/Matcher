package matcher.jobs;

public enum JobState {
	CREATED,
	QUEUED,
	RUNNING,
	CANCELING,
	CANCELED,
	ERRORED,
	SUCCEEDED
}
