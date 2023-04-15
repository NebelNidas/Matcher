package matcher.bcprovider.impl.jvm.instructions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

import matcher.bcprovider.instructions.BcInvokeDynamicInstruction;

public class JvmBcInvokeDynamicInstruction extends JvmBcInstruction implements BcInvokeDynamicInstruction {
	JvmBcInvokeDynamicInstruction(InvokeDynamicInsnNode asmNode) {
		super(asmNode);
		this.asmNode = asmNode;
	}

	public String getName() {
		return asmNode.name;
	}

	public String getDescriptor() {
		return asmNode.desc;
	}

	public Handle getBootstrapMethodHandle() {
		return asmNode.bsm;
	}

	public List<Object> getBootstrapMethodArgs() {
		return Collections.unmodifiableList(Arrays.asList(asmNode.bsmArgs));
	}

	private InvokeDynamicInsnNode asmNode;
}
