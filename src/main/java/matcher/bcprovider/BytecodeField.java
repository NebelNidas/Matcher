package matcher.bcprovider;

public interface BytecodeField {
	String getName();

	String getDesc();

	String getSignature();

	Object getValue();

	int getAccess();
}
