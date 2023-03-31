package matcher.bcprovider;

public interface BytecodeClassRemapNameProvider extends BytecodeClassVisitor {
	default String remapPackageName(String currentName) {
		return currentName;
	}

	default String remapAnnotationAttributeName(String currentDesc, String name) {
		return currentDesc;
	}

	default String remapOwnName(String currentName) {
		return currentName;
	}

	default String remapOwnDesc(String currentDesc) {
		return currentDesc;
	}

	default String remapType(String currentInternalName) {
		return currentInternalName;
	}

	default String[] remapTypes(String[] currentInternalNames) {
		return currentInternalNames;
	}

	default String remapInnerClassName(String currentName, String ownerName, String innerName) {
		return currentName;
	}

	default String remapSignature(String currentSignature, boolean typeSignature) {
		return currentSignature;
	}

	default String remapFieldName(String owner, String currentName, String desc) {
		return currentName;
	}

	default Object remapValue(Object currentValue) {
		return currentValue;
	}

	default String remapMethodDesc(String currentMethodDesc) {
		return currentMethodDesc;
	}

	default String remapMethodName(String owner, String currentName, String desc) {
		return currentName;
	}

	default String remapMethodName(String owner, String currentName, String desc, boolean itf) {
		return currentName;
	}

	default String remapInvokeDynamicMethodName(String currentName, String desc) {
		return currentName;
	}

	default String remapArbitraryInvokeDynamicMethodName(String currentName, String desc) {
		return currentName;
	}

	default String remapRecordComponentName(String owner, String currentName, String desc) {
		return currentName;
	}

	default String remapMethodParameterName(String className, String methodName, String methodDesc, String currentName, int lvIndex) {
		return currentName;
	}

	default String remapMethodLocalVariableName(String className, String methodName, String methodDesc, String currentName,
			String desc, int lvIndex, int startInsn, int endInsn) {
		return currentName;
	}
}
