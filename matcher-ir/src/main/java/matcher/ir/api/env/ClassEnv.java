package matcher.ir.api.env;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import matcher.ir.api.identity.Identity;
import matcher.ir.api.instance.FieldInstance;
import matcher.ir.api.instance.MethodArgInstance;
import matcher.ir.api.instance.MethodInstance;
import matcher.ir.api.instance.MethodVarInstance;
import matcher.ir.api.instance.extensible.ClassInstance;

/**
 * Supplier of arbitrarily grouped together {@link ClassInstance}s.
 * All returned collections are immutable.
 */
public interface ClassEnv<
		C extends ClassInstance<C, F, M, MA, MV>,
		F extends FieldInstance<C, F, M, MA, MV>,
		M extends MethodInstance<C, F, M, MA, MV>,
		MA extends MethodArgInstance<C, F, M, MA, MV>,
		MV extends MethodVarInstance<C, F, M, MA, MV>> {
	/**
	 * @return Internal names of all contained classes.
	 */
	@NotNull
	Set<String> getClassNames();

	/**
	 * @return Stream over all contained {@link ClassInstance}s.
	 * In contrast to {@link #getClasses}, this method loads lazily.
	 */
	@NotNull
	Stream<C> streamClasses();

	/**
	 * @return All contained {@link ClassInstance}s.
	 * Warning: This method loads content eagerly, use the lazy {@link #streamClasses()} instead where possible!
	 */
	@NotNull
	Collection<C> getClasses();

	/**
	 * @return Class with passed internal name, if present.
	 */
	@Nullable
	C getClassByName(@NotNull String internalName);

	/**
	 * @return Class with corresponding identity, if present.
	 */
	@Nullable
	C getClassByIdentity(@NotNull Identity clsIdentity);

	@Nullable
	F getFieldByIdentity(@NotNull Identity fldIdentity);

	@Nullable
	M getMethodByIdentity(@NotNull Identity mthIdentity);

	@Nullable
	MA getMethodArgByIdentity(@NotNull Identity argIdentity);

	@Nullable
	MV getMethodVarByIdentity(@NotNull Identity varIdentity);

	@NotNull
	default String getId() {
		return String.valueOf(ID_GENERATOR.getAndIncrement());
	}

	AtomicInteger ID_GENERATOR = new AtomicInteger(0);
}
