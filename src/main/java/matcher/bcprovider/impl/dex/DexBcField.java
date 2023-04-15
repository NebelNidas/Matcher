package matcher.bcprovider.impl.dex;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import com.android.tools.smali.dexlib2.iface.Field;

import matcher.bcprovider.BcField;

public class DexBcField implements BcField {
	public DexBcField(Field dexNode) {
		this.dexNode = dexNode;
	}

	@Override
	public String getName() {
		assert dexNode.getName() != null;
		return dexNode.getName();
	}

	@Override
	public String getDescriptor() {
		assert dexNode.getType() != null;
		return dexNode.getType();
	}

	@Override
	public String getSignature() {
		return DexBytecodeHelper.extractSignature(dexNode.getAnnotations());
	}

	@Override
	public int getValue() {
		return dexNode.getInitialValue();
	}

	@Override
	public int getAccess() {
		return dexNode.getAccessFlags();
	}

	@Deprecated
	public Field getDexNode() {
		return dexNode;
	}

	@Override
	public @NotNull List<DexBcAnnotation> getAnnotations() {
		List<DexBcAnnotation> bcAnnotations = new ArrayList<>();
		dexNode.getAnnotations().forEach(ann -> bcAnnotations.add(new DexBcAnnotation(ann)));
		return bcAnnotations;
	}

	private final Field dexNode;
}
