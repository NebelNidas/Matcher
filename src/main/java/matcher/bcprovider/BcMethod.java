package matcher.bcprovider;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.TryCatchBlockNode;

public interface BcMethod extends Annotatable {
	int getAccess();

	@Nullable
	String getSignature();

	@NotNull
	List<? extends BcParameter> getParameters();

	@NotNull
	List<? extends BcInstruction> getInstructions();

	@NotNull
	String getName();

	@NotNull
	String getDescriptor();

	@NotNull
	List<TryCatchBlockNode> getTryCatchBlocks();

	@NotNull
	List<String> getExceptions();
}
