package matcher.bcprovider.impl.dex.instructions;

import org.objectweb.asm.tree.IincInsnNode;

import matcher.bcprovider.instructions.BcIncrementIntegerInstruction;

public class DexBcIncrementIntegerInstruction extends DexBcInstruction implements BcIncrementIntegerInstruction {
	DexBcIncrementIntegerInstruction(IincInsnNode asmNode) {
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
