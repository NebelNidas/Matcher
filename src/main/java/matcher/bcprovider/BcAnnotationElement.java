package matcher.bcprovider;

import org.jetbrains.annotations.NotNull;

public interface BcAnnotationElement {
	@NotNull
	String getName();

	@NotNull
	Object getValue();
}
