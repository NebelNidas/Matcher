package matcher.bcprovider.instructions;

import matcher.bcprovider.BcInstruction;

public interface BcInvokeMethodInstruction extends BcInstruction {
	String getName();

	String getDescriptor();

	String getOwner();

	boolean isOwnerInterface();
}
