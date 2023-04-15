package matcher.bcprovider.impl.jvm.instructions;

import org.objectweb.asm.tree.IincInsnNode;

import matcher.bcprovider.instructions.BcIncrementIntegerInstruction;

public class JvmBcIncrementIntegerInstruction extends JvmBcInstruction implements BcIncrementIntegerInstruction {
	JvmBcIncrementIntegerInstruction(IincInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public int getIntegerIndex() {
		return asmNode.var;
	}

	@Override
	public int getIncrement() {
		return asmNode.incr;
	}

	private IincInsnNode asmNode;
}
