package matcher.bcprovider.impl.jvm;

import java.util.Collections;
import java.util.List;

import org.objectweb.asm.tree.ParameterNode;

import matcher.bcprovider.BcParameter;

public class JvmBcParameter implements BcParameter {
	JvmBcParameter(String name, int access, List<JvmBcAnnotation> annotations) {
		this(new ParameterNode(name, access), annotations);
	}

	JvmBcParameter(ParameterNode asmNode, List<JvmBcAnnotation> annotations) {
		this.asmNode = asmNode;
		this.annotations = annotations;
	}

	@Override
	public String getName() {
		return asmNode.name;
	}

	@Override
	public int getAccess() {
		return asmNode.access;
	}

	@Override
	public List<JvmBcAnnotation> getAnnotations() {
		return Collections.unmodifiableList(annotations);
	}

	private final ParameterNode asmNode;
	private final List<JvmBcAnnotation> annotations;
}
