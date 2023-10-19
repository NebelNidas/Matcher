package matcher.ir.api.access;

import org.objectweb.asm.Opcodes;

public interface PotentiallyPublic extends AccessFlagsOwner {
	default boolean isPublic() {
		return (getAccess() & Opcodes.ACC_PUBLIC) != 0;
	}
}
