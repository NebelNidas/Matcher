package matcher.bcprovider;

public class SharedBcAccessFlags {
	/** Classes, fields and methods. */
	public static int PUBLIC = 0x1;

	/** Classes, fields and methods. */
	public static int PRIVATE = 0x2;

	/** Classes, fields and methods. */
	public static int PROTECTED = 0x4;

	/** Classes (only in Dalvik bytecode), fields and methods. */
	public static int STATIC = 0x8;

	/** Classes, fields, methods and parameters. */
	public static int FINAL = 0x10;

	/** Methods. */
	public static int SYNCHRONIZED = 0x20;

	/** Fields. */
	public static int VOLATILE = 0x40;

	/** Methods. */
	public static int BRIDGE = 0x40;

	/** Fields. */
	public static int TRANSIENT = 0x80;

	/** Methods. */
	public static int VARARGS = 0x80;

	/** Methods. */
	public static int NATIVE = 0x100;

	/** Classes. */
	public static int INTERFACE = 0x200;

	/** Classes and methods. */
	public static int ABSTRACT = 0x400;

	/** Methods. */
	public static int STRICT = 0x800;

	/** Classes, methods, fields, parameters and JVM modules. */
	public static int SYNTHETIC = 0x1000;

	/** Classes. */
	public static int ANNOTATION = 0x2000;

	/** Classes and fields. */
	public static int ENUM = 0x4000;

	/** Classes. */
	public static int RECORD = 0x10000;

	/** Classes, methods and fields. */
	public static int DEPRECATED = 0x20000;
}
