package matcher.bcprovider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BcField extends Annotatable {
	@NotNull
	String getName();

	@NotNull
	String getDescriptor();

	@Nullable
	String getSignature();

	@Nullable
	Object getValue();

	int getAccess();
}
