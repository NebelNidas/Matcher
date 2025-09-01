package job4j;

public class BuiltinJobCancellationReasons {
	public static final String NEWER_JOB_WITH_SAME_ID = "Cancelled by newer job with same ID";
	public static final String PARENT_CANCELLATION = "Cancelled due to parent cancellation";
	public static final String PARENT_ERROR = "Cancelled due to error in parent job";
	public static final String INTERRUPTED = "Cancelled due to interruption";
	public static final String SHUTDOWN = "Cancelled due to shutdown";
	public static final String TIMEOUT = "Cancelled due to timeout";
	public static final String USER_REQUEST = "Cancelled by user request";
	public static final String UNSPECIFIED = "Cancelled for unspecified reason";
}
