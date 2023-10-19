package matcher.ir.api.identity;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.env.ClassEnv;
import matcher.ir.api.env.CombiningClassEnv;
import matcher.ir.api.instance.Instance;

/**
 * {@link Instance}s can differ between {@link ClassEnv}s, even if they represent the same logical unit.
 * Sometimes the change is small, like an additional member or added property, in which case they can
 * be merged into a single {@link Instance} view via the {@link CombiningClassEnv} for example.
 *
 * <p>In other cases, the differences are fundamentally incompatible, caused by a class rename (class {@code A}
 * in ClassEnv 1 having been renamed to {@code B} in ClassEnv 2), a member being moved between classes etc.
 * The represented logical unit can still be considered the same, but basic properties of the concrete implementations
 * differ (e.g. fully qualified name), making merging into a single view impossible. Hence why this {@link Identity} interface exists, allowing us to track the logical unit
 * across multiple {@link ClassEnv}s without issue.
 *
 * <p>How large the diff between two {@link Instance}s is allowed to be for them to still be considered of the same identity
 * (logical unit) is entirely up to the implementor.
 */
public interface Identity {
	/**
	 * @return The originating {@link IdentityRegistry}.
	 */
	@NotNull
	IdentityRegistry getRegistry();

	/**
	 * @return An unmodifiable view of the map holding the ID of the concrete instance of this identity per {@link ClassEnv}.
	 */
	@NotNull
	Map<ClassEnv<?, ?, ?, ?, ?>, @NotNull String> getInstanceIdsByEnv();

	/**
	 * Non-standard implementations may allow linking and unlinking different Identity objects dynamically,
	 * meaning object equality checking doesn't suffice for checking semantic identity equality.
	 * So to check whether or not two {@link Identity} objects point to semantically identical {@link Instance}s, use this method.
	 * Overriding {@link #equals(Object)} wouldn't work, since dynamic (un)linking requires the objects to stay un-equal for use in Maps etc.
	 */
	boolean matches(Identity other);
}
