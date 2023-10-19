package matcher.ir.api.env;

import matcher.ir.api.instance.extensible.ClassInstance;
import matcher.ir.impl.MultiClassEnvImpl;

public class ClassEnvs {
	private ClassEnvs() {
	}

	/**
	 * @return Unifying view on multiple {@link ClassEnv}s which provide binary compatible implementations of
	 * possibly shared {@matcher.ir.api.type.ClassInstance}s.
	 */
	public static final <T extends ClassInstance> CombiningClassEnv<T> newInstanceNameCompatibleCombiningClassEnv(T type, ClassEnv<T>... children) {
		return new MultiClassEnvImpl<T>(children);
	}
}
