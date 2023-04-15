package matcher.bcprovider.instructions;

import matcher.bcprovider.BcInstruction;

public interface BcIncrementIntegerInstruction extends BcInstruction {
	int getIntegerIndex();

	int getIncrement();
}
