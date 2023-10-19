package matcher.ir.api.access;

import org.objectweb.asm.Opcodes;

public interface PotentiallyMandated extends AccessFlagsOwner {
	default boolean isMandated() {
		return (getAccess() & Opcodes.ACC_MANDATED) != 0;
	}
}
