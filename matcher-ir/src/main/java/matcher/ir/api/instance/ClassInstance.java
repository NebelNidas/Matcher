package matcher.ir.api.instance;

import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.objectweb.asm.tree.ClassNode;

import matcher.ir.api.env.ClassEnv;
import matcher.ir.api.identity.Identity;
import matcher.ir.api.instance.info.ClassNameInfo;
import matcher.ir.api.instance.info.InnerClassStatusInfo;
import matcher.ir.impl.ClassInstanceImpl;

public non-sealed interface ClassInstance extends Instance {
	@Override
	@NotNull
	default String getId() {
		return getNameInfo().getDescriptor();
	}

	@Override
	@NotNull
	ClassNameInfo getNameInfo();

	@Override
	Identity getIdentity();

	/**
	 * Gets all backing ASM nodes. May be lazily constructed according to cache policy.
	 */
	@NotNull
	Stream<? extends ClassNode> getAsmNodes();

	InnerClassStatusInfo getInnerClassStatus();

	/**
	 * @return
	 */
	@NotNull
	Stream<? extends ClassInstanceCandidate> getOuterClassCandidates();

	/**
	 * @return The likeliest outer class, according to the originating {@link ClassEnv}'s settings.
	 */
	@Nullable
	ClassInstanceCandidate getLikeliestOuterClass();

	/**
	 * @return
	 */
	default Stream<? extends ClassInstanceCandidate> getOutermostClassCandidates() {
		return getOuterClassCandidates().findAny().isEmpty()
				? Stream.of(new ClassInstanceCandidate(1, this))
				: getOuterClassCandidates().flatMap(candidate -> candidate.getInstance().getOutermostClassCandidates());
	};

	/**
	 * @return The likeliest outermost class, or itself, if no parent is present.
	 */
	@NotNull
	default ClassInstanceCandidate getLikeliestOutermostClass() {
		ClassInstanceCandidate outer = getLikeliestOuterClass();
		return outer == null ? new ClassInstanceCandidate(1, this) : outer.instance.getLikeliestOutermostClass();
	}

	@Nullable
	ClassInstance getSuperClass();

	/**
	 * @return List of implemented interfaces.
	 */
	@NotNull
	Stream<? extends ClassInstance> getInterfaces();

	/**
	 * @return A lazily constructing stream of all inner class candidates present in the originating {@link ClassEnv}.
	 */
	@NotNull
	Stream<? extends ClassInstanceCandidate> getInnerClassCandidates();

	/**
	 * @return A lazily constructing stream of all subclasses present in the originating {@link ClassEnv}.
	 */
	@NotNull
	Stream<? extends ClassInstance> getSubclasses();

	/**
	 * @return All fields in the declared order.
	 */
	@NotNull
	Stream<? extends FieldInstance> getFields();

	/**
	 * @return All fields in the declared order.
	 */
	@NotNull
	Stream<? extends MethodInstance> getMethods();

	class ClassInstanceCandidate {
		private final float probability;
		private final ClassInstance instance;

		protected ClassInstanceCandidate(@Range(from = 0, to = 1) float probability, ClassInstance instance) {
			this.probability = probability;
			this.instance = instance;
		}

		protected float getProbability() {
			return probability;
		}

		protected ClassInstance getInstance() {
			return instance;
		}
	}

	interface ClassInstanceBuilder extends InstanceBuilder<ClassInstance> {
		@Override
		void setName(@NotNull String internalName);
	}

	static ClassInstanceBuilder builder() {
		return new ClassInstanceImpl.Builder();
	}
}
