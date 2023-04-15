package matcher.bcprovider.instructions;

import org.objectweb.asm.Label;

import matcher.bcprovider.BcInstruction;

public interface BcInstructionLabel extends BcInstruction {
	Label getLabel();

	void resetLabel();
}
