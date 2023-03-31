package matcher.bcprovider;

import java.util.List;

import org.objectweb.asm.tree.InnerClassNode;

import matcher.bcprovider.jvm.JvmBcClass;

public interface BytecodeClass {
	String getName();
	List<BytecodeField> getFields();
	List<BytecodeMethod> getMethods();

	static BytecodeClass empty() {
		return new JvmBcClass();
	}

	int getAccess();
	String getOuterClass();
	String getOuterMethod();
	List<InnerClassNode> getInnerClasses();
	List<String> getInterfaces();
	String getSuperName();
	String getSignature();
	byte[] serialize();
	BytecodeClass getCopy();
	BytecodeClass getRemappedCopy(BytecodeClassRemapNameProvider renameProvider);
	void accept(BytecodeClassVisitor visitor);
}
