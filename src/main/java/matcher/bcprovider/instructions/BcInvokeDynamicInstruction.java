package matcher.bcprovider.instructions;

import java.util.List;

import org.objectweb.asm.Handle;

import matcher.bcprovider.BcInstruction;

public interface BcInvokeDynamicInstruction extends BcInstruction {
	String getName();

	String getDescriptor();

	Handle getBootstrapMethodHandle();

	List<Object> getBootstrapMethodArgs();
}
