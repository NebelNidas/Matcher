package matcher.bcprovider;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class BytecodeClass extends ClassNode {
	public BytecodeClass() {
		super(Opcodes.ASM9);
	}
}
