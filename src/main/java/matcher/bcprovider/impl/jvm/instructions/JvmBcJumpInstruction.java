package matcher.bcprovider.impl.jvm.instructions;

import org.objectweb.asm.tree.JumpInsnNode;

import matcher.bcprovider.instructions.BcJumpInstruction;

public class JvmBcJumpInstruction extends JvmBcInstruction implements BcJumpInstruction {
	JvmBcJumpInstruction(JumpInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public JvmBcInstructionLabel getLabel() {
		return new JvmBcInstructionLabel(asmNode.label);
	}

	private JumpInsnNode asmNode;
}
