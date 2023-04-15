package matcher.bcprovider.impl.jvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import matcher.bcprovider.BcField;

public class JvmBcField implements BcField {
	public JvmBcField(FieldNode asmNode) {
		this.asmNode = asmNode;
	}

	public JvmBcField(int access, String name, String descriptor, String signature, Object value) {
		this.asmNode = new FieldNode(access, name, descriptor, signature, value);
	}

	@Override
	public String getName() {
		assert asmNode.name != null;
		return asmNode.name;
	}

	@Override
	public String getDescriptor() {
		assert asmNode.desc != null;
		return asmNode.desc;
	}

	@Override
	public String getSignature() {
		return asmNode.signature;
	}

	@Override
	public Object getValue() {
		return asmNode.value;
	}

	@Override
	public int getAccess() {
		return asmNode.access;
	}

	@Deprecated
	public FieldNode getAsmNode() {
		return asmNode;
	}

	@Override
	public @NotNull List<JvmBcAnnotation> getAnnotations() {
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

	private final FieldNode asmNode;
}
