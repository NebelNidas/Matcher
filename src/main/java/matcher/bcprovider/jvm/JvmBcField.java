package matcher.bcprovider.jvm;

import org.objectweb.asm.tree.FieldNode;

import matcher.bcprovider.BytecodeField;

public class JvmBcField implements BytecodeField {
	public JvmBcField(FieldNode asmNode) {
		this.asmNode = asmNode;
	}

	public JvmBcField(int access, String name, String descriptor, String signature, Object value) {
		this.asmNode = new FieldNode(access, name, descriptor, signature, value);
	}

	@Override
	public String getName() {
		return asmNode.name;
	}

	@Override
	public String getDesc() {
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

	private final FieldNode asmNode;
}
