package matcher.ir.api.instance.extensible;

import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.instance.FieldInstance;
import matcher.ir.api.instance.extensible.info.GenericFieldNameInfo;

public interface GenericFieldInstance<
		C extends GenericClassInstance<C, F, M, MA, MV>,
		F extends GenericFieldInstance<C, F, M, MA, MV>, // own type
		M extends GenericMethodInstance<C, F, M, MA, MV>,
		MA extends GenericMethodArgInstance<C, F, M, MA, MV>,
		MV extends GenericMethodVarInstance<C, F, M, MA, MV>> extends FieldInstance {
	@Override
	@NotNull
	GenericFieldNameInfo<C, F, M, MA, MV> getNameInfo();

	@Override
	@NotNull
	C getType();

	@Override
	M getLinkedRecordComponent();

	@Override
	@NotNull
	Stream<? extends M> getReadRefs();

	@Override
	@NotNull
	Stream<? extends M> getWriteRefs();
}
