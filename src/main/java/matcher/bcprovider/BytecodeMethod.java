package matcher.bcprovider;

import java.util.List;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public interface BytecodeMethod {
	int getAccess();

	String getSignature();

	List<LocalVariableNode> getLocalVariables();

	InsnList getInstructions();

	String getName();

	String getDesc();

	int getMaxStack();

	int getMaxLocals();

	List<TryCatchBlockNode> getTryCatchBlocks();
}
