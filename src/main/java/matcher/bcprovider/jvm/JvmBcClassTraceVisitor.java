// package matcher.bcprovider.jvm;

// import java.io.PrintWriter;

// import org.objectweb.asm.AnnotationVisitor;
// import org.objectweb.asm.Attribute;
// import org.objectweb.asm.ClassVisitor;
// import org.objectweb.asm.FieldVisitor;
// import org.objectweb.asm.MethodVisitor;
// import org.objectweb.asm.ModuleVisitor;
// import org.objectweb.asm.RecordComponentVisitor;
// import org.objectweb.asm.TypePath;
// import org.objectweb.asm.util.Printer;
// import org.objectweb.asm.util.TraceClassVisitor;

// public class JvmBcClassTraceVisitor extends JvmBcClassVisitor {
// 	public JvmBcClassTraceVisitor(ClassVisitor classVisitor, Printer printer, PrintWriter printWriter) {
// 		super(JvmBytecodeProvider.ASM_VERSION, classVisitor);

// 		this.traceClassVisitor = new TraceClassVisitor(classVisitor, printer, printWriter);
// 	}

// 	@Override
// 	public void visit(
// 			int version,
// 			int access,
// 			String name,
// 			String signature,
// 			String superName,
// 			String[] interfaces) {
// 		traceClassVisitor.visit(version, access, name, signature, superName, interfaces);
// 	}

// 	@Override
// 	public void visitSource(String file, String debug) {
// 		traceClassVisitor.visitSource(file, debug);
// 	}

// 	@Override
// 	public ModuleVisitor visitModule(String name, int flags, String version) {
// 		return traceClassVisitor.visitModule(name, flags, version);
// 	}

// 	@Override
// 	public void visitNestHost(String nestHost) {
// 		traceClassVisitor.visitNestHost(nestHost);
// 	}

// 	@Override
// 	public void visitOuterClass(String owner, String name, String descriptor) {
// 		traceClassVisitor.visitOuterClass(owner, name, descriptor);
// 	}

// 	@Override
// 	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
// 		return traceClassVisitor.visitAnnotation(descriptor, visible);
// 	}

// 	@Override
// 	public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
// 		return traceClassVisitor.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
// 	}

// 	@Override
// 	public void visitAttribute(Attribute attribute) {
// 		traceClassVisitor.visitAttribute(attribute);
// 	}

// 	@Override
// 	public void visitNestMember(String nestMember) {
// 		traceClassVisitor.visitNestMember(nestMember);
// 	}

// 	@Override
// 	public void visitPermittedSubclass(String permittedSubclass) {
// 		traceClassVisitor.visitPermittedSubclass(permittedSubclass);
// 	}

// 	@Override
// 	public void visitInnerClass(String name, String outerName, String innerName, int access) {
// 		traceClassVisitor.visitInnerClass(name, outerName, innerName, access);
// 	}

// 	@Override
// 	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
// 		return traceClassVisitor.visitRecordComponent(name, descriptor, signature);
// 	}

// 	@Override
// 	public FieldVisitor visitField(
// 			int access,
// 			String name,
// 			String descriptor,
// 			String signature,
// 			Object value) {
// 		return traceClassVisitor.visitField(access, name, descriptor, signature, value);
// 	}

// 	@Override
// 	public MethodVisitor visitMethod(
// 			int access,
// 			String name,
// 			String descriptor,
// 			String signature,
// 			String[] exceptions) {
// 		return traceClassVisitor.visitMethod(access, name, descriptor, signature, exceptions);
// 	}

// 	@Override
// 	public void visitEnd() {
// 		traceClassVisitor.visitEnd();
// 	}

// 	private final TraceClassVisitor traceClassVisitor;
// }
