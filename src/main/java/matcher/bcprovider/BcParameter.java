package matcher.bcprovider;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BcParameter {
	@Nullable
	String getName();

	int getAccess();

	@NotNull
	List<? extends BcAnnotation> getAnnotations();
}
