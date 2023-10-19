package matcher.ir.api.instance.info;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.instance.MethodInstance;

public interface MethodNameInfo extends MemberNameInfo {
	@Override
	@NotNull
	MethodInstance getOwner();

	@Override
	@NotNull
	default String getContextualName() {
		return getOwner().getOwner().getNameInfo().getContextualName() + getName();
	}
}
