package matcher.ir.api.instance;

import matcher.ir.api.NameType;

public interface RenamableInstance extends Instance {
	String getName(NameType type);

	boolean hasMappedName();
	boolean hasLocalTmpName();
	boolean hasAuxName(int index);

	String getMappedComment();
	void setMappedComment(String comment);

	int getUid();
	boolean isNameObfuscated();
}
