package matcher.bcprovider.impl.jvm;

import org.objectweb.asm.commons.Remapper;

import matcher.bcprovider.BcClassRemapNameProvider;

class JvmBcClassRemapNameProviderConverter {
	static final Remapper asAsmRemapper(BcClassRemapNameProvider renameProvider) {
		return new Remapper() {
			@Override
			public String mapPackageName(String name) {
				return renameProvider.remapPackageName(name);
			}

			@Override
			public String mapAnnotationAttributeName(String descriptor, String name) {
				return renameProvider.remapAnnotationAttributeName(descriptor, name);
			}

			@Override
			public String map(String internalName) {
				return renameProvider.remapOwnName(internalName);
			}

			@Override
			public String mapDesc(String descriptor) {
				return renameProvider.remapOwnDesc(descriptor);
			}

			@Override
			public String mapType(String internalName) {
				return renameProvider.remapType(internalName);
			}

			@Override
			public String[] mapTypes(String[] internalNames) {
				return renameProvider.remapTypes(internalNames);
			}

			@Override
			public String mapInnerClassName(String name, String ownerName, String innerName) {
				return renameProvider.remapInnerClassName(name, ownerName, innerName);
			}

			@Override
			public String mapSignature(String signature, boolean typeSignature) {
				return renameProvider.remapSignature(signature, typeSignature);
			}

			@Override
			public String mapFieldName(String owner, String name, String descriptor) {
				return renameProvider.remapFieldName(owner, name, descriptor);
			}

			@Override
			public Object mapValue(Object value) {
				return renameProvider.remapValue(value);
			}

			@Override
			public String mapMethodDesc(String methodDescriptor) {
				return renameProvider.remapMethodDesc(methodDescriptor);
			}

			@Override
			public String mapMethodName(String owner, String name, String descriptor) {
				return renameProvider.remapMethodName(owner, name, descriptor);
			}

			@Override
			public String mapInvokeDynamicMethodName(String name, String descriptor) {
				return renameProvider.remapInvokeDynamicMethodName(name, descriptor);
			}

			@Override
			public String mapRecordComponentName(String owner, String name, String descriptor) {
				return renameProvider.remapRecordComponentName(owner, name, descriptor);
			}
		};
	}
}
