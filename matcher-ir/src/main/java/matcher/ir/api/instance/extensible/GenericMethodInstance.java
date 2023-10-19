package matcher.ir.api.instance.extensible;

import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.instance.MethodInstance;

public interface GenericMethodInstance<
		C extends GenericClassInstance<C, F, M, MA, MV>,
		F extends GenericFieldInstance<C, F, M, MA, MV>,
		M extends GenericMethodInstance<C, F, M, MA, MV>,
		MA extends GenericMethodArgInstance<C, F, M, MA, MV>,
		MV extends GenericMethodVarInstance<C, F, M, MA, MV>> extends MethodInstance {
	@Override
	F getLinkedRecordComponent();

	@NotNull
	Stream<? extends MA> getArgs();

	@NotNull
	Stream<? extends MV> getVars();

	@NotNull
	C getRetType();
}
