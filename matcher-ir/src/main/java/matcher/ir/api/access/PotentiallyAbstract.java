package matcher.ir.api.access;

import org.objectweb.asm.Opcodes;

public interface PotentiallyAbstract extends AccessFlagsOwner {
	default boolean isAbstract() {
		return (getAccess() & Opcodes.ACC_ABSTRACT) != 0;
	}
}
