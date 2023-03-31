// package matcher.bcprovider.jvm;

// import org.objectweb.asm.AnnotationVisitor;
// import org.objectweb.asm.Attribute;
// import org.objectweb.asm.ClassReader;
// import org.objectweb.asm.ClassWriter;
// import org.objectweb.asm.FieldVisitor;
// import org.objectweb.asm.Handle;
// import org.objectweb.asm.MethodVisitor;
// import org.objectweb.asm.ModuleVisitor;
// import org.objectweb.asm.Opcodes;
// import org.objectweb.asm.RecordComponentVisitor;
// import org.objectweb.asm.TypePath;

// public class JvmBcClassWriter extends JvmBcClassVisitor {
// 	public JvmBcClassWriter(int flags) {
// 		this.classWriter = new ClassWriter(flags);
// 	}

// 	public boolean hasFlags(final int flags) {
// 		return this.classWriter.hasFlags(flags);
// 	}

// 	@Override
// 	public final void visit(
// 			int version,
// 			int access,
// 			String name,
// 			String signature,
// 			String superName,
// 			String[] interfaces) {
// 		this.classWriter.visit(version, access, name, signature, superName, interfaces);
// 	}

// 	@Override
// 	public final void visitSource(String file, String debug) {
// 		this.classWriter.visitSource(file, debug);
// 	}

// 	@Override
// 	public final ModuleVisitor visitModule(String name, int access, String version) {
// 		return this.classWriter.visitModule(name, access, version);
// 	}

// 	@Override
// 	public final void visitNestHost(String nestHost) {
// 		this.classWriter.visitNestHost(nestHost);
// 	}

// 	@Override
// 	public final void visitOuterClass(String owner, String name, String descriptor) {
// 		this.classWriter.visitOuterClass(owner, name, descriptor);
// 	}

// 	@Override
// 	public final AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
// 		return this.classWriter.visitAnnotation(descriptor, visible);
// 	}

// 	@Override
// 	public final AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
// 		return this.classWriter.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
// 	}

// 	@Override
// 	public final void visitAttribute(Attribute attribute) {
// 		this.classWriter.visitAttribute(attribute);
// 	}

// 	@Override
// 	public final void visitNestMember(String nestMember) {
// 		this.classWriter.visitNestMember(nestMember);
// 	}

// 	@Override
// 	public final void visitPermittedSubclass(String permittedSubclass) {
// 		this.classWriter.visitPermittedSubclass(permittedSubclass);
// 	}

// 	@Override
// 	public final void visitInnerClass(String name, String outerName, String innerName, int access) {
// 		this.classWriter.visitInnerClass(name, outerName, innerName, access);
// 	}

// 	@Override
// 	public final RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
// 		return this.classWriter.visitRecordComponent(name, descriptor, signature);
// 	}

// 	@Override
// 	public final FieldVisitor visitField(
// 			final int access,
// 			final String name,
// 			final String descriptor,
// 			final String signature,
// 			final Object value) {
// 		return classWriter.visitField(access, name, descriptor, signature, value);
// 	}

// 	@Override
// 	public final MethodVisitor visitMethod(
// 			final int access,
// 			final String name,
// 			final String descriptor,
// 			final String signature,
// 			final String[] exceptions) {
// 		return classWriter.visitMethod(access, name, descriptor, signature, exceptions);
// 	}

// 	@Override
// 	public final void visitEnd() {
// 		classWriter.visitEnd();
// 	}

// 	public byte[] toByteArray() {
// 		return this.classWriter.toByteArray();
// 	}

// 	public int newConst(final Object value) {
// 		return classWriter.newConst(value);
// 	}

// 	public int newUTF8(final String value) {
// 		return classWriter.newUTF8(value);
// 	}

// 	public int newClass(final String value) {
// 		return classWriter.newClass(value);
// 	}

// 	public int newMethodType(final String methodDescriptor) {
// 		return classWriter.newMethodType(methodDescriptor);
// 	}

// 	public int newModule(final String moduleName) {
// 		return classWriter.newModule(moduleName);
// 	}

// 	public int newPackage(final String packageName) {
// 		return classWriter.newPackage(packageName);
// 	}

// 	public int newHandle(
// 			final int tag,
// 			final String owner,
// 			final String name,
// 			final String descriptor,
// 			final boolean isInterface) {
// 		return classWriter.newHandle(tag, owner, name, descriptor, isInterface);
// 	}

// 	public int newConstantDynamic(
// 			final String name,
// 			final String descriptor,
// 			final Handle bootstrapMethodHandle,
// 			final Object... bootstrapMethodArguments) {
// 		return classWriter.newConstantDynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
// 	}

// 	public int newInvokeDynamic(
// 			final String name,
// 			final String descriptor,
// 			final Handle bootstrapMethodHandle,
// 			final Object... bootstrapMethodArguments) {
// 		return classWriter.newInvokeDynamic(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
// 	}

// 	public int newField(final String owner, final String name, final String descriptor) {
// 		return classWriter.newField(owner, name, descriptor);
// 	}

// 	public int newMethod(
// 			final String owner, final String name, final String descriptor, final boolean isInterface) {
// 		return newMethod(owner, name, descriptor, isInterface);
// 	}

// 	public int newNameType(final String name, final String descriptor) {
// 		return classWriter.newNameType(name, descriptor);
// 	}

// 	private final ClassWriter classWriter;
// }
