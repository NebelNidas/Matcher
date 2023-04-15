package matcher.bcprovider.impl.dex.instructions;

import org.objectweb.asm.tree.VarInsnNode;

import matcher.bcprovider.instructions.BcLocalVariableInstruction;

public class DexBcLocalVariableInstruction extends DexBcInstruction implements BcLocalVariableInstruction {
	DexBcLocalVariableInstruction(VarInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public int getVarIndex() {
		return asmNode.var;
	}

	private VarInsnNode asmNode;
}
