package matcher.ir.api.instance.annotable.info;

import org.jetbrains.annotations.Nullable;

import matcher.ir.api.instance.info.ClassNameInfo;
import matcher.ir.api.instance.info.ClassNames;

public interface AnnotableClassNameInfo extends AnnotableNameInfo, ClassNameInfo, AnnotableDescriptorOwningNameInfo {
	/**
	 * @see ClassNameInfo#getFullyQualifiedName()
	 */
	@Nullable
	default String getFullyQualifiedName(int namespace) {
		return ClassNames.toFullyQualifiedName(getInternalName(namespace, false));
	}

	/**
	 * @see ClassNameInfo#getInternalName(boolean)
	 */
	@Nullable
	String getInternalName(int namespace, boolean dotsForInnerClasses);

	/**
	 * @see ClassNameInfo#getContextualName()
	 */
	@Override
	@Nullable
	default String getContextualName(int namespace) {
		return ClassNames.toContextualName(getInternalName(namespace, false));
	}

	/**
	 * @see ClassNameInfo#getName()
	 */
	@Override
	@Nullable
	default String getName(int namespace) {
		return ClassNames.toSimpleName(getInternalName(namespace, true));
	}

	/**
	 * @see matcher.ir.api.instance.info.DescriptorOwningNameInfo#getDescriptor()
	 */
	@Override
	@Nullable
	default String getDescriptor(int namespace) {
		return ClassNames.toDescriptor(getInternalName(namespace, false));
	}

	/**
	 * @see ClassNameInfo#getPackageName(boolean)
	 */
	@Nullable
	default String getPackageName(int namespace, boolean slashesAsSeparator) {
		return ClassNames.getPackageName(getInternalName(namespace, false), slashesAsSeparator);
	}
}
