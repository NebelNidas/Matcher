package matcher.ir.api.identity;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.env.ClassEnv;

public interface IdentityRegistry {
	@NotNull
	Identity getIdentity(ClassEnv<?, ?, ?, ?, ?> env, String scopedInstanceId);
}
