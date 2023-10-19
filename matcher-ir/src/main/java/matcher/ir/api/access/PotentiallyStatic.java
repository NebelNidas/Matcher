package matcher.ir.api.access;

import org.objectweb.asm.Opcodes;

public interface PotentiallyStatic extends AccessFlagsOwner {
	default boolean isStatic() {
		return (getAccess() & Opcodes.ACC_STATIC) != 0;
	}
}
