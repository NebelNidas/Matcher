package matcher.ir.api.reader;

import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import matcher.ir.api.env.ClassEnv;
import matcher.ir.api.instance.ClassInstance;

public class JarReader<T extends ClassInstance> implements ClassEnv<T> {
	@Override
	public @NotNull Stream<T> streamClasses() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'streamClasses'");
	}

	@Override
	public @NotNull Set<T> getClasses() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getClasses'");
	}

	@Override
	public @Nullable T getClassByName(String fullyQualifiedName) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getClass'");
	}

	@Override
	public @Nullable T getClass(int uid) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getClass'");
	}

	@Override
	public @Nullable ClassNode getBackingAsmNode(T cls) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBackingAsmNode'");
	}
}
