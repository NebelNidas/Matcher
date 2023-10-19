package matcher.ir.api.instance.extensible;

import matcher.ir.api.instance.MethodVarInstance;

public interface GenericMethodVarInstance<
		C extends GenericClassInstance<C, F, M, MA, MV>,
		F extends GenericFieldInstance<C, F, M, MA, MV>,
		M extends GenericMethodInstance<C, F, M, MA, MV>,
		MA extends GenericMethodArgInstance<C, F, M, MA, MV>,
		MV extends GenericMethodVarInstance<C, F, M, MA, MV>> extends MethodVarInstance {
	@Override
	M getOwner();

	int getIndex();

	int getLvIndex();

	int getLvtIndex();

	int getAsmIndex();

	C getType();

	/**
	 * Inclusive.
	 */
	int getStartInsnIndex();

	/**
	 * Exclusive.
	 */
	int getEndInsnIndex();

	int getStartOpIdx();
}
