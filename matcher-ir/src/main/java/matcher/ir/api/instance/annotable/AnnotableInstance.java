package matcher.ir.api.instance.annotable;

import java.util.SortedMap;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.instance.annotable.info.AnnotableNameInfo;

public sealed interface AnnotableInstance permits AnnotableClassInstance {
	@NotNull
	AnnotableNameInfo getNameInfo();

	/**
	 * @return A map of all namespaces' indices and names used for adding
	 * annotations to the current instance, sorted by index (ascending).
	 */
	@NotNull
	SortedMap<Integer, String> getAnnotationNamespaces();

	String getComment(int namespace);
	void setComment(int namespace, String comment);
}
