package matcher.ir.api.env;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import matcher.ir.api.identity.Identity;
import matcher.ir.api.instance.extensible.ClassInstance;
import matcher.ir.api.instance.extensible.ClassInstanceBuilder;
import matcher.ir.unsorted.Util;

/**
 * Loads classes from a JAR.
 */
public class JarClassEnv implements ClassEnv {
	public JarClassEnv(Path jarPath) {
		this.jarPath = jarPath;
	}

	public JarClassEnv(Path jarPath, ClassInstanceBuilder clsBuilder) {

	}

	private void loadNamesIfNecessary() {
		if (classNames != null) return;

		synchronized (classNamesLock) {
			if (classNames != null) return;

			Set<String> ret = new HashSet<>();

			Util.iterateJar(jarPath, true, path -> {
				String pathName = path.toString();
				String clsName = pathName.substring(1, pathName.length() - classSuffixLength);
				ret.add(clsName);
			});

			classNames = ret;
			spliterator = new ClassInstanceGeneratingSpliterator(classNames, builder);
		}
	}

	@Override
	@NotNull
	public Set<String> getClassNames() {
		loadNamesIfNecessary();
		return classNames;
	}

	@Override
	@NotNull
	public Stream<C> streamClasses() {
		loadNamesIfNecessary();
		return StreamSupport.stream(spliterator, false);
	}

	@Override
	@NotNull
	public Set<C> getClasses() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getClasses'");
	}

	@Override
	@Nullable
	public ClassInstance getClassByName(String internalName) {
		if (cla)
	}

	@Override
	@Nullable
	public ClassInstance getClassByIdentity(Identity<C> clsIdentity) {
		String name = clsIdentity.getInstanceIds().get(this);

		if (name == null) return null;

		return getClassByName(name);
	}

	@Override
	@Nullable
	public ClassNode getBackingAsmNode(ClassInstance cls) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBackingAsmNode'");
	}

	private static final int classSuffixLength = ".class".length();
	private final Path jarPath;
	private final Object classNamesLock = new Object();
	private final Object spliteratorLock = new Object();
	private Set<String> classNames;
	private ClassInstanceGeneratingSpliterator spliterator;
	private HashMap<String, ClassInstance> classesByName;
}
