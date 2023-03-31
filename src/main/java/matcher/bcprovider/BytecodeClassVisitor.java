package matcher.bcprovider;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.TypePath;

public interface BytecodeClassVisitor {
	default void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
	}

	default void visitSource(String source, String debug) {
	}

	default void visitNestHost(String nestHost) {
	}

	default void visitOuterClass(String owner, String name, String descriptor) {
	}

	default void visitAnnotation(String descriptor, boolean visible) {
	}

	default void visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
	}

	default void visitAttribute(Attribute attribute) {
	}

	default void visitNestMember(String nestMember) {
	}

	default void visitPermittedSubclass(String permittedSubclass) {
	}

	default void visitInnerClass(String name, String outerName, String innerName, int access) {
	}

	default void visitRecordComponent(String name, String descriptor, String signature) {
	}

	default void visitField(int access, String name, String descriptor, String signature, Object value) {
	}

	default void visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
	}

	default void visitEnd() {
	}
}
