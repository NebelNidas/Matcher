package matcher.bcprovider.impl.dex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import com.android.tools.smali.dexlib2.ValueType;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.AnnotationElement;
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue;
import com.android.tools.smali.dexlib2.iface.value.EncodedValue;
import com.android.tools.smali.dexlib2.iface.value.IntEncodedValue;
import com.android.tools.smali.dexlib2.iface.value.StringEncodedValue;
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue;

public class DexBytecodeHelper {
	@Nullable
	static String extractSignature(Set<? extends Annotation> annotations) {
		Annotation annotation = null;

		annotation = findAnnotation(annotations, DexBytecodeConstants.AnnotationTypes.SIGNATURE);

		if (annotation == null) {
			return null;
		}

		ArrayEncodedValue values = (ArrayEncodedValue) findAnnotationElementValue(annotation, ValueType.ARRAY);

		if (values == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();

		for (EncodedValue value : values.getValue()) {
			assert value.getValueType() == ValueType.STRING;

			sb.append(((StringEncodedValue) value).getValue());
		}

		return sb.toString();
	}

	@Nullable
	static List<String> extractExceptions(Set<? extends Annotation> annotations) {
		Annotation annotation = null;

		annotation = findAnnotation(annotations, DexBytecodeConstants.AnnotationTypes.THROWS);

		if (annotation == null) {
			return null;
		}

		ArrayEncodedValue values = (ArrayEncodedValue) findAnnotationElementValue(annotation, ValueType.ARRAY);

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

	@Nullable
	public static Integer extractParamAccess(Set<? extends Annotation> annotations, int paramIndex) {
		Annotation annotation = null;

		annotation = findAnnotation(annotations, DexBytecodeConstants.AnnotationTypes.METHOD_PARAMETERS);

		if (annotation == null) {
			return null;
		}

		ArrayEncodedValue values = (ArrayEncodedValue) findAnnotationElementValue(annotation, ValueType.ARRAY);

		if (values == null) {
			return null;
		}

		EncodedValue value = values.getValue().get(paramIndex);
		assert value.getValueType() == ValueType.INT;

		return ((IntEncodedValue) value).getValue();
	}

	@Nullable
	static Annotation findAnnotation(Set<? extends Annotation> annotations, String descriptor) {
		for (Annotation annotation: annotations) {
			if (annotation.getType().equals(descriptor)) {
				return annotation;
			}
		}

		return null;
	}

	@Nullable
	static EncodedValue findAnnotationElementValue(Annotation annotation, @Nullable Integer valueType) {
		for (AnnotationElement annotationElement : annotation.getElements()) {
			if (annotationElement.getName().equals("value")) {
				EncodedValue encodedValue = annotationElement.getValue();

				if (valueType != null && encodedValue.getValueType() != valueType) {
					return null;
				}

				return encodedValue;
			}
		}

		return null;
	}
}
