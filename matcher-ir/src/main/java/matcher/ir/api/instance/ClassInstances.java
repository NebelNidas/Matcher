package matcher.ir.api.instance;

import matcher.ir.impl.ClassInstanceImpl;

public class ClassInstances {
	public static ClassInstance newObject() {
		return new ClassInstanceImpl();
	}

	public static ClassInstance newArray() {
		return null;
	}

	public static ClassInstance newRenamableObject() {
		return null;
	}

	public static ClassInstance newRenamableArray() {
		return null;
	}
}
