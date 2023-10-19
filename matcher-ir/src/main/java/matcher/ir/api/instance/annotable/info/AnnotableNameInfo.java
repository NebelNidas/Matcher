package matcher.ir.api.instance.annotable.info;

import org.jetbrains.annotations.Nullable;

import matcher.ir.api.instance.info.NameInfo;

/**
 * Index -1 represents the original name, 0 or greater point to renames.
 */
public interface AnnotableNameInfo extends NameInfo {
	/**
	 * @return Whether or not renames are present at index 0 or greater.
	 */
	boolean hasRename();

	/**
	 * @return Whether or not the rename at the specified index has been inferred
	 * from other hierarchy members on the classpath.
	 */
	boolean isInferredRename(int namespace);

	/**
	 * @see #getName()
	 */
	@Nullable
	String getName(int namespace);

	/**
	 * @see #getContextualName()
	 */
	@Nullable
	String getContextualName(int namespace);
}
