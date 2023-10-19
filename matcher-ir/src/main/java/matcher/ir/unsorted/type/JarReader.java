package matcher.ir.type;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.objectweb.asm.tree.ClassNode;

import matcher.ir.api.env.ClassEnv;
import matcher.ir.unsorted.Util;

public class JarReader {
	public static List<ClassInstance> processInputs(Path input, Predicate<ClassNode> obfuscationChecker, ClassEnv targetEnv) {
		URI origin = input.toUri();

		Util.iterateJar(input, true, file -> {
			ClassInstance cls = readClass(file, origin, obfuscationChecker);
			String id = cls.getId();
			String name = cls.getName();

			if (env.getSharedClsById(id) != null) return;
			if (env.getSharedClassLocation(name) != null) return;
			if (classPathIndex.containsKey(name)) return;

			ClassInstance prev = classes.get(id);

			if (prev == null) {
				classes.put(id, cls);
			} else if (prev.isInput()) {
				mergeClasses(cls, prev);
			}
		});
	}

	private static ClassInstance readClass(Path path, URI origin, Predicate<ClassNode> nameObfuscated) {
		ClassNode cn = Util.readClass(path, false);

		return new ClassInstance(ClassInstance.getId(cn.name), origin, this, cn, nameObfuscated.test(cn));
	}

	private final List<ClassInstance> classes = new ArrayList<>();
}
