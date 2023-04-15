package matcher.bcprovider.impl.dex.instructions;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LabelNode;

import matcher.bcprovider.instructions.BcInstructionLabel;

public class DexBcInstructionLabel extends DexBcInstruction implements BcInstructionLabel {
	DexBcInstructionLabel(LabelNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public Label getLabel() {
		return asmNode.getLabel();
	}

	@Override
	public void resetLabel() {
		asmNode.resetLabel();
	}

	private LabelNode asmNode;
}
