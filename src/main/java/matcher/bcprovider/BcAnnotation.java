package matcher.bcprovider;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public interface BcAnnotation {
	/**
	 * 0 is invisible, higher values are defined
	 * by implementors.
	 */
	@Range(from = 0, to = Integer.MAX_VALUE)
	int getVisibility();

	boolean isTypeAnnotation();

	@NotNull
	String getDescriptor();

	@NotNull
	List<? extends BcAnnotationElement> getElements();
}
