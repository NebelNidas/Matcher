package matcher.ir.api.instance.info;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.instance.FieldInstance;

public interface FieldNameInfo extends MemberNameInfo {
	@Override
	@NotNull
	FieldInstance getOwner();

	@Override
	@NotNull
	default String getContextualName() {
		return getOwner().getOwner().getNameInfo().getContextualName() + getName();
	}
}
