package matcher.bcprovider.impl.jvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Range;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import matcher.bcprovider.BcAnnotation;

public class JvmBcAnnotation implements BcAnnotation {
	JvmBcAnnotation(String desc, boolean visible) {
		this(new AnnotationNode(desc), visible);
	}

	JvmBcAnnotation(AnnotationNode asmNode, boolean visible) {
		this.asmNode = asmNode;
		this.visible = visible;
		this.typeAnnotation = asmNode instanceof TypeAnnotationNode;
	}

	@Override
	@Range(from = 0, to = 1)
	public int getVisibility() {
		return visible ? 1 : 0;
	}

	@Override
	public String getDescriptor() {
		assert asmNode.desc != null;
		return asmNode.desc;
	}

	@Override
	public boolean isTypeAnnotation() {
		return typeAnnotation;
	}

	public int getTypeRef() {
		assert typeAnnotation;
		return ((TypeAnnotationNode) asmNode).typeRef;
	}

	public TypePath getTypePath() {
		assert typeAnnotation;
		return ((TypeAnnotationNode) asmNode).typePath;
	}

	@Override
	public List<JvmBcAnnotationElement> getElements() {
		if (asmNode.values == null) {
			return Collections.emptyList();
		}

		List<JvmBcAnnotationElement> elements = new ArrayList<>((int) Math.ceil(asmNode.values.size() / 2));

		for (int i = 0, n = asmNode.values.size(); i < n; i += 2) {
			String name = (String) asmNode.values.get(i);
			Object value = asmNode.values.get(i + 1);
			elements.add(new JvmBcAnnotationElement(name, value));
		}

		return elements;
	}

	private final AnnotationNode asmNode;
	private final boolean visible;
	private final boolean typeAnnotation;
}
