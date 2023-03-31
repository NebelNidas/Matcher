package matcher.bcremap;

import matcher.NameType;
import matcher.bcprovider.BytecodeClassRemapNameProvider;
import matcher.type.ClassEnv;
import matcher.type.ClassInstance;
import matcher.type.FieldInstance;
import matcher.type.MethodInstance;
import matcher.type.MethodVarInstance;

public class ClassRemapNameProvider implements BytecodeClassRemapNameProvider {
	public ClassRemapNameProvider(ClassEnv env, NameType nameType) {
		this.env = env;
		this.nameType = nameType;
	}

	@Override
	public String remapOwnName(String typeName) {
		ClassInstance cls = env.getClsByName(typeName);
		if (cls == null) return typeName;

		return cls.getName(nameType);
	}

	@Override
	public String remapFieldName(String owner, String name, String desc) {
		ClassInstance cls = env.getClsByName(owner);
		if (cls == null) return name;

		FieldInstance field = cls.resolveField(name, desc);
		if (field == null) return name;

		return field.getName(nameType);
	}

	@Override
	public String remapMethodName(String owner, String name, String desc) {
		if (!desc.startsWith("(")) { // workaround for Remapper.mapValue calling mapMethodName even if the Handle is a field one
			return remapFieldName(owner, name, desc);
		}

		ClassInstance cls = env.getClsByName(owner);
		if (cls == null) return name;

		MethodInstance method = cls.getMethod(name, desc);

		if (method == null) {
			assert false : String.format("can't find method %s%s in %s", name, desc, cls);;
			return name;
		}

		return method.getName(nameType);
	}

	public String remapMethodName(String owner, String name, String desc, boolean itf) {
		ClassInstance cls = env.getClsByName(owner);
		if (cls == null) return name;

		MethodInstance method = cls.resolveMethod(name, desc, itf);
		if (method == null) return name;

		return method.getName(nameType);
	}

	public String remapArbitraryInvokeDynamicMethodName(String owner, String name) {
		ClassInstance cls = env.getClsByName(owner);
		if (cls == null) return name;

		MethodInstance method = cls.getMethod(name, null);
		if (method == null) return name;

		return method.getName(nameType);
	}

	@Override
	public String remapMethodParameterName(String className, String methodName, String methodDesc, String currentName, int lvIndex) {
		return remapParameterOrLocalVariable(className, methodName, methodDesc, currentName, methodDesc, true, lvIndex, -1, -1);
	}

	@Override
	public String remapMethodLocalVariableName(String className, String methodName, String methodDesc, String name, String desc, int lvIndex, int startInsn, int endInsn) {
		return remapParameterOrLocalVariable(className, methodName, methodDesc, name, desc, false, lvIndex, startInsn, endInsn);
	}

	private String remapParameterOrLocalVariable(String className, String methodName, String methodDesc, String name,
			String desc, boolean parameter, int lvIndex, int startInsn, int endInsn) {
		ClassInstance cls = env.getClsByName(className);
		if (cls == null) return name;

		MethodInstance method = cls.getMethod(methodName, methodDesc);
		if (method == null) return name;

		if (parameter) {
			return method.getArg(lvIndex).getName(nameType);
		} else {
			MethodVarInstance var = method.getArgOrVar(lvIndex, startInsn, endInsn);

			if (var != null) {
				assert var.getType().getId().equals(desc);

				name = var.getName(nameType);
			}

			return name;
		}
	}

	private final ClassEnv env;
	private final NameType nameType;
}
