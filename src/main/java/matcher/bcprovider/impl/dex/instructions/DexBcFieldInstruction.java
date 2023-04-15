package matcher.bcprovider.impl.dex.instructions;

import org.objectweb.asm.tree.FieldInsnNode;

import matcher.bcprovider.instructions.BcFieldInstruction;

public class DexBcFieldInstruction extends DexBcInstruction implements BcFieldInstruction {
	DexBcFieldInstruction(FieldInsnNode asmNode) {
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

	private FieldInsnNode asmNode;
}
