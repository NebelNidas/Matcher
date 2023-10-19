package matcher.ir.api.instance;

public non-sealed interface MethodVarInstance extends Instance {
	@Override
	MethodInstance getOwner();

	int getIndex();

	int getLvIndex();

	int getLvtIndex();

	int getAsmIndex();

	ClassInstance getType();

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
