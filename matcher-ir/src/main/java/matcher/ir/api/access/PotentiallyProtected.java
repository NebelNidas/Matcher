package matcher.ir.api.access;

import org.objectweb.asm.Opcodes;

public interface PotentiallyProtected extends AccessFlagsOwner {
	default boolean isProtected() {
		return (getAccess() & Opcodes.ACC_PROTECTED) != 0;
	}
}
