package matcher.ir.api.instance.annotable;

public sealed interface AnnotableParentInstance permits AnnotableClassInstance {
	boolean hasAnnotatedChildren();
}
