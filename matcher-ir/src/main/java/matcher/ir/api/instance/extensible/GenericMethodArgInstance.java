package matcher.ir.api.instance.extensible;

import matcher.ir.api.instance.MethodArgInstance;

public interface GenericMethodArgInstance<
		C extends GenericClassInstance<C, F, M, MA, MV>,
		F extends GenericFieldInstance<C, F, M, MA, MV>,
		M extends GenericMethodInstance<C, F, M, MA, MV>,
		MA extends GenericMethodArgInstance<C, F, M, MA, MV>,
		MV extends GenericMethodVarInstance<C, F, M, MA, MV>> extends GenericMethodVarInstance<C, F, M, MA, MV>, MethodArgInstance {
}
