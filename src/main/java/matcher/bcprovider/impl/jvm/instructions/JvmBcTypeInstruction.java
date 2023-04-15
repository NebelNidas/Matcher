package matcher.bcprovider.impl.jvm.instructions;

import org.objectweb.asm.tree.TypeInsnNode;

import matcher.bcprovider.instructions.BcTypeInstruction;

public class JvmBcTypeInstruction extends JvmBcInstruction implements BcTypeInstruction {
	JvmBcTypeInstruction(TypeInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	@Override
	public String getType() {
		return asmNode.desc;
	}

	private TypeInsnNode asmNode;
}
