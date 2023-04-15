package matcher.bcprovider.impl.dex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.android.tools.smali.dexlib2.iface.MethodParameter;

import matcher.bcprovider.BcAnnotation;
import matcher.bcprovider.BcParameter;

public class DexBcParameter implements BcParameter {
	DexBcParameter(MethodParameter dexNode, int access) {
		this.dexNode = dexNode;
		this.access = access;
	}

	@Override
	public String getName() {
		return dexNode.getName();
	}

	@Override
	public int getAccess() {
		return access;
	}

	@Override
	public List<BcAnnotation> getAnnotations() {
		if (annotations == null) {
			annotations = new ArrayList<>(dexNode.getAnnotations().size());
			dexNode.getAnnotations().forEach(ann -> annotations.add(new DexBcAnnotation(ann)));
		}

		return Collections.unmodifiableList(annotations);
	}

	private final MethodParameter dexNode;
	private final int access;
	private List<BcAnnotation> annotations;
}
