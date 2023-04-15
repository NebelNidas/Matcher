package matcher.bcprovider.impl.dex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import com.android.tools.smali.dexlib2.ValueType;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.MethodImplementation;
import com.android.tools.smali.dexlib2.iface.MethodParameter;
import com.android.tools.smali.dexlib2.iface.instruction.Instruction;
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue;
import com.android.tools.smali.dexlib2.iface.value.EncodedValue;
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue;

import matcher.bcprovider.BcAnnotation;
import matcher.bcprovider.BcInstruction;
import matcher.bcprovider.BcMethod;
import matcher.bcprovider.BcParameter;

public class DexBcMethod implements BcMethod {
	public DexBcMethod(Method dexNode, boolean direct) {
		this.dexNode = dexNode;
		this.dexNodeImpl = dexNode.getImplementation();
	}

	@Override
	public int getAccess() {
		return dexNode.getAccessFlags();
	}

	@Override
	public String getSignature() {
		return DexBytecodeHelper.extractSignature(dexNode.getAnnotations());
	}

	@Override
	public List<BcInstruction> getInstructions() {
		if (dexNodeImpl == null) {
			return Collections.emptyList();
		}

		List<BcInstruction> bcInstructions = new ArrayList<>();
		int i = 0;

		for (Instruction dexInsn : dexNodeImpl.getInstructions()) {
			if (dexInsn instanceof IntInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof VarInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof TypeInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof FieldInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof MethodInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof InvokeDynamicInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof JumpInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof LabelNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof LdcInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof IincInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof TableSwitchInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof LookupSwitchInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof MultiANewArrayInsnNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof FrameNode) {
				bcInstructions.add(null);
			} else if (dexInsn instanceof LineNumberNode) {
				bcInstructions.add(null);
			}
		}

		return bcInstructions;
	}

	@Override
	public String getName() {
		return dexNode.getName();
	}

	@Override
	public String getDescriptor() {
		return dexNode.getReturnType();
	}

	public int getRegisterCount() {
		if (dexNodeImpl == null) {
			return 0;
		}

		return dexNodeImpl.getRegisterCount();
	}

	@Override
	public List<TryCatchBlockNode> getTryCatchBlocks() {
		if (dexNodeImpl == null) {
			return Collections.emptyList();
		}

		return dexNodeImpl.getTryBlocks();
	}

	@Override
	public List<String> getExceptions() {
		Annotation annotation = null;

		annotation = DexBytecodeHelper.findAnnotation(dexNode.getAnnotations(), DexBytecodeConstants.AnnotationTypes.THROWS);

		if (annotation == null) {
			return null;
		}

		ArrayEncodedValue values = (ArrayEncodedValue) DexBytecodeHelper.findAnnotationElementValue(annotation, ValueType.ARRAY);

		if (values == null) {
			return null;
		}

		List<String> exceptionTypes = new ArrayList<>();

		for (EncodedValue value : values.getValue()) {
			assert value.getValueType() == ValueType.TYPE;

			exceptionTypes.add(((TypeEncodedValue) value).getValue());
		}

		return exceptionTypes;
	}

	@Deprecated
	public Method getDexNode() {
		return dexNode;
	}

	@Override
	public List<BcAnnotation> getAnnotations() {
		List<BcAnnotation> bcAnnotations = new ArrayList<>();
		dexNode.getAnnotations().forEach(ann -> bcAnnotations.add(new DexBcAnnotation(ann)));
		return bcAnnotations;
	}

	@Override
	public List<BcParameter> getParameters() {
		if (dexNode.getParameters().isEmpty()) {
			return Collections.emptyList();
		}

		List<BcParameter> bcParameters = new ArrayList<>(dexNode.getParameters().size());

		for (int i = 0; i < dexNode.getParameters().size(); i++) {
			MethodParameter param = dexNode.getParameters().get(i);

			bcParameters.add(new DexBcParameter(param, DexBytecodeHelper.extractParamAccess(dexNode.getAnnotations(), i)));
		}

		return Collections.unmodifiableList(bcParameters);
	}

	Method dexNode;
	MethodImplementation dexNodeImpl;
}
