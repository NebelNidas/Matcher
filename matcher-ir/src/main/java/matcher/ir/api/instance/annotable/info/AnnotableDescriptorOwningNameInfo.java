package matcher.ir.api.instance.annotable.info;

import org.jetbrains.annotations.NotNull;

interface AnnotableDescriptorOwningNameInfo {
	/**
	 * @see matcher.ir.api.instance.info.DescriptorOwningNameInfo#getDescriptor()
	 */
	@NotNull
	String getDescriptor(int namespace);
}
