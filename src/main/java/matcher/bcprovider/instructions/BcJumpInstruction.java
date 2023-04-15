package matcher.bcprovider.instructions;

import matcher.bcprovider.BcInstruction;
import matcher.bcprovider.impl.jvm.instructions.JvmBcInstructionLabel;

public interface BcJumpInstruction extends BcInstruction {
	JvmBcInstructionLabel getLabel();
}
