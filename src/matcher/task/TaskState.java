package matcher.task;

public enum TaskState {
	CREATED,
	QUEUED,
	RUNNING,
	CANCELING,
	CANCELED,
	ERRORED,
	SUCCEEDED
}
