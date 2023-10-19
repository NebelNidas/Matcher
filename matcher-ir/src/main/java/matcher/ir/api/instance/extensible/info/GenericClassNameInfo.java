package matcher.ir.api.instance.extensible.info;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.instance.extensible.GenericClassInstance;
import matcher.ir.api.instance.extensible.GenericFieldInstance;
import matcher.ir.api.instance.extensible.GenericMethodArgInstance;
import matcher.ir.api.instance.extensible.GenericMethodInstance;
import matcher.ir.api.instance.extensible.GenericMethodVarInstance;
import matcher.ir.api.instance.info.ClassNameInfo;

public interface GenericClassNameInfo<
		C extends GenericClassInstance<C, F, M, MA, MV>,
		F extends GenericFieldInstance<C, F, M, MA, MV>,
		M extends GenericMethodInstance<C, F, M, MA, MV>,
		MA extends GenericMethodArgInstance<C, F, M, MA, MV>,
		MV extends GenericMethodVarInstance<C, F, M, MA, MV>> extends ClassNameInfo {
	@Override
	@NotNull
	C getOwner();
}
