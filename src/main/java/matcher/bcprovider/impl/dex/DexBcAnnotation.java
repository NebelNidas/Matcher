package matcher.bcprovider.impl.dex;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Range;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.AnnotationElement;

import matcher.bcprovider.BcAnnotation;
import matcher.bcprovider.BcAnnotationElement;

public class DexBcAnnotation implements BcAnnotation {
	DexBcAnnotation(Annotation dexNode) {
		this.dexNode = dexNode;
	}

	@Override
	@Range(from = 0, to = 2)
	public int getVisibility() {
		return dexNode.getVisibility();
	}

	@Override
	public String getDescriptor() {
		assert dexNode.getType() != null;
		return dexNode.getType();
	}

	@Override
	public boolean isTypeAnnotation() {
		// Dalvik bytecode doesn't have type annotations
		return false;
	}

	@Override
	public List<BcAnnotationElement> getElements() {
		List<BcAnnotationElement> elements = new ArrayList<>();

		for (AnnotationElement element : dexNode.getElements()) {
			elements.add(new DexBcAnnotationElement(element.getName(), element.getValue()));
		}

		return elements;
	}

	private final Annotation dexNode;
}
