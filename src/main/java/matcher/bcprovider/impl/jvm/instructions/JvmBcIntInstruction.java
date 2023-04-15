package matcher.bcprovider.impl.jvm.instructions;

import org.objectweb.asm.tree.IntInsnNode;

import matcher.bcprovider.instructions.BcIntInstruction;

public class JvmBcIntInstruction extends JvmBcInstruction implements BcIntInstruction {
	JvmBcIntInstruction(IntInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public int getOperand() {
		return asmNode.operand;
	}

	private IntInsnNode asmNode;
}
