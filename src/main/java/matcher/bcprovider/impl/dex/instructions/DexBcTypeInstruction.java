package matcher.bcprovider.impl.dex.instructions;

import org.objectweb.asm.tree.TypeInsnNode;

import matcher.bcprovider.instructions.BcTypeInstruction;

public class DexBcTypeInstruction extends DexBcInstruction implements BcTypeInstruction {
	DexBcTypeInstruction(TypeInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public String getType() {
		return asmNode.desc;
	}

	private TypeInsnNode asmNode;
}
