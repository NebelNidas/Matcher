package matcher.bcprovider.impl.jvm.instructions;

import org.objectweb.asm.tree.MethodInsnNode;

import matcher.bcprovider.instructions.BcInvokeMethodInstruction;

public class JvmBcInvokeMethodInstruction extends JvmBcInstruction implements BcInvokeMethodInstruction {
	JvmBcInvokeMethodInstruction(MethodInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public String getName() {
		return asmNode.name;
	}

	@Override
	public String getDescriptor() {
		return asmNode.desc;
	}

	@Override
	public String getOwner() {
		return asmNode.owner;
	}

	@Override
	public boolean isOwnerInterface() {
		return asmNode.itf;
	}

	private MethodInsnNode asmNode;
}
