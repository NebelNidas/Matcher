package matcher.bcprovider.instructions;

import matcher.bcprovider.BcInstruction;

public interface BcLocalVariableInstruction extends BcInstruction {
	int getVarIndex();
}
