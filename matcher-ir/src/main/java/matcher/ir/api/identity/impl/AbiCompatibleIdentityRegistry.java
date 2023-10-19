package matcher.ir.api.identity.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import matcher.ir.api.env.ClassEnv;
import matcher.ir.api.identity.Identity;
import matcher.ir.api.identity.IdentityRegistry;

/**
 * Uses the same {@link Identity} object per instance ID, so two instances with
 * the same ID always have the same identity, no matter the originating {@link ClassEnv}.
 */
public class AbiCompatibleIdentityRegistry implements IdentityRegistry {
	private final Map<String, AbiCompatibleIdentity> identitiesByInstanceId = Collections.synchronizedMap(new HashMap<>());

	@Nullable
	public Identity getIdentity(ClassEnv<?, ?, ?, ?, ?> env, String instanceId) {
		if (env == null || instanceId == null) return null;

		AbiCompatibleIdentity identity = identitiesByInstanceId.computeIfAbsent(instanceId, (unused) -> new AbiCompatibleIdentity(this));
		identity.instanceIdsByEnv.put(env, instanceId);
		return identity;
	}

	private static class AbiCompatibleIdentity implements Identity {
		private final AbiCompatibleIdentityRegistry registry;
		final Map<ClassEnv<?, ?, ?, ?, ?>, String> instanceIdsByEnv = new HashMap<>();

		AbiCompatibleIdentity(AbiCompatibleIdentityRegistry registry) {
			this.registry = registry;
		}

		@Override
		@NotNull
		public IdentityRegistry getRegistry() {
			return registry;
		}

		@Override
		@NotNull
		public Map<ClassEnv<?, ?, ?, ?, ?>, @NotNull String> getInstanceIdsByEnv() {
			return instanceIdsByEnv;
		}

		@Override
		public boolean matches(Identity other) {
			if (other == this) return true;

			synchronized (instanceIdsByEnv) {
				for (Map.Entry<ClassEnv<?, ?, ?, ?, ?>, String> entry : instanceIdsByEnv.entrySet()) {
					if (entry.getValue().equals(other.getInstanceIdsByEnv().get(entry.getKey()))) return true;
				}
			}

			return false;
		}
	}
}
