package matcher.ir.api.env;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Spliterator;
import java.util.function.Consumer;

import matcher.ir.api.instance.extensible.ClassInstance;
import matcher.ir.api.instance.extensible.ClassInstanceBuilder;

public class ClassInstanceGeneratingSpliterator implements Spliterator<ClassInstance> {
	public ClassInstanceGeneratingSpliterator(Collection<String> classNames, ClassInstanceBuilder builder) {
		this.classNames = new LinkedHashSet<>(classNames);
		this.builder = builder;
	}

	@Override
	public boolean tryAdvance(Consumer<? super ClassInstance> action) {
		if (pos >= classNames.size() - 1) return false;

		action.accept(builder.build());
		return true;
	}

	@Override
	public Spliterator<ClassInstance> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return classNames.size();
	}

	@Override
	public int characteristics() {
		return DISTINCT | SIZED | NONNULL | IMMUTABLE;
	}

	private final LinkedHashSet<String> classNames;
	private final ClassInstanceBuilder builder;
	private int pos = 0;
}
