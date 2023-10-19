package matcher.ir.api.instance.info;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.instance.MemberInstance;

public interface MemberNameInfo extends NameInfo, DescriptorOwner {
	@Override
	@NotNull
	MemberInstance getOwner();

	@Override
	@NotNull
	String getName();

	@Override
	@NotNull
	String getContextualName();
}
