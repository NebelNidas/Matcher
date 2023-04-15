package matcher.bcprovider.instructions;

import matcher.bcprovider.BcInstruction;

public interface BcLoadConstantInstruction extends BcInstruction {
	Object getConstant();
}
