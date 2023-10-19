package matcher.ir.api.instance;

import java.lang.constant.ClassDesc;
import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

import matcher.ir.api.instance.info.FieldNameInfo;
import matcher.ir.api.signature.FieldSignature;

public non-sealed interface FieldInstance extends MemberInstance {
	default boolean isVolatile() {
		return (getAccess() & Opcodes.ACC_VOLATILE) != 0;
	}

	default boolean isTransient() {
		return (getAccess() & Opcodes.ACC_TRANSIENT) != 0;
	}

	default boolean isEnumElement() {
		return (getAccess() & Opcodes.ACC_ENUM) != 0;
	}

	@Override
	@NotNull
	default String getId() {
		return getOwner().getId() + getNameInfo().getName();
	}

	@Override
	@NotNull
	FieldNameInfo getNameInfo();

	@Override
	@NotNull
	default ClassDesc getDescriptor() {
		return ClassDesc.ofDescriptor(getNameInfo().getDescriptor());
	}

	@NotNull
	ClassInstance getType();

	@Override
	@NotNull
	MethodInstance getLinkedRecordComponent();

	@NotNull
	FieldSignature getSignature();

	@NotNull
	List<? extends AbstractInsnNode> getInitializer();

	@NotNull
	Stream<? extends MethodInstance> getReadRefs();

	@NotNull
	Stream<? extends MethodInstance> getWriteRefs();
}
