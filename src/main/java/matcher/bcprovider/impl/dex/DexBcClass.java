package matcher.bcprovider.impl.dex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.android.tools.smali.dexlib2.ValueType;
import com.android.tools.smali.dexlib2.VersionMap;
import com.android.tools.smali.dexlib2.iface.Annotation;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.android.tools.smali.dexlib2.iface.value.ArrayEncodedValue;
import com.android.tools.smali.dexlib2.iface.value.EncodedValue;
import com.android.tools.smali.dexlib2.iface.value.MethodEncodedValue;
import com.android.tools.smali.dexlib2.iface.value.TypeEncodedValue;

import matcher.bcprovider.BcAnnotation;
import matcher.bcprovider.BcClass;
import matcher.bcprovider.BcClassRemapNameProvider;
import matcher.bcprovider.BcField;
import matcher.bcprovider.BcMethod;

public class DexBcClass implements BcClass {
	public DexBcClass(DexFile dexFile, ClassDef dexNode) {
		this(dexFile, dexNode, null, null);
	}

	DexBcClass(
			@NotNull DexFile dexFile,
			@NotNull ClassDef dexNode,
			@Nullable List<BcField> fields,
			@Nullable List<BcMethod> methods) {
		this.dexFile = dexFile;
		this.dexNode = dexNode;

		if (fields != null) this.fields = fields;
		if (methods != null) this.methods = methods;
	}

	@Override
	public List<BcField> getFields() {
		List<BcField> bcFields = new ArrayList<>();
		dexNode.getStaticFields().forEach(fld -> bcFields.add(new DexBcField(fld)));
		dexNode.getInstanceFields().forEach(fld -> bcFields.add(new DexBcField(fld)));
		return Collections.unmodifiableList(bcFields);
	}

	@Override
	public List<BcMethod> getMethods() {
		List<BcMethod> bcMethods = new ArrayList<>();
		dexNode.getDirectMethods().forEach(mth -> bcMethods.add(new DexBcMethod(mth, true)));
		dexNode.getVirtualMethods().forEach(mth -> bcMethods.add(new DexBcMethod(mth, false)));
		return Collections.unmodifiableList(bcMethods);
	}

	@Override
	public String getInternalName() {
		assert dexNode.getType() != null;
		return dexNode.getType();
	}

	@Override
	public int getAccess() {
		return dexNode.getAccessFlags();
	}

	@Override
	public String getOuterClass() {
		Annotation annotation;

		annotation = DexBytecodeHelper.findAnnotation(dexNode.getAnnotations(), DexBytecodeConstants.AnnotationTypes.ENCLOSING_CLASS);

		if (annotation == null) {
			return null;
		}

		TypeEncodedValue value = (TypeEncodedValue) DexBytecodeHelper.findAnnotationElementValue(annotation, ValueType.TYPE);

		if (value == null) {
			return null;
		}

		return value.getValue();
	}

	@Override
	public String getOuterMethod() {
		Annotation annotation;

		annotation = DexBytecodeHelper.findAnnotation(dexNode.getAnnotations(), DexBytecodeConstants.AnnotationTypes.ENCLOSING_METHOD);

		if (annotation == null) {
			return null;
		}

		MethodEncodedValue value = (MethodEncodedValue) DexBytecodeHelper.findAnnotationElementValue(annotation, ValueType.METHOD);

		if (value == null) {
			return null;
		}

		return value.getValue().getName();
	}

	@Override
	public List<String> getInnerClasses() {
		Annotation annotation;

		annotation = DexBytecodeHelper.findAnnotation(dexNode.getAnnotations(), DexBytecodeConstants.AnnotationTypes.MEMBER_CLASSES);

		if (annotation == null) {
			return null;
		}

		ArrayEncodedValue values = (ArrayEncodedValue) DexBytecodeHelper.findAnnotationElementValue(annotation, ValueType.ARRAY);

		if (values == null) {
			return null;
		}

		List<String> innerClasses = new ArrayList<>();

		for (EncodedValue value : values.getValue()) {
			assert value.getValueType() == ValueType.TYPE;

			innerClasses.add(((TypeEncodedValue) value).getValue());
		}

		return innerClasses;
	}

	@Override
	public List<String> getInterfaces() {
		return dexNode.getInterfaces();
	}

	@Override
	public String getSuperName() {
		return dexNode.getSuperclass();
	}

	@Override
	public String getSignature() {
		return dexNode.getType();
	}

	@Deprecated
	public ClassDef getDexNode() {
		return dexNode;
	}

	@Override
	public DexBcClass getCopy() {
		return new DexBcClass(dexFile, dexNode);
	}

	@Override
	public DexBcClass getRemappedCopy(BcClassRemapNameProvider renameProvider) {
		return DexBcClassRemapper.process(this, renameProvider);
	}

	@Override
	public int getVersion() {
		return VersionMap.mapApiToDexVersion(dexFile.getOpcodes().api);
	}

	/**
	 * The name of the source file from which this class was compiled.
	 */
	@Override
	public String getSourceFile() {
		return dexNode.getSourceFile();
	}

	@Override
	public String getOuterMethodDesc() {
		Annotation annotation;

		annotation = DexBytecodeHelper.findAnnotation(dexNode.getAnnotations(), DexBytecodeConstants.AnnotationTypes.ENCLOSING_METHOD);

		if (annotation == null) {
			return null;
		}

		MethodEncodedValue value = (MethodEncodedValue) DexBytecodeHelper.findAnnotationElementValue(annotation, ValueType.METHOD);

		if (value == null) {
			return null;
		}

		return value.getValue().getReturnType();
	}

	@Override
	public List<BcAnnotation> getAnnotations() {
		List<BcAnnotation> bcAnnotations = new ArrayList<>();
		dexNode.getAnnotations().forEach(ann -> bcAnnotations.add(new DexBcAnnotation(ann)));
		return bcAnnotations;
	}

	private final DexFile dexFile;
	private final ClassDef dexNode;
	private List<BcField> fields;
	private List<BcMethod> methods;
}
