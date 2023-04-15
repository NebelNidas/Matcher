package matcher.bcprovider.impl.jvm;

import org.jetbrains.annotations.NotNull;

import matcher.bcprovider.BcAnnotationElement;

public class JvmBcAnnotationElement implements BcAnnotationElement {
	JvmBcAnnotationElement(@NotNull String name, @NotNull Object value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		assert name != null;
		return name;
	}

	@Override
	public Object getValue() {
		assert value != null;
		return value;
	}

	private final String name;
	private final Object value;
}
