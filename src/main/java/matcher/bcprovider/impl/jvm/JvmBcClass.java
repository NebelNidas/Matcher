package matcher.bcprovider.impl.jvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import matcher.bcprovider.BcClass;
import matcher.bcprovider.BcClassRemapNameProvider;

public class JvmBcClass implements BcClass {
	public JvmBcClass() {
		this.asmNode = new ClassNode();
	}

	public JvmBcClass(ClassNode asmNode) {
		this.asmNode = asmNode;
	}

	@Override
	public List<JvmBcField> getFields() {
		if (asmNode.fields == null) {
			return Collections.emptyList();
		}

		List<JvmBcField> bcFields = new ArrayList<>(asmNode.fields.size());
		asmNode.fields.forEach(fld -> bcFields.add(new JvmBcField(fld)));
		return Collections.unmodifiableList(bcFields);
	}

	@Override
	public List<JvmBcMethod> getMethods() {
		if (asmNode.methods == null) {
			return Collections.emptyList();
		}

		List<JvmBcMethod> bcMethods = new ArrayList<>(asmNode.methods.size());
		asmNode.methods.forEach(mth -> bcMethods.add(new JvmBcMethod(mth)));
		return Collections.unmodifiableList(bcMethods);
	}

	@Override
	public String getInternalName() {
		assert asmNode.name != null;
		return asmNode.name;
	}

	@Override
	public int getAccess() {
		return asmNode.access;
	}

	@Override
	public String getOuterClass() {
		return asmNode.outerClass;
	}

	@Override
	public String getOuterMethod() {
		return asmNode.outerMethod;
	}

	@Override
	public List<String> getInnerClasses() {
		List<String> innerClassNames = new ArrayList<>(asmNode.innerClasses.size());

		for (InnerClassNode innerClass : asmNode.innerClasses) {
			innerClassNames.add(innerClass.name);
		}

		return innerClassNames;
	}

	@Override
	public List<String> getInterfaces() {
		return asmNode.interfaces;
	}

	@Override
	public String getSuperName() {
		return asmNode.superName;
	}

	@Override
	public String getSignature() {
		return asmNode.signature;
	}

	public ModuleNode getModule() {
		return asmNode.module;
	}

	@Override
	public int getVersion() {
		return asmNode.version;
	}

	@Override
	public String getSourceFile() {
		return asmNode.sourceFile;
	}

	public String getSourceDebug() {
		return asmNode.sourceDebug;
	}

	@Override
	public String getOuterMethodDesc() {
		return asmNode.outerMethodDesc;
	}

	public String getNestHostClass() {
		return asmNode.nestHostClass;
	}

	@Override
	public List<JvmBcAnnotation> getAnnotations() {
		List<AnnotationNode> visibleAnnotations = asmNode.visibleAnnotations;
		List<AnnotationNode> invisibleAnnotations = asmNode.invisibleAnnotations;
		List<TypeAnnotationNode> visibleTypeAnnotations = asmNode.visibleTypeAnnotations;
		List<TypeAnnotationNode> invisibleTypeAnnotations = asmNode.invisibleTypeAnnotations;

		if (visibleAnnotations == null && invisibleAnnotations == null
				&& visibleTypeAnnotations == null && invisibleTypeAnnotations == null) {
			return Collections.emptyList();
		}

		List<JvmBcAnnotation> bcAnnotations = new ArrayList<>();
		if (visibleAnnotations != null) visibleAnnotations.forEach(ann -> bcAnnotations.add(new JvmBcAnnotation(ann, true)));
		if (invisibleAnnotations != null) invisibleAnnotations.forEach(ann -> bcAnnotations.add(new JvmBcAnnotation(ann, false)));
		if (visibleTypeAnnotations != null) visibleAnnotations.forEach(ann -> bcAnnotations.add(new JvmBcAnnotation(ann, true)));
		if (invisibleTypeAnnotations != null) invisibleAnnotations.forEach(ann -> bcAnnotations.add(new JvmBcAnnotation(ann, false)));
		return bcAnnotations;
	}

	@NotNull
	public List<Attribute> getAttributes() {
		if (asmNode.attrs == null) {
			return Collections.emptyList();
		}

		return asmNode.attrs;
	}

	public byte[] serialize() {
		ClassWriter writer = new ClassWriter(0);
		asmNode.accept(writer);
		return writer.toByteArray();
	}

	@Override
	public JvmBcClass getCopy() {
		ClassNode copy = new ClassNode();
		asmNode.accept(copy);
		return new JvmBcClass(copy);
	}

	@Override
	public JvmBcClass getRemappedCopy(BcClassRemapNameProvider renameProvider) {
		return JvmBcClassRemapper.process(this, renameProvider);
	}

	ClassNode getAsmNode() {
		return asmNode;
	}

	private final ClassNode asmNode;
}
