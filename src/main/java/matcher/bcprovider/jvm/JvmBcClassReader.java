package matcher.bcprovider.jvm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public class JvmBcClassReader {
	public JvmBcClassReader(byte[] classFile) {
		classReader = new ClassReader(classFile);
	}

	public JvmBcClass read(int parsingOptions) {
		ClassNode cn = new ClassNode();

		classReader.accept(cn, parsingOptions);

		return new JvmBcClass(cn);
	}

	public class ParsingOption {
		public static int SKIP_CODE = 1;
		public static int SKIP_DEBUG = 2;
		public static int SKIP_FRAMES = 4;
		public static int EXPAND_FRAMES = 8;
	}

	ClassReader classReader;
}
