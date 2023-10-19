package matcher.ir.api.access;

import org.objectweb.asm.Opcodes;

public interface PotentiallySynthetic extends AccessFlagsOwner {
	default boolean isSynthetic() {
		return (getAccess() & Opcodes.ACC_SYNTHETIC) != 0;
	}
}
