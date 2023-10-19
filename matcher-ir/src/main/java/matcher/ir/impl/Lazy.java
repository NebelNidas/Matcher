package matcher.ir.impl;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

abstract sealed class Lazy<T> {
	public abstract T get();

	/**
	 * @return A {@link Lazy} instance complying with {@link MatcherIrConfig#getLazyPolicy()}.
	 */
	static <T> Lazy<T> of(Supplier<T> supplier) {
		return switch (MatcherIrConfig.getLazyPolicy()) {
		case NONE -> new PrecomputedLazy<>(supplier.get());
		case NON_THREAD_SAFE -> new SimpleLazy<>(supplier);
		case LOCK_BASED_THREAD_SAFE -> new ReentrantLockLazy<>(supplier);
		case SYNCHRONIZED_BASED_THREAD_SAFE -> new SynchronizedLazy<>(supplier);
		};
	}

	static <T> Lazy<T> ofPrecomputed(T value) {
		return new PrecomputedLazy<>(value);
	}

	static <T> Lazy<T> ofNonThreadSafe(Supplier<T> supplier) {
		return new SimpleLazy<>(supplier);
	}

	static <T> Lazy<T> ofLockBasedThreadSafe(Supplier<T> supplier) {
		return new ReentrantLockLazy<>(supplier);
	}

	static <T> Lazy<T> ofSynchronizedBasedThreadSafe(Supplier<T> supplier) {
		return new SynchronizedLazy<>(supplier);
	}

	static final class PrecomputedLazy<T> extends Lazy<T> {
		private final T value;

		PrecomputedLazy(T value) {
			this.value = value;
		}

		public T get() {
			return value;
		}
	}

	static final class SimpleLazy<T> extends Lazy<T> {
		private Supplier<T> supplier;
		private T value;
		private boolean calculated;

		SimpleLazy(Supplier<T> supplier) {
			this.supplier = supplier;
		}

		public T get() {
			if (calculated) return value;

			value = supplier.get();
			calculated = true;
			supplier = null; // Allow supplier to be garbage-collected

			return value;
		}
	}

	static final class ReentrantLockLazy<T> extends Lazy<T> {
		private final ReentrantLock lock = new ReentrantLock();
		private Supplier<T> supplier;
		private volatile T value;
		private volatile boolean calculated;

		ReentrantLockLazy(Supplier<T> supplier) {
			this.supplier = supplier;
		}

		public T get() {
			if (calculated) return value;
			lock.lock();

			try {
				if (calculated) return value;
				value = supplier.get();
				calculated = true;
			} finally {
				lock.unlock();
			}

			supplier = null; // Allow supplier to be garbage-collected
			return value;
		}
	}

	static final class SynchronizedLazy<T> extends Lazy<T> {
		private Supplier<T> supplier;
		private volatile T value;
		private volatile boolean calculated;

		SynchronizedLazy(Supplier<T> supplier) {
			this.supplier = supplier;
		}

		public T get() {
			if (calculated) return value;

			synchronized (supplier) {
				if (calculated) return value;
				value = supplier.get();
				calculated = true;
			}

			supplier = null; // Allow supplier to be garbage-collected
			return value;
		}
	}
}
