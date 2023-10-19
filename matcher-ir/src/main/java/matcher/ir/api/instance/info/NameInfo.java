package matcher.ir.api.instance.info;

import org.jetbrains.annotations.Nullable;

import matcher.ir.api.instance.Instance;

public interface NameInfo {
	@Nullable
	Instance getOwner();

	/**
	 * @return Just the name of this instance, without any enclosing names.
	 * Nullable for method args and vars.
	 */
	@Nullable
	String getName();

	/**
	 * Returns the contextual name of this instance.
	 * For fields, methods and inner classes, it's the name ({@link #getName()})
	 * prefixed with the contextual name of their parent instance.
	 * For top level classes, it's only their name. See {@link ClassNameInfo#getSimpleName()}
	 *
	 * <p>Examples:
	 * <ul>
	 *   <li>Outer class: "ClassA"</li>
	 *   <li>Inner class: "ClassA$ClassB"</li>
	 *   <li>Method: "ClassA.methodC"</li>
	 * </ul>
	 */
	@Nullable
	String getContextualName();
}
