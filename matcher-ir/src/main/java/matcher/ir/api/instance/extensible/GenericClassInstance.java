package matcher.ir.api.instance.extensible;

import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import matcher.ir.api.env.ClassEnv;
import matcher.ir.api.instance.ClassInstance;
import matcher.ir.api.instance.extensible.info.GenericClassNameInfo;

public interface GenericClassInstance<
		C extends GenericClassInstance<C, F, M, MA, MV>, // own type
		F extends GenericFieldInstance<C, F, M, MA, MV>,
		M extends GenericMethodInstance<C, F, M, MA, MV>,
		MA extends GenericMethodArgInstance<C, F, M, MA, MV>,
		MV extends GenericMethodVarInstance<C, F, M, MA, MV>> extends ClassInstance {
	@Override
	@NotNull
	GenericClassNameInfo<C, F, M, MA, MV> getNameInfo();

	@Override
	@NotNull
	Stream<? extends GenericClassInstanceCandidate<C, F, M, MA, MV>> getOuterClassCandidates();

	/**
	 * @return The likeliest outer class, according to the originating {@link ClassEnv}'s settings.
	 */
	@Nullable
	GenericClassInstanceCandidate<C, F, M, MA, MV> getLikeliestOuterClass();

	/**
	 * @return
	 */
	@SuppressWarnings("unchecked")
	default Stream<? extends GenericClassInstanceCandidate<C, F, M, MA, MV>> getOutermostClassCandidates() {
		return getOuterClassCandidates().findAny().isEmpty()
				? Stream.of(new GenericClassInstanceCandidate<C, F, M, MA, MV>(1, (C) this))
				: getOuterClassCandidates().flatMap((GenericClassInstanceCandidate<C, F, M, MA, MV> candidate) -> candidate.getInstance().getOutermostClassCandidates());
	};

	/**
	 * @return The likeliest outermost class, or itself, if no parent is present.
	 */
	@NotNull
	@SuppressWarnings("unchecked")
	default GenericClassInstanceCandidate<C, F, M, MA, MV> getLikeliestOutermostClass() {
		GenericClassInstanceCandidate<C, F, M, MA, MV> outer = getLikeliestOuterClass();
		return outer == null ? new GenericClassInstanceCandidate<>(1, (C) this) : outer.getInstance().getLikeliestOutermostClass();
	}

	@Nullable
	C getSuperClass();

	/**
	 * @return List of implemented interfaces.
	 */
	@NotNull
	Stream<? extends C> getInterfaces();

	/**
	 * @return A lazily constructing stream of all inner class candidates present in the originating {@link ClassEnv}.
	 */
	@NotNull
	Stream<? extends GenericClassInstanceCandidate<C, F, M, MA, MV>> getInnerClassCandidates();

	/**
	 * @return A lazily constructing stream of all subclasses present in the originating {@link ClassEnv}.
	 */
	@NotNull
	Stream<? extends C> getSubclasses();

	/**
	 * @return All fields in the declared order.
	 */
	@NotNull
	Stream<? extends F> getFields();

	/**
	 * @return All fields in the declared order.
	 */
	@NotNull
	Stream<? extends M> getMethods();

	class GenericClassInstanceCandidate<
			C extends GenericClassInstance<C, F, M, MA, MV>,
			F extends GenericFieldInstance<C, F, M, MA, MV>,
			M extends GenericMethodInstance<C, F, M, MA, MV>,
			MA extends GenericMethodArgInstance<C, F, M, MA, MV>,
			MV extends GenericMethodVarInstance<C, F, M, MA, MV>> extends ClassInstanceCandidate {
		GenericClassInstanceCandidate(@Range(from = 0, to = 1) float probability, C instance) {
			super(probability, instance);
		}

		@Override
		@SuppressWarnings("unchecked")
		protected C getInstance() {
			return (C) super.getInstance();
		}
	}

	interface ClassInstanceBuilder<
			C extends GenericClassInstance<C, F, M, MA, MV>,
			F extends GenericFieldInstance<C, F, M, MA, MV>,
			M extends GenericMethodInstance<C, F, M, MA, MV>,
			MA extends GenericMethodArgInstance<C, F, M, MA, MV>,
			MV extends GenericMethodVarInstance<C, F, M, MA, MV>> extends InstanceBuilder<C> {
		@Override
		void setName(@NotNull String internalName);
	}
}
