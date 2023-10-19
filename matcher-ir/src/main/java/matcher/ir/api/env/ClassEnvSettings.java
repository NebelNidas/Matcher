package matcher.ir.api.env;

import java.util.BitSet;

public class ClassEnvSettings {
	private final LazyPolicy lazyPolicy;
	private final BitSet innerClassDeterminationTechnique;

	public ClassEnvSettings(LazyPolicy lazyPolicy, BitSet innerClassDeterminationTechnique) {
		this.lazyPolicy = lazyPolicy;
		this.innerClassDeterminationTechnique = innerClassDeterminationTechnique;
	}

	public LazyPolicy getLazyPolicy() {
		return lazyPolicy;
	}

	public BitSet getInnerClassDeterminationTechnique() {
		return innerClassDeterminationTechnique;
	}

	public enum LazyPolicy {
		NONE,
		NON_THREAD_SAFE,
		LOCK_BASED_THREAD_SAFE,
		SYNCHRONIZED_BASED_THREAD_SAFE
	}

	public class InnerClassDeterminationTechniques {
		public static final int NAME = 1;
		public static final int OWN_ATTRIBUTES = 2;
		public static final int CLASSPATH_ATTRIBUTES = 3;
		public static final int DETAILED_ANALYSIS = 4;
	}
}
