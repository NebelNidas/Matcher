package matcher.bcprovider;

import org.objectweb.asm.ClassVisitor;

public class BytecodeClassVisitor extends ClassVisitor {
	protected BytecodeClassVisitor(int api) {
		super(api);
	}

	protected BytecodeClassVisitor(int api, ClassVisitor classVisitor) {
		super(api, classVisitor);
	}
}
