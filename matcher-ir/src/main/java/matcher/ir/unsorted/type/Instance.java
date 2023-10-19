package matcher.ir.type;

import matcher.ir.api.NameType;
import matcher.ir.api.env.ClassEnv;

public interface Instance<T extends Instance<T>> {
	InstanceKind getKind();

	String getId();
	String getName();
	String getName(NameType type);

	default String getDisplayName(NameType type, boolean full) {
		return toSimpleName(type);
	}

	boolean hasMappedName();
	boolean hasLocalTmpName();
	boolean hasAuxName(int index);

	String getMappedComment();
	void setMappedComment(String comment);

	int getUid();
	boolean isNameObfuscated();

	Instance<?> getOwner();
	ClassEnv getEnv();
}
