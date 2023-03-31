package matcher.bcprovider.jvm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.RecordComponentVisitor;
import org.objectweb.asm.TypePath;

import matcher.bcprovider.BytecodeClassVisitor;

public class JvmBcClassVisitor implements BytecodeClassVisitor {
	public void visitModule(String name, int access, String version) {
	}

	public ClassVisitor asAsmVisitor() {
		JvmBcClassVisitor mainVisitor = this;

		return new ClassVisitor(JvmBytecodeProvider.ASM_VERSION) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				mainVisitor.visit(version, access, name, signature, superName, interfaces);
			}

			@Override
			public void visitSource(String source, String debug) {
				mainVisitor.visitSource(source, debug);
			}

			@Override
			public ModuleVisitor visitModule(String name, int access, String version) {
				mainVisitor.visitModule(name, access, version);
				return null;
			}

			@Override
			public void visitNestHost(String nestHost) {
				mainVisitor.visitNestHost(nestHost);
			}

			@Override
			public void visitOuterClass(String owner, String name, String descriptor) {
				mainVisitor.visitOuterClass(owner, name, descriptor);
			}

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				mainVisitor.visitAnnotation(descriptor, visible);
				return null;
			}

			@Override
			public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
				mainVisitor.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
				return null;
			}

			@Override
			public void visitAttribute(Attribute attribute) {
				mainVisitor.visitAttribute(attribute);
			}

			@Override
			public void visitNestMember(String nestMember) {
				mainVisitor.visitNestMember(nestMember);
			}

			@Override
			public void visitPermittedSubclass(String permittedSubclass) {
				mainVisitor.visitPermittedSubclass(permittedSubclass);
			}

			@Override
			public void visitInnerClass(String name, String outerName, String innerName, int access) {
				mainVisitor.visitInnerClass(name, outerName, innerName, access);
			}

			@Override
			public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
				mainVisitor.visitRecordComponent(name, descriptor, signature);
				return null;
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				mainVisitor.visitField(access, name, descriptor, signature, value);
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				mainVisitor.visitMethod(access, name, descriptor, signature, exceptions);
				return null;
			}
		};
	}
}
