package matcher.bcprovider.impl.dex.instructions;

import org.objectweb.asm.tree.InsnNode;

public class DexBcNopInstruction extends DexBcInstruction {
	DexBcNopInstruction(InsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	private InsnNode asmNode;
}
