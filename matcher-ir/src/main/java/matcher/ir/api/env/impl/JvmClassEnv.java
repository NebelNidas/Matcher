package matcher.ir.api.env.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import matcher.ir.api.env.ClassEnv;
import matcher.ir.api.env.PhantomCapableClassEnv;
import matcher.ir.api.identity.Identity;
import matcher.ir.api.instance.FieldInstance;
import matcher.ir.api.instance.MethodArgInstance;
import matcher.ir.api.instance.MethodInstance;
import matcher.ir.api.instance.MethodVarInstance;
import matcher.ir.api.instance.extensible.ClassInstance;

/**
 * Built-in Singleton {@link ClassEnv} used for providing Java standard library classes.
 */
public final class JvmClassEnv implements PhantomCapableClassEnv {
	private static final JvmClassEnv INSTANCE = new JvmClassEnv();
	private final Map<String, ClassInstance> classesByInternalName = new HashMap<>();

	public static JvmClassEnv getInstance() {
		return INSTANCE;
	}

	@Override
	public @NotNull Set<String> getRealClassNames() {
		return Collections.emptySet();
	}

	@Override
	public @NotNull Set<String> getPhantomClassNames() {
		return Collections.unmodifiableSet(classesByInternalName.keySet());
	}

	@Override
	public @NotNull Stream streamRealClasses() {
		return Stream.empty();
	}

	@Override
	public @NotNull Stream streamPhantomClasses() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'streamPhantomClasses'");
	}

	@Override
	public @NotNull Collection getRealClasses() {
		return Collections.emptyList();
	}

	@Override
	public @NotNull Collection getPhantomClasses() {
		return Collections.unmodifiableCollection(classesByInternalName.values());
	}

	@Override
	public @Nullable ClassInstance getClassByName(@NotNull String internalName) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getClassByName'");
	}

	@Override
	public @Nullable ClassInstance getClassByIdentity(@NotNull Identity clsIdentity) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getClassByIdentity'");
	}

	@Override
	public @Nullable FieldInstance getFieldByIdentity(@NotNull Identity fldIdentity) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getFieldByIdentity'");
	}

	@Override
	public @Nullable MethodInstance getMethodByIdentity(@NotNull Identity mthIdentity) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getMethodByIdentity'");
	}

	@Override
	public @Nullable MethodArgInstance getMethodArgByIdentity(@NotNull Identity argIdentity) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getMethodArgByIdentity'");
	}

	@Override
	public @Nullable MethodVarInstance getMethodVarByIdentity(@NotNull Identity varIdentity) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getMethodVarByIdentity'");
	}

	@Override
	public @Nullable ClassInstance getRealClassByIdentity(@NotNull Identity clsIdentity) {
		return null;
	}

	@Override
	public @Nullable FieldInstance getRealFieldByIdentity(@NotNull Identity fldIdentity) {
		return null;
	}

	@Override
	public @Nullable MethodInstance getRealMethodByIdentity(@NotNull Identity mthIdentity) {
		return null;
	}

	@Override
	public @Nullable MethodArgInstance getRealMethodArgByIdentity(@NotNull Identity argIdentity) {
		return null;
	}

	@Override
	public @Nullable MethodVarInstance getRealMethodVarByIdentity(@NotNull Identity varIdentity) {
		return null;
	}
}
