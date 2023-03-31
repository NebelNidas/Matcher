package matcher.bcprovider.jvm;

import java.util.List;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import matcher.bcprovider.BytecodeMethod;

public class JvmBcMethod implements BytecodeMethod {
	public JvmBcMethod(MethodNode asmNode) {
		this.asmNode = asmNode;
	}

	public JvmBcMethod() {
		this.asmNode = new MethodNode();
	}

	public JvmBcMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		this.asmNode = new MethodNode(access, name, descriptor, signature, exceptions);
	}

	@Override
	public int getAccess() {
		return asmNode.access;
	}

	@Override
	public String getSignature() {
		return asmNode.signature;
	}

	@Override
	public List<LocalVariableNode> getLocalVariables() {
		return asmNode.localVariables;
	}

	@Override
	public InsnList getInstructions() {
		return asmNode.instructions;
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
	public int getMaxStack() {
		return asmNode.maxStack;
	}

	@Override
	public int getMaxLocals() {
		return asmNode.maxLocals;
	}

	@Override
	public List<TryCatchBlockNode> getTryCatchBlocks() {
		return asmNode.tryCatchBlocks;
	}

	@Deprecated
	public MethodNode getAsmNode() {
		return asmNode;
	}

	MethodNode asmNode;
}
