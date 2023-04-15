package matcher.bcprovider.impl.jvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.ParameterNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import matcher.bcprovider.BcMethod;
import matcher.bcprovider.impl.jvm.instructions.JvmBcInstruction;

public class JvmBcMethod implements BcMethod {
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

	public List<LocalVariableNode> getLocalVariables() {
		if (asmNode.localVariables == null) {
			return Collections.emptyList();
		}

		return asmNode.localVariables;
	}

	@Override
	public List<JvmBcInstruction> getInstructions() {
		if (asmNode.instructions == null) {
			return Collections.emptyList();
		}

		List<JvmBcInstruction> bcInstructions = new ArrayList<>(asmNode.instructions.size());

		for (int i = 0; i < asmNode.instructions.size(); i++) {
			AbstractInsnNode asmInstruction = asmNode.instructions.get(i);

			if (asmInstruction instanceof IntInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof VarInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof TypeInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof FieldInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof MethodInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof InvokeDynamicInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof JumpInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof LabelNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof LdcInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof IincInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof TableSwitchInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof LookupSwitchInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof MultiANewArrayInsnNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof FrameNode) {
				bcInstructions.add(null);
			} else if (asmInstruction instanceof LineNumberNode) {
				bcInstructions.add(null);
			}
		}

		return bcInstructions;
	}

	@Override
	public String getName() {
		return asmNode.name;
	}

	@Override
	public String getDescriptor() {
		return asmNode.desc;
	}

	public int getMaxStack() {
		return asmNode.maxStack;
	}

	public int getMaxLocals() {
		return asmNode.maxLocals;
	}

	@Override
	public List<TryCatchBlockNode> getTryCatchBlocks() {
		if (asmNode.tryCatchBlocks == null) {
			return Collections.emptyList();
		}

		return asmNode.tryCatchBlocks;
	}

	@Override
	public List<String> getExceptions() {
		if (asmNode.exceptions == null) {
			return Collections.emptyList();
		}

		return asmNode.exceptions;
	}

	@Deprecated
	public MethodNode getAsmNode() {
		return asmNode;
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

	@Override
	public List<JvmBcParameter> getParameters() {
		if (asmNode.parameters == null) {
			return Collections.emptyList();
		}

		List<JvmBcParameter> bcParameters = new ArrayList<>(asmNode.parameters.size());

		for (int i = 0; i < asmNode.parameters.size(); i++) {
			ParameterNode param = asmNode.parameters.get(i);
			List<AnnotationNode> currentAnns = asmNode.visibleParameterAnnotations[i];
			List<JvmBcAnnotation> bcAnns = new ArrayList<>();

			if (currentAnns != null) {
				currentAnns.forEach(ann -> bcAnns.add(new JvmBcAnnotation(ann, true)));
			}

			currentAnns = asmNode.invisibleParameterAnnotations[i];

			if (currentAnns != null) {
				currentAnns.forEach(ann -> bcAnns.add(new JvmBcAnnotation(ann, false)));
			}

			bcParameters.add(new JvmBcParameter(param, bcAnns));
		}

		return Collections.unmodifiableList(bcParameters);
	}

	public List<Attribute> getNonStandardAttributes() {
		if (asmNode.attrs == null) {
			return Collections.emptyList();
		}

		return asmNode.attrs;
	}

	MethodNode asmNode;
}
