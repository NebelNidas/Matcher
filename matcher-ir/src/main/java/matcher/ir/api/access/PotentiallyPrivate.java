package matcher.ir.api.access;

import org.objectweb.asm.Opcodes;

public interface PotentiallyPrivate extends AccessFlagsOwner {
	default boolean isPrivate() {
		return (getAccess() & Opcodes.ACC_PRIVATE) != 0;
	}
}
