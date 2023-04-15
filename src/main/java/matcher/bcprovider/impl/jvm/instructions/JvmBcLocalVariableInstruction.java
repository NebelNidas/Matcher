package matcher.bcprovider.impl.jvm.instructions;

import org.objectweb.asm.tree.VarInsnNode;

import matcher.bcprovider.instructions.BcLocalVariableInstruction;

public class JvmBcLocalVariableInstruction extends JvmBcInstruction implements BcLocalVariableInstruction {
	JvmBcLocalVariableInstruction(VarInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public int getVarIndex() {
		return asmNode.var;
	}

	private VarInsnNode asmNode;
}
