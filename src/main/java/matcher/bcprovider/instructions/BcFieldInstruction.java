package matcher.bcprovider.instructions;

import matcher.bcprovider.BcInstruction;

public interface BcFieldInstruction extends BcInstruction {
	String getName();

	String getDescriptor();

	String getOwner();
}
