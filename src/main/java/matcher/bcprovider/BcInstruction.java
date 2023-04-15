package matcher.bcprovider;

import com.strobel.annotations.Nullable;

public interface BcInstruction {
	@Nullable // at least for now
	BcOpcode getOpcode();

	int getInstructionType();
}
