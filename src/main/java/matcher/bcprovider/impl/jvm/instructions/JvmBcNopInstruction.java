package matcher.bcprovider.impl.jvm.instructions;

import org.objectweb.asm.tree.InsnNode;

public class JvmBcNopInstruction extends JvmBcInstruction {
	JvmBcNopInstruction(InsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	private InsnNode asmNode;
}
