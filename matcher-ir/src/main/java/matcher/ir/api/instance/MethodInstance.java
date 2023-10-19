package matcher.ir.api.instance;

import java.lang.constant.MethodTypeDesc;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;

import matcher.ir.api.access.PotentiallyAbstract;

public non-sealed interface MethodInstance extends MemberInstance, PotentiallyAbstract {
	default boolean isSynchronized() {
		return (getAccess() & Opcodes.ACC_SYNCHRONIZED) != 0;
	}

	default boolean isBridge() {
		return (getAccess() & Opcodes.ACC_BRIDGE) != 0;
	}

	default boolean isVarargs() {
		return (getAccess() & Opcodes.ACC_VARARGS) != 0;
	}

	default boolean isNative() {
		return (getAccess() & Opcodes.ACC_NATIVE) != 0;
	}

	default boolean isStrict() {
		return (getAccess() & Opcodes.ACC_STRICT) != 0;
	}

	@Override
	@NotNull
	default MethodTypeDesc getDescriptor() {
		return MethodTypeDesc.ofDescriptor(getNameInfo().getDescriptor());
	}

	@Override
	@NotNull
	FieldInstance getLinkedRecordComponent();

	@NotNull
	Stream<? extends MethodArgInstance> getArgs();

	@NotNull
	Stream<? extends MethodVarInstance> getVars();

	@NotNull
	ClassInstance getRetType();
}
