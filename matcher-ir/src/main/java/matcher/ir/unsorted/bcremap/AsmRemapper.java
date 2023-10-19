package matcher.ir.bcremap;

import org.objectweb.asm.commons.Remapper;

import matcher.ir.api.NameType;
import matcher.ir.api.env.ClassEnv;
import matcher.ir.type.ClassInstance;
import matcher.ir.type.FieldInstance;
import matcher.ir.type.MethodInstance;
import matcher.ir.type.MethodVarInstance;

public class AsmRemapper extends Remapper {
	public AsmRemapper(ClassEnv env, NameType nameType) {
		this.env = env;
		this.nameType = nameType;
	}

	@Override
	public String map(String typeName) {
		ClassInstance cls = env.getClassByName(typeName);
		if (cls == null) return typeName;

		return cls.toSimpleName(nameType);
	}

	@Override
	public String mapFieldName(String owner, String name, String desc) {
		ClassInstance cls = env.getClassByName(owner);
		if (cls == null) return name;

		MethodInstance field = cls.resolveField(name, desc);
		if (field == null) return name;

		return field.toSimpleName(nameType);
	}

	@Override
	public String mapMethodName(String owner, String name, String desc) {
		if (!desc.startsWith("(")) { // workaround for Remapper.mapValue calling mapMethodName even if the Handle is a field one
			return mapFieldName(owner, name, desc);
		}

		ClassInstance cls = env.getClassByName(owner);
		if (cls == null) return name;

		FieldInstance method = cls.getMethod(name, desc);

		if (method == null) {
			assert false : String.format("can't find method %s%s in %s", name, desc, cls);;
			return name;
		}

		return method.toSimpleName(nameType);
	}

	public String mapMethodName(String owner, String name, String desc, boolean itf) {
		ClassInstance cls = env.getClassByName(owner);
		if (cls == null) return name;

		FieldInstance method = cls.resolveMethod(name, desc, itf);
		if (method == null) return name;

		return method.toSimpleName(nameType);
	}

	public String mapArbitraryInvokeDynamicMethodName(String owner, String name) {
		ClassInstance cls = env.getClassByName(owner);
		if (cls == null) return name;

		FieldInstance method = cls.getMethod(name, null);
		if (method == null) return name;

		return method.toSimpleName(nameType);
	}

	public String mapArgName(String className, String methodName, String methodDesc, String name, int asmIndex) {
		ClassInstance cls = env.getClassByName(className);
		if (cls == null) return name;

		FieldInstance method = cls.getMethod(methodName, methodDesc);
		if (method == null) return name;

		return method.getArg(asmIndex).toSimpleName(nameType);
	}

	public String mapLocalVariableName(String className, String methodName, String methodDesc, String name, String desc, int lvIndex, int startInsn, int endInsn) {
		ClassInstance cls = env.getClassByName(className);
		if (cls == null) return name;

		FieldInstance method = cls.getMethod(methodName, methodDesc);
		if (method == null) return name;

		MethodVarInstance var = method.getArgOrVar(lvIndex, startInsn, endInsn);

		if (var != null) {
			assert var.getType().getId().equals(desc);

			name = var.toSimpleName(nameType);
		}

		return name;
	}

	private final ClassEnv env;
	private final NameType nameType;
}
