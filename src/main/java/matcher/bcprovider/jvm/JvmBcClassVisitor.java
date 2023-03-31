package matcher.bcprovider.jvm;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;

import matcher.bcprovider.BytecodeClassVisitor;

public class JvmBcClassVisitor implements BytecodeClassVisitor {
	protected JvmBcClassVisitor() {
		this.classVisitor = new ClassVisitorImpl();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		classVisitor.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public void visitSource(String source, String debug) {
		classVisitor.visitSource(source, debug);
	}

	public ModuleVisitor visitModule(String name, int access, String version) {
		return classVisitor.visitModule(name, access, version);
	}

	@Override
	public void visitNestHost(String nestHost) {
		classVisitor.visitNestHost(nestHost);
	}

	@Override
	public void visitOuterClass(String owner, String name, String descriptor) {
		classVisitor.visitOuterClass(owner, name, descriptor);
	}

	@Override
	public void visitAnnotation(String descriptor, boolean visible) {
		classVisitor.visitAnnotation(descriptor, visible);
	}

	@Override
	public void visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
		classVisitor.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}

	@Override
	public void visitAttribute(Attribute attribute) {
		classVisitor.visitAttribute(attribute);
	}

	@Override
	public void visitNestMember(String nestMember) {
		classVisitor.visitNestMember(nestMember);
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		classVisitor.visitPermittedSubclass(permittedSubclass);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		classVisitor.visitInnerClass(name, outerName, innerName, access);
	}

	@Override
	public void visitRecordComponent(String name, String descriptor, String signature) {
		classVisitor.visitRecordComponent(name, descriptor, signature);
	}

	@Override
	public void visitField(int access, String name, String descriptor, String signature, Object value) {
		classVisitor.visitField(access, name, descriptor, signature, value);
	}

	@Override
	public void visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		classVisitor.visitMethod(access, name, descriptor, signature, exceptions);
	}

	@Override
	public void visitEnd() {
		classVisitor.visitEnd();
	}

	private class ClassVisitorImpl extends ClassVisitor {
		protected ClassVisitorImpl() {
			super(Opcodes.ASM9);
		}
	}

	protected ClassVisitor classVisitor;
}
