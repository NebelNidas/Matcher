package matcher.ir.api.instance.annotable;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.instance.FieldInstance;
import matcher.ir.api.instance.MethodArgInstance;
import matcher.ir.api.instance.MethodInstance;
import matcher.ir.api.instance.MethodVarInstance;
import matcher.ir.api.instance.annotable.info.AnnotableClassNameInfo;
import matcher.ir.api.instance.extensible.ClassInstance;

public non-sealed interface AnnotableClassInstance<
			C extends AnnotableClassInstance<C, F, M, MA, MV>, // own type
			F extends FieldInstance<C, F, M, MA, MV>,
			M extends MethodInstance<C, F, M, MA, MV>,
			MA extends GenericMethodArgInstance<C, F, M, MA, MV>,
			MV extends GenericMethodVarInstance<C, F, M, MA, MV>>
			extends ClassInstance<C, F, M, MA, MV>,
			AnnotableInstance,
			AnnotableParentInstance {
	@NotNull
	AnnotableClassNameInfo getNameInfo();
}
