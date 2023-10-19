package matcher.ir.api.instance.info;

import org.jetbrains.annotations.NotNull;

interface DescriptorOwner {
	/**
	 * @return The instance's descriptor.
	 * E.g. {@code int[] foo(int i, String s)} becomes {@code (ILjava/lang/String;)[I},
	 * and {@code String bar} becomes {@code Ljava/lang/String;}.
	 */
	@NotNull
	String getDescriptor();
}
