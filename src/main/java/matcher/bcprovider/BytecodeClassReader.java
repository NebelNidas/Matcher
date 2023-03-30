package matcher.bcprovider;

import org.objectweb.asm.ClassReader;

public class BytecodeClassReader extends ClassReader {
	public BytecodeClassReader(byte[] classFile) {
		super(classFile);
	}
}
