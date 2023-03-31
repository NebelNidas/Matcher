package matcher.bcprovider.jvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import matcher.bcprovider.BytecodeClass;
import matcher.bcprovider.BytecodeClassRemapNameProvider;
import matcher.bcprovider.BytecodeField;
import matcher.bcprovider.BytecodeMethod;

public class JvmBcClass implements BytecodeClass {
	public JvmBcClass() {
		this.asmNode = new ClassNode();
	}

	public JvmBcClass(ClassNode asmNode) {
		this.asmNode = asmNode;
	}

	@Override
	public List<BytecodeField> getFields() {
		if (fields.isEmpty()) {
			for (FieldNode field : asmNode.fields) {
				fields.add(new JvmBcField(field));
			}
		}

		return Collections.unmodifiableList(fields);
	}

	@Override
	public List<BytecodeMethod> getMethods() {
		if (methods.isEmpty()) {
			for (MethodNode method : asmNode.methods) {
				methods.add(new JvmBcMethod(method));
			}
		}

		return Collections.unmodifiableList(methods);
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
	public String getOuterClass() {
		return asmNode.outerClass;
	}

	@Override
	public String getOuterMethod() {
		return asmNode.outerMethod;
	}

	@Override
	public List<InnerClassNode> getInnerClasses() {
		return asmNode.innerClasses;
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

	@Deprecated
	public ClassNode getAsmNode() {
		return asmNode;
	}

	@Override
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
	public JvmBcClass getRemappedCopy(BytecodeClassRemapNameProvider renameProvider) {
		return JvmBcClassRemapper.process(this, renameProvider);
	}

	private final ClassNode asmNode;
	private List<JvmBcField> fields = new ArrayList<>();
	private List<JvmBcMethod> methods = new ArrayList<>();
}
