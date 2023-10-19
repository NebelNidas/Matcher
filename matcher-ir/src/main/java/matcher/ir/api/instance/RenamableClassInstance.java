package matcher.ir.api.instance;

import org.jetbrains.annotations.NotNull;

public interface RenamableClassInstance extends ClassInstance {
	/**
	 * The fully qualified class name contains the package the class originated from,
	 * and all enclosing classes if the target is an inner class. Example:
	 * {@code java.lang.Character$Subset}.
	 */
	@NotNull
	String getFullyQualifiedName(int namespace);

	/**
	 * The internal name of a class is just the fully qualified name of this class, where dots are replaced with slashes.
	 * For example the internal name of {@link String} is {@code java/lang/Character$Subset}.
	 */
	@NotNull
	String getInternalName(int namespace);

	/**
	 * Same as the fully qualified name, but with packages stripped out.
	 */
	@NotNull
	String getSimpleName(int namespace);

	/**
	 * Just the class name, without enclosing class names or packages.
	 */
	@NotNull
	String getName(int namespace);
}
