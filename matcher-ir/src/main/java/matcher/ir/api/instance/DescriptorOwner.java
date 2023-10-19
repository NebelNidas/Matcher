package matcher.ir.api.instance;

import java.lang.invoke.TypeDescriptor;

import org.jetbrains.annotations.NotNull;

interface DescriptorOwner {
	@NotNull
	TypeDescriptor getDescriptor();
}
