package matcher.ir.api.instance.info;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import matcher.ir.api.instance.ClassInstance;

public interface ClassNameInfo extends NameInfo, DescriptorOwner {
	@Override
	@NotNull
	ClassInstance getOwner();
	/**
	 * The fully qualified class name contains the package the class originated from,
	 * and all enclosing classes if the target is an inner class.
	 * Example: {@code java.lang.Character$Subset}
	 */
	@NotNull
	default String getFullyQualifiedName() {
		return ClassNames.toFullyQualifiedName(getInternalName(false));
	}

	/**
	 * The internal name is the fully qualified name, but with dots replaced by slashes.
	 * Example: {@code java/lang/Character$Subset}.
	 * @param dotsForInnerClasses
	 * 		The dollar sign isn't a reserved character according to the class file specification,
	 * 		hence why it's unwise to rely on it denoting inner classes (obfuscators might mess with it).
	 * 		With this parameter, the returned string will instead use dots to separate inner classes,
	 * 		which is a reserved character. Note that in order to determine which parts actually point
	 *      to subclasses, all potential outer classes need to be loaded, so this is a rather expensive operation.
	 */
	@NotNull
	String getInternalName(boolean dotsForInnerClasses);

	/**
	 * The canonical name is the fully qualified name, but with dots as the delimiting characters for inner classes.
	 * Example: {@code java.lang.Character.Subset}
	 *
	 * <p>Note that in order to determine which parts actually point to subclasses,
	 * all potential outer classes need to be loaded, so this is a rather expensive operation.
	 */
	@NotNull
	default String getCanonicalName() {
		return ClassNames.toFullyQualifiedName(getInternalName(true));
	}

	/**
	 * Same as the fully qualified name, but with packages stripped out.
	 * Example: {@code Character$Subset}
	 */
	@Override
	@NotNull
	default String getContextualName() {
		return ClassNames.toContextualName(getInternalName(false));
	}

	/**
	 * Just the class name, without enclosing class names or packages.
	 * Note that in order to determine which parts actually point to subclasses,
	 * all potential outer classes need to be loaded, so this is a rather expensive operation.
	 */
	@Override
	@NotNull
	default String getName() {
		return ClassNames.toSimpleName(getInternalName(true));
	}

	@Override
	@Nullable
	default String getDescriptor() {
		return ClassNames.toDescriptor(getInternalName(false));
	}

	@Nullable
	default String getPackageName(boolean slashesAsSeparator) {
		return ClassNames.getPackageName(getInternalName(false), slashesAsSeparator);
	}
}
