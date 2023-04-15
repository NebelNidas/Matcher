package matcher.bcprovider;

import java.util.List;

import org.jetbrains.annotations.NotNull;

public interface Annotatable {
	@NotNull
	List<? extends BcAnnotation> getAnnotations();
}
