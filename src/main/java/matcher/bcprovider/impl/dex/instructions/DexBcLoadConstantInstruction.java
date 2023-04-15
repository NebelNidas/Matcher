package matcher.bcprovider.impl.dex.instructions;

import org.objectweb.asm.tree.LdcInsnNode;

import matcher.bcprovider.instructions.BcLoadConstantInstruction;

public class DexBcLoadConstantInstruction extends DexBcInstruction implements BcLoadConstantInstruction {
	DexBcLoadConstantInstruction(LdcInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public Object getConstant() {
		return asmNode.cst;
	}

	private LdcInsnNode asmNode;
}
