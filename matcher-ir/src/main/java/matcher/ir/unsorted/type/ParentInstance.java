package matcher.ir.type;

public interface ParentInstance<T extends Instance<T>> extends Instance<T> {
	boolean hasMappedChildren();
}
