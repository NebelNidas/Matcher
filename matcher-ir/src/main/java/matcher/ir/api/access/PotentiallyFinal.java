package matcher.ir.api.access;

import org.objectweb.asm.Opcodes;

public interface PotentiallyFinal extends AccessFlagsOwner {
	default boolean isFinal() {
		return (getAccess() & Opcodes.ACC_FINAL) != 0;
	}
}
