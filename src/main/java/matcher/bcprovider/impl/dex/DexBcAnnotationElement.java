package matcher.bcprovider.impl.dex;

import org.jetbrains.annotations.NotNull;
import com.android.tools.smali.dexlib2.iface.value.EncodedValue;

import matcher.bcprovider.BcAnnotationElement;

public class DexBcAnnotationElement implements BcAnnotationElement {
	DexBcAnnotationElement(@NotNull String name, @NotNull EncodedValue value) {
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
	private final EncodedValue value;
}
