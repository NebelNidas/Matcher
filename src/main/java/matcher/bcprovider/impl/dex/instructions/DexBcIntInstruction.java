package matcher.bcprovider.impl.dex.instructions;

import org.objectweb.asm.tree.IntInsnNode;

import matcher.bcprovider.instructions.BcIntInstruction;

public class DexBcIntInstruction extends DexBcInstruction implements BcIntInstruction {
	DexBcIntInstruction(IntInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public int getOperand() {
		return asmNode.operand;
	}

	private IntInsnNode asmNode;
}
