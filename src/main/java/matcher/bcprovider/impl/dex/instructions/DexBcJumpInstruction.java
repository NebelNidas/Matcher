package matcher.bcprovider.impl.dex.instructions;

import org.objectweb.asm.tree.JumpInsnNode;

import matcher.bcprovider.instructions.BcJumpInstruction;

public class DexBcJumpInstruction extends DexBcInstruction implements BcJumpInstruction {
	DexBcJumpInstruction(JumpInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public DexBcInstructionLabel getLabel() {
		return new DexBcInstructionLabel(asmNode.label);
	}

	private JumpInsnNode asmNode;
}
