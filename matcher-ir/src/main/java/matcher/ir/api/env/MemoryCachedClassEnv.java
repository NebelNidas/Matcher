package matcher.ir.api.env;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import matcher.ir.api.instance.extensible.ClassInstance;

public abstract class MemoryCachedClassEnv<T extends ClassInstance> implements ClassEnv<T> {
	@Override
	public Stream<T> streamClasses() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'streamClasses'");
	}

	@Override
	public Set<T> getClasses() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getClasses'");
	}

	@Override
	public T getClassByName(String internalName) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getClass'");
	}

	protected Map<String, T> classesByName = new HashMap<>();
	protected Map<Integer, T> classesByUuid = new HashMap<>();
	protected boolean fullyLoaded;
}
