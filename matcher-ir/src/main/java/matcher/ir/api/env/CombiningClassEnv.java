package matcher.ir.api.env;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import matcher.ir.api.instance.extensible.ClassInstance;

public interface CombiningClassEnv<T extends ClassInstance> extends ClassEnv<T> {
	void addEnv(ClassEnv<T> env);

	void removeEnv(ClassEnv<T> env);
	void removeEnv(int envId);

	@NotNull
	Set<ClassEnv<T>> getChildEnvs();

	/**
	 * @return See {@link ClassEnv#streamClasses()}.
	 * {@link ClassInstance}s present in multiple child envs will only be returned once.
	 */
	@Override
	Stream<T> streamClasses();

	/**
	 * @return See {@link ClassEnv#getClasses()}.
	 * {@link ClassInstance}s present in multiple child envs will only be returned once.
	 */
	@Override
	Set<T> getClasses();

	/**
	 * Different child envs may have different ClassInstances representing the same class.
	 * Implementors may chose to handle this correctly, by default it simply throws a RuntimeException
	 * when encountering such a situation. Please use {@link #getClasses(String)} instead.
	 */
	@Override
	default T getClassByName(String internalName) {
		Map<ClassEnv<T>, T> map = getClasses(internalName);

		if (map.size() > 1) {
			throw new RuntimeException("Multiple ClassEnvs provide different ClassInstances with the same internal name");
		}

		return map.isEmpty() ? null : map.entrySet().iterator().next().getValue();
	}

	@NotNull
	Map<ClassEnv<T>, T> getClasses(String internalName);

	/**
	 * Different child envs may have different ASM ClassNodes representing the same ClassInstance.
	 * Implementors may chose to handle this correctly, by default it simply throws a RuntimeException
	 * when encountering such a situation. Please use {@link #getBackingAsmNodes(ClassInstance)} instead.
	 */
	@Override
	default ClassNode getBackingAsmNode(T cls) {
		Map<ClassEnv<T>, ClassNode> map = getBackingAsmNodes(cls);

		if (map.size() > 1) {
			throw new RuntimeException("Multiple ClassEnvs provide different ASM nodes backing the same ClassInstance");
		}

		return map.isEmpty() ? null : map.entrySet().iterator().next().getValue();
	}

	@NotNull
	Map<ClassEnv<T>, ClassNode> getBackingAsmNodes(T cls);
}
