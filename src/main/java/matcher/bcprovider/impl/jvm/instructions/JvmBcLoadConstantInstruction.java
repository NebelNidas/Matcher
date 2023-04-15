package matcher.bcprovider.impl.jvm.instructions;

import org.objectweb.asm.tree.LdcInsnNode;

import matcher.bcprovider.instructions.BcLoadConstantInstruction;

public class JvmBcLoadConstantInstruction extends JvmBcInstruction implements BcLoadConstantInstruction {
	JvmBcLoadConstantInstruction(LdcInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public Object getConstant() {
		return asmNode.cst;
	}

	private LdcInsnNode asmNode;
}
