package matcher.ir.api.access;

import org.objectweb.asm.Opcodes;

public interface PotentiallyPackagePrivate extends AccessFlagsOwner {
	default boolean isPackagePrivate() {
		return (getAccess() & Opcodes.ACC_PRIVATE) == 0
				&& (getAccess() & Opcodes.ACC_PROTECTED) == 0
				&& (getAccess() & Opcodes.ACC_PUBLIC) == 0;
	}
}
