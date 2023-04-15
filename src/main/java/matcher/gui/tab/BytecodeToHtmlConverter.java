/*
 * Most of this file is copied from ASM's Textifier class,
 * tweaked to output HTML instead of plain text. Original license:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package matcher.gui.tab;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.TypeReference;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.tree.ModuleNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TextifierSupport;
import org.objectweb.asm.util.TraceSignatureVisitor;

import matcher.NameType;
import matcher.bcprovider.BcAnnotation;
import matcher.bcprovider.BcAnnotationElement;
import matcher.bcprovider.BcClass;
import matcher.bcprovider.BcField;
import matcher.bcprovider.BcInstruction;
import matcher.bcprovider.BcMethod;
import matcher.bcprovider.BcParameter;
import matcher.bcprovider.BcTypeAnnotation;
import matcher.bcprovider.impl.jvm.JvmBcClass;
import matcher.bcprovider.impl.jvm.JvmBcMethod;
import matcher.bcprovider.impl.jvm.JvmBytecodeHelper;
import matcher.srcprocess.HtmlUtil;
import matcher.type.ClassInstance;
import matcher.type.FieldInstance;
import matcher.type.MethodInstance;

final class BytecodeToHtmlConverter {
	BytecodeToHtmlConverter(ClassInstance cls, NameType nameType) {
		this.cls = cls;
		this.clsAccess = cls.getAccess();
		this.nameType = nameType;
	}

	public String convert() {
		BcClass bcClass = cls.getMergedBytecodeClass();

		visitClass(bcClass);

		if (bcClass.getSourceFile() != null || bcClass.getSourceDebug() != null) {
			visitSource(bcClass.getSourceFile(), bcClass.getSourceDebug());
		}

		if (bcClass instanceof JvmBcClass jvmBcClass && jvmBcClass.getModule() != null) {
			moduleVisitor.visitModule(jvmBcClass.getModule());
		}

		if (bcClass.getNestHostClass() != null) {
			visitNestHost(bcClass.getNestHostClass());
		}

		if (bcClass.getOuterClass() != null) {
			visitOuterClass(bcClass.getOuterClass(), bcClass.getOuterMethod(), bcClass.getOuterMethodDesc());
		}

		for (BcAnnotation bcAnnotation : bcClass.getAnnotations()) {
			visitAnnotation(bcAnnotation, bcAnnotation.getVisibility() > 0);
		}

		for (BcTypeAnnotation bcAnnotation : bcClass.getTypeAnnotations()) {
			visitTypeAnnotation(bcAnnotation, bcAnnotation.getVisibility() > 0);
		}

		if (bcClass instanceof JvmBcClass jvmBcClass) {
			for (Attribute attribute : jvmBcClass.getAttributes()) {
				visitAttribute(attribute);
			}
		}

		// TODO: NestMembers
		// TODO: PermittedSubclasses
		// TODO: InnerClasses
		// TODO: RecordComponents

		for (BcMethod bcMethod : bcClass.getMethods()) {
			visitMethod(bcMethod);
		}

		for (BcField bcField : bcClass.getFields()) {
			visitField(bcField);
		}

		visitClassEnd();
		return HtmlUtil.escape(stringBuilder.toString());
	}

	// -----------------------------------------------------------------------------------------------
	// Classes
	// -----------------------------------------------------------------------------------------------

	protected void visitClass(BcClass cls) {
		final int version = cls.getVersion();
		final int access = cls.getAccess();
		final String name = cls.getInternalName();
		final String signature = cls.getSignature();
		final String superName = cls.getSuperName();
		final List<String> interfaces = cls.getInterfaces();

		if ((access & Opcodes.ACC_MODULE) != 0) {
			// Modules are printed in visitModule.
			return;
		}

		this.clsAccess = access;
		boolean jvmBcCls = cls instanceof JvmBcClass;

		stringBuilder
				.append('\n')
				.append("<span class=\"comment\">")
				.append("// ");
		if (jvmBcCls) stringBuilder.append("class ");
		stringBuilder.append("version ");

		if (!jvmBcCls) {
			stringBuilder.append(version);
		} else {
			stringBuilder
					.append(version & 0xFFFF)
					.append('.')
					.append(version >>> 16)
					.append(" (")
					.append(version)
					.append(')');
		}

		stringBuilder.append("</span>");

		if ((access & Opcodes.ACC_DEPRECATED) != 0) {
			stringBuilder.append(DEPRECATED);
		}

		if ((access & Opcodes.ACC_RECORD) != 0) {
			stringBuilder.append(RECORD);
		}

		appendRawAccess(access);

		appendDescriptor(CLASS_SIGNATURE, signature);

		if (signature != null) {
			appendJavaDeclaration(name, signature);
		}

		appendAccess(access & ~(Opcodes.ACC_SUPER | Opcodes.ACC_MODULE));

		if ((access & Opcodes.ACC_ANNOTATION) != 0) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append("@interface")
					.append("</span> ");
		} else if ((access & Opcodes.ACC_INTERFACE) != 0) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append("interface")
					.append("</span> ");
		} else if ((access & Opcodes.ACC_ENUM) == 0) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append("class")
					.append("</span> ");
		}

		appendDescriptor(INTERNAL_NAME, name);

		if (superName != null && !"java/lang/Object".equals(superName)) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append(" extends")
					.append("</span> ");

			appendDescriptor(INTERNAL_NAME, superName);
		}

		if (interfaces != null && interfaces.size() > 0) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append(" implements")
					.append("</span> ");

			for (int i = 0; i < interfaces.size(); ++i) {
				appendDescriptor(INTERNAL_NAME, interfaces.get(i));

				if (i != interfaces.size() - 1) {
					stringBuilder.append(' ');
				}
			}
		}

		stringBuilder.append(" {\n\n");
	}

	protected void visitSource(String file, String debug) {
		if (file != null) {
			stringBuilder
					.append(tab)
					.append("<span class=\"comment\">")
					.append("// compiled from: ")
					.append(file)
					.append("</span>\n");
		}

		if (debug != null) {
			stringBuilder
					.append(tab)
					.append("<span class=\"comment\">")
					.append("// debug info: ")
					.append(debug)
					.append("</span>\n");
		}
	}

	protected void visitNestHost(String nestHost) {
		stringBuilder
				.append(tab)
				.append("<span class=\"keyword\">")
				.append("NESTHOST")
				.append("</span> ");
		appendDescriptor(INTERNAL_NAME, nestHost);
		stringBuilder.append('\n');
	}

	protected void visitOuterClass(String owner, String name, String descriptor) {
		stringBuilder
				.append(tab)
				.append("<span class=\"keyword\">")
				.append("OUTERCLASS")
				.append("</span> ");
		appendDescriptor(INTERNAL_NAME, owner);
		stringBuilder.append(' ');

		if (name != null) {
			stringBuilder
					.append("<span class=\"class-name\">")
					.append(name)
					.append("</span> ");
		}

		appendDescriptor(METHOD_DESCRIPTOR, descriptor);
		stringBuilder.append('\n');
	}

	protected void visitNestMember(String nestMember) {
		stringBuilder
				.append(tab)
				.append("<span class=\"keyword\">")
				.append("NESTMEMBER")
				.append("</span> ");
		appendDescriptor(INTERNAL_NAME, nestMember);
		stringBuilder.append('\n');
	}

	protected void visitPermittedSubclass(String permittedSubclass) {
		stringBuilder
				.append(tab)
				.append("<span class=\"keyword\">")
				.append("PERMITTEDSUBCLASS")
				.append("</span> ");
		appendDescriptor(INTERNAL_NAME, permittedSubclass);
		stringBuilder.append('\n');
	}

	protected void visitInnerClass(String name, String outerName, String innerName, int access) {
		stringBuilder.append(tab);
		appendRawAccess(access & ~Opcodes.ACC_SUPER);
		stringBuilder.append(tab);
		appendAccess(access);
		stringBuilder
				.append("<span class=\"keyword\">")
				.append("INNERCLASS")
				.append("</span> ");
		appendDescriptor(INTERNAL_NAME, name);
		stringBuilder.append(' ');
		appendDescriptor(INTERNAL_NAME, outerName);
		stringBuilder.append(' ');
		appendDescriptor(INTERNAL_NAME, innerName);
		stringBuilder.append('\n');
	}

	public void visitRecordComponent(String name, String descriptor, String signature) {
		stringBuilder
				.append(tab)
				.append("<span class=\"keyword\">")
				.append("RECORDCOMPONENT")
				.append("</span> ");
		if (signature != null) {
			stringBuilder.append(tab);
			appendDescriptor(FIELD_SIGNATURE, signature);
			stringBuilder.append(tab);
			appendJavaDeclaration(name, signature);
		}

		stringBuilder.append(tab);

		appendDescriptor(FIELD_DESCRIPTOR, descriptor);
		stringBuilder
				.append(" <span class=\"class-name\">")
				.append(name)
				.append("</span>\n");
	}

	public void visitField(BcField bcField) {
		int access = bcField.getAccess();
		String name = bcField.getName();
		String descriptor = bcField.getDescriptor();
		String signature = bcField.getSignature();
		Object value = bcField.getValue();
		FieldInstance fieldInstance = cls.getField(name, descriptor, nameType);

		if (fieldInstance != null) {
			stringBuilder.append(String.format("\n<div id=\"%s\">", HtmlUtil.getId(fieldInstance)));
		}

		stringBuilder.append('\n');

		if ((access & Opcodes.ACC_DEPRECATED) != 0) {
			stringBuilder
					.append(tab)
					.append(DEPRECATED);
		}

		stringBuilder.append(tab);
		appendRawAccess(access);

		if (signature != null) {
			stringBuilder.append(tab);
			appendDescriptor(FIELD_SIGNATURE, signature);
			stringBuilder.append(tab);
			appendJavaDeclaration(name, signature);
		}

		stringBuilder.append(tab);
		appendAccess(access);

		appendDescriptor(FIELD_DESCRIPTOR, descriptor);
		stringBuilder
				.append(" <span class=\"field\">")
				.append(name)
				.append("</span>");
		if (value != null) {
			stringBuilder.append(" = ");

			if (value instanceof String) {
				stringBuilder
						.append('\"')
						.append(value)
						.append('\"');
			} else {
				stringBuilder.append(value);
			}
		}

		stringBuilder.append('\n');
		if (fieldInstance != null) stringBuilder.append("</div>");

		for (BcAnnotation bcAnnotation : bcField.getAnnotations()) {
			visitAnnotation(bcAnnotation, bcAnnotation.getVisibility() > 0);
		}

		for (BcTypeAnnotation bcAnnotation : bcField.getTypeAnnotations()) {
			visitTypeAnnotation(bcAnnotation, bcAnnotation.getVisibility() > 0);
		}

		if (bcField instanceof JvmBcClass jvmBcField) {
			for (Attribute attribute : jvmBcField.getAttributes()) {
				visitAttribute(attribute);
			}
		}
	}

	public void visitMethod(BcMethod bcMethod) {
		int access = bcMethod.getAccess();
		String name = bcMethod.getName();
		String descriptor = bcMethod.getDescriptor();
		String signature = bcMethod.getSignature();
		List<String> exceptions = bcMethod.getExceptions();
		MethodInstance methodInstance = cls.getMethod(name, descriptor, nameType);

		if (methodInstance != null) {
			stringBuilder.append(String.format("<div id=\"%s\">", HtmlUtil.getId(methodInstance)));
		}

		stringBuilder.append('\n');

		if ((access & Opcodes.ACC_DEPRECATED) != 0) {
			stringBuilder
					.append(tab)
					.append(DEPRECATED);
		}

		stringBuilder.append(tab);
		appendRawAccess(access);

		if (signature != null) {
			stringBuilder.append(tab);
			appendDescriptor(METHOD_SIGNATURE, signature);
			stringBuilder.append(tab);
			appendJavaDeclaration(name, signature);
		}

		stringBuilder.append(tab);
		appendAccess(access & ~(Opcodes.ACC_VOLATILE | Opcodes.ACC_TRANSIENT));

		if ((access & Opcodes.ACC_NATIVE) != 0) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append("native")
					.append("</span> ");
		}

		if ((access & Opcodes.ACC_VARARGS) != 0) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append("varargs")
					.append("</span> ");
		}

		if ((access & Opcodes.ACC_BRIDGE) != 0) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append("bridge")
					.append("</span> ");
		}

		if ((this.clsAccess & Opcodes.ACC_INTERFACE) != 0
				&& (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC)) == 0) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append("default")
					.append("</span> ");
		}

		stringBuilder
				.append("<span class=\"method-name\">")
				.append(name)
				.append("</span>");
		appendDescriptor(METHOD_DESCRIPTOR, descriptor);

		if (exceptions != null && exceptions.size() > 0) {
			stringBuilder
					.append(" <span class=\"keyword\">")
					.append("throws")
					.append("</span> ");
			for (String exception : exceptions) {
				appendDescriptor(INTERNAL_NAME, exception);
				stringBuilder.append(' ');
			}
		}

		stringBuilder.append("</span>");

		stringBuilder.append('\n');
		if (methodInstance != null) stringBuilder.append("</div>");

		for (BcParameter bcParameter : bcMethod.getParameters()) {
			visitParameter(bcParameter);
		}

		for (BcAnnotation bcAnnotation : bcMethod.getAnnotations()) {
			visitAnnotation(bcAnnotation, bcAnnotation.getVisibility() > 0);
		}

		for (BcTypeAnnotation bcAnnotation : bcMethod.getTypeAnnotations()) {
			visitTypeAnnotation(bcAnnotation, bcAnnotation.getVisibility() > 0);
		}

		for (int i = 0; i < bcMethod.getParameters().size(); i++) {
			BcParameter bcParameter = bcMethod.getParameters().get(i);

			for (BcAnnotation bcAnnotation : bcParameter.getAnnotations()) {
				if (bcAnnotation == null) continue;
				visitParameterAnnotation(i, bcAnnotation);
			}
		}

		if (bcMethod instanceof JvmBcMethod jvmBcMethod) {
			for (Attribute attribute : jvmBcMethod.getNonStandardAttributes()) {
				visitAttribute(attribute);
			}
		}

		if (bcMethod.getInstructions().size() > 0) {
			for (TryCatchBlockNode tryCatchBlock : bcMethod.getTryCatchBlocks()) {
				tryCatchBlock.updateIndex(access);
			}

			for (BcInstruction instructions : bcMethod.getInstructions()) {
				// visit
			}
		}
	}

	protected void visitClassEnd() {
		stringBuilder.append("}\n");
	}

	// -----------------------------------------------------------------------------------------------
	// Modules
	// -----------------------------------------------------------------------------------------------

	private class HtmlModuleVisitor extends ModuleVisitor {
		protected HtmlModuleVisitor() {
			super(JvmBytecodeHelper.ASM_VERSION);
		}

		public void visitModule(ModuleNode module) {
			String name = module.name;
			String version = module.version;
			int access = module.access;

			if ((access & Opcodes.ACC_OPEN) != 0) {
				stringBuilder
						.append("<span class=\"keyword\">")
						.append("open")
						.append("</span> ");
			}

			stringBuilder
					.append("<span class=\"keyword\">")
					.append("module ")
					.append("</span>")
					.append("<span class=\"class-name\">")
					.append(name)
					.append("</span>")
					.append(" { ")
					.append(version == null ? "" : "<span class=\"comment\">// " + version)
					.append("</span>\n\n");
		}

		@Override
		public void visitMainClass(final String mainClass) {
			stringBuilder
					.append("  <span class=\"comment\">")
					.append("// main class ")
					.append(mainClass)
					.append("</span>\n");
		}

		@Override
		public void visitPackage(final String packaze) {
			stringBuilder
					.append("  <span class=\"comment\">")
					.append("// package ")
					.append(packaze)
					.append("</span>\n");
		}

		@Override
		public void visitRequire(final String require, final int access, final String version) {
			stringBuilder
					.append("<span class=\"keyword\">")
					.append(tab)
					.append("requires")
					.append("</span> ");
			if ((access & Opcodes.ACC_TRANSITIVE) != 0) {
				stringBuilder
						.append("<span class=\"keyword\">")
						.append("transitive")
						.append("</span> ");
			}

			if ((access & Opcodes.ACC_STATIC_PHASE) != 0) {
				stringBuilder
						.append("<span class=\"keyword\">")
						.append("static")
						.append("</span> ");
			}

			stringBuilder
					.append(require)
					.append(';');
			appendRawAccess(access);

			if (version != null) {
				stringBuilder
						.append("  <span class=\"comment\">")
						.append("// version ")
						.append(version)
						.append("</span>\n");
			}
		}

		@Override
		public void visitExport(final String packaze, final int access, final String... modules) {
			visitExportOrOpen("exports", packaze, access, modules);
		}

		@Override
		public void visitOpen(final String packaze, final int access, final String... modules) {
			visitExportOrOpen("opens", packaze, access, modules);
		}

		private void visitExportOrOpen(
				final String method, final String packaze, final int access, final String... modules) {
			stringBuilder
					.append(tab)
					.append("<span class=\"keyword\">")
					.append(method)
					.append("</span>")
					.append("<span class=\"import-declaration-package\">")
					.append(packaze)
					.append("</span>");
			if (modules != null && modules.length > 0) {
				stringBuilder
						.append(" <span class=\"keyword\">")
						.append("to")
						.append("</span>");
			} else {
				stringBuilder.append(';');
			}

			appendRawAccess(access);

			if (modules != null && modules.length > 0) {
				for (int i = 0; i < modules.length; ++i) {
					stringBuilder
							.append(tab2)
							.append("<span class=\"import-declaration-package\">")
							.append(modules[i])
							.append("</span>")
							.append(i != modules.length - 1 ? ",\n" : ";\n");
				}
			}
		}

		@Override
		public void visitUse(final String use) {
			stringBuilder
					.append(tab)
					.append("<span class=\"keyword\">")
					.append("uses")
					.append("</span> ");
			appendDescriptor(INTERNAL_NAME, use);
			stringBuilder.append(";\n");
		}

		@Override
		public void visitProvide(final String provide, final String... providers) {
			stringBuilder
					.append(tab)
					.append("<span class=\"keyword\">")
					.append("provides")
					.append("</span> ");
			appendDescriptor(INTERNAL_NAME, provide);
			stringBuilder
					.append(" <span class=\"keyword\">")
					.append("with")
					.append("</span>\n");
			for (int i = 0; i < providers.length; ++i) {
				stringBuilder.append(tab2);
				appendDescriptor(INTERNAL_NAME, providers[i]);
				stringBuilder.append(i != providers.length - 1 ? ",\n" : ";\n");
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Annotations
	// -----------------------------------------------------------------------------------------------

	protected void visitInnerAnnotationValue(final String name, final Object value) {
		visitAnnotationValue(name);

		if (value instanceof String) {
			visitString((String) value);
		} else if (value instanceof Type) {
			visitType((Type) value);
		} else if (value instanceof Byte) {
			visitByte(((Byte) value).byteValue());
		} else if (value instanceof Boolean) {
			visitBoolean(((Boolean) value).booleanValue());
		} else if (value instanceof Short) {
			visitShort(((Short) value).shortValue());
		} else if (value instanceof Character) {
			visitChar(((Character) value).charValue());
		} else if (value instanceof Integer) {
			visitInt(((Integer) value).intValue());
		} else if (value instanceof Float) {
			visitFloat(((Float) value).floatValue());
		} else if (value instanceof Long) {
			visitLong(((Long) value).longValue());
		} else if (value instanceof Double) {
			visitDouble(((Double) value).doubleValue());
		} else if (value.getClass().isArray()) {
			stringBuilder.append('{');

			if (value instanceof byte[]) {
				byte[] byteArray = (byte[]) value;

				for (int i = 0; i < byteArray.length; i++) {
					maybeAppendComma(i);
					visitByte(byteArray[i]);
				}
			} else if (value instanceof boolean[]) {
				boolean[] booleanArray = (boolean[]) value;

				for (int i = 0; i < booleanArray.length; i++) {
					maybeAppendComma(i);
					visitBoolean(booleanArray[i]);
				}
			} else if (value instanceof short[]) {
				short[] shortArray = (short[]) value;

				for (int i = 0; i < shortArray.length; i++) {
					maybeAppendComma(i);
					visitShort(shortArray[i]);
				}
			} else if (value instanceof char[]) {
				char[] charArray = (char[]) value;

				for (int i = 0; i < charArray.length; i++) {
					maybeAppendComma(i);
					visitChar(charArray[i]);
				}
			} else if (value instanceof int[]) {
				int[] intArray = (int[]) value;

				for (int i = 0; i < intArray.length; i++) {
					maybeAppendComma(i);
					visitInt(intArray[i]);
				}
			} else if (value instanceof long[]) {
				long[] longArray = (long[]) value;

				for (int i = 0; i < longArray.length; i++) {
					maybeAppendComma(i);
					visitLong(longArray[i]);
				}
			} else if (value instanceof float[]) {
				float[] floatArray = (float[]) value;

				for (int i = 0; i < floatArray.length; i++) {
					maybeAppendComma(i);
					visitFloat(floatArray[i]);
				}
			} else if (value instanceof double[]) {
				double[] doubleArray = (double[]) value;

				for (int i = 0; i < doubleArray.length; i++) {
					maybeAppendComma(i);
					visitDouble(doubleArray[i]);
				}
			}

			stringBuilder.append('}');
		}
	}

	private void visitInt(final int value) {
		stringBuilder.append(value);
	}

	private void visitLong(final long value) {
		stringBuilder
				.append(value)
				.append("<span class=\"keyword\">")
				.append('L')
				.append("</span>");
	}

	private void visitFloat(final float value) {
		stringBuilder
				.append(value)
				.append("<span class=\"keyword\">")
				.append('F')
				.append("</span>");
	}

	private void visitDouble(final double value) {
		stringBuilder
				.append(value)
				.append("<span class=\"keyword\">")
				.append('D')
				.append("</span>");
	}

	private void visitChar(final char value) {
		stringBuilder
				.append('(')
				.append("<span class=\"keyword\">")
				.append("char")
				.append("</span>")
				.append(')')
				.append((int) value);
	}

	private void visitShort(final short value) {
		stringBuilder
				.append('(')
				.append("<span class=\"keyword\">")
				.append("short")
				.append("</span>")
				.append(')')
				.append(value);
	}

	private void visitByte(final byte value) {
		stringBuilder
				.append('(')
				.append("<span class=\"keyword\">")
				.append("byte")
				.append("</span>")
				.append(')')
				.append(value);
	}

	private void visitBoolean(final boolean value) {
		stringBuilder
				.append("<span class=\"keyword\">")
				.append(value)
				.append("</span>");
	}

	private void visitString(final String value) {
		stringBuilder.append("<span class=\"string\">");
		appendString(stringBuilder, value);
		stringBuilder.append("</span>");
	}

	private void visitType(final Type value) {
		stringBuilder
				.append("<span class=\"class-name\">")
				.append(value.getClassName())
				.append("</span>")
				.append("<span class=\"field\">")
				.append(CLASS_SUFFIX)
				.append("</span>");
	}

	protected void visitEnum(final String name, final String descriptor, final String value) {
		visitAnnotationValue(name);
		appendDescriptor(FIELD_DESCRIPTOR, descriptor);
		stringBuilder
				.append('.')
				.append("<span class=\"enum-constant\">")
				.append(value)
				.append("</span>");
	}

	public void visitNestedAnnotation(String name, BcAnnotation bcAnnotation) {
		annotation = true;
		stringBuilder.append("<span class=\"annotation\">");
		visitAnnotationValue(name);
		stringBuilder.append('@');
		appendDescriptor(FIELD_DESCRIPTOR, bcAnnotation.getDescriptor());
		stringBuilder.append('(');

		visitAnnotationElements(bcAnnotation);

		stringBuilder.append(")</span>");
		annotation = false;
	}

	public void visitArray(final String name) {
		visitAnnotationValue(name);
		stringBuilder.append('{');
		stringBuilder.append("}");
	}

	private void visitAnnotationValue(final String name) {
		maybeAppendComma(numAnnotationValues++);

		if (name != null) {
			stringBuilder
					.append("<span class=\"variable\">")
					.append(name)
					.append("</span>")
					.append('=');
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Methods
	// -----------------------------------------------------------------------------------------------

	protected void visitParameter(BcParameter bcParameter) {
		stringBuilder
				.append(tab2)
				.append("<span class=\"comment\">")
				.append("// parameter ");
		appendAccess(bcParameter.getAccess());

		String name = bcParameter.getName();

		stringBuilder
				.append(' ')
				.append((name == null) ? "<no name>" : name)
				.append("</span>\n");
	}

	public void visitAnnotationDefault() {
		stringBuilder.append(tab2 + "default=");
		stringBuilder.append("\n");
	}

	public void visitParameterAnnotation(int paramIndex, BcAnnotation bcAnnotation) {
		annotation = true;
		stringBuilder.append("<span class=\"annotation\">");
		stringBuilder
				.append(tab2)
				.append('@');
		appendDescriptor(FIELD_DESCRIPTOR, bcAnnotation.getDescriptor());
		annotation = false;
		stringBuilder.append('(');

		visitAnnotationElements(bcAnnotation);

		stringBuilder
				.append(")")
				.append("</span> ")
				.append("<span class=\"comment\">")
				.append(bcAnnotation.getVisibility() != 0 ? "// parameter " : "// invisible, parameter ")
				.append(paramIndex)
				.append("</span>\n");
	}

	protected void visitFrame(
			final int type,
			final int numLocal,
			final Object[] local,
			final int numStack,
			final Object[] stack) {
		stringBuilder.append('\n');
		stringBuilder
				.append(ltab)
				.append("<span class=\"keyword\">")
				.append("FRAME ");
		switch (type) {
		case Opcodes.F_NEW:
		case Opcodes.F_FULL:
			stringBuilder
					.append("FULL")
					.append("</span> ")
					.append('[');
			appendFrameTypes(numLocal, local);
			stringBuilder.append("] [");
			appendFrameTypes(numStack, stack);
			stringBuilder.append(']');
			break;
		case Opcodes.F_APPEND:
			stringBuilder
					.append("APPEND")
					.append("</span> ")
					.append('[');
			appendFrameTypes(numLocal, local);
			stringBuilder.append(']');
			break;
		case Opcodes.F_CHOP:
			stringBuilder
					.append("CHOP")
					.append("</span> ")
					.append(numLocal);
			break;
		case Opcodes.F_SAME:
			stringBuilder
					.append("SAME")
					.append("</span>");
			break;
		case Opcodes.F_SAME1:
			stringBuilder
					.append("SAME1")
					.append("</span> ");
			appendFrameTypes(1, stack);
			break;
		default:
			throw new IllegalArgumentException();
		}

		stringBuilder.append('\n');
	}

	protected void visitInsn(final int opcode) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append(OPCODES[opcode])
				.append("</span>\n");
	}

	protected void visitIntInsn(final int opcode, final int operand) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append(OPCODES[opcode])
				.append(' ');

		if (opcode == Opcodes.NEWARRAY) {
			stringBuilder
					.append(TYPES[operand])
					.append("</span>");
		} else {
			stringBuilder
					.append("</span>")
					.append(Integer.toString(operand));
		}

		stringBuilder.append('\n');
	}

	protected void visitVarInsn(final int opcode, final int varIndex) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append(OPCODES[opcode])
				.append("</span>")
				.append(' ')
				.append("<span class=\"number\">")
				.append(varIndex)
				.append("</span>")
				.append('\n');
	}

	protected void visitTypeInsn(final int opcode, final String type) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append(OPCODES[opcode])
				.append("</span>")
				.append(' ');
		appendDescriptor(INTERNAL_NAME, type);
		stringBuilder.append('\n');
	}

	protected void visitFieldInsn(
			final int opcode, final String owner, final String name, final String descriptor) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append(OPCODES[opcode])
				.append("</span>")
				.append(' ');
		appendDescriptor(INTERNAL_NAME, owner);
		stringBuilder
				.append('.')
				.append("<span class=\"field\">")
				.append(name)
				.append("</span>")
				.append(" : ");
		appendDescriptor(FIELD_DESCRIPTOR, descriptor);
		stringBuilder.append('\n');
	}

	protected void visitMethodInsn(
			final int opcode,
			final String owner,
			final String name,
			final String descriptor,
			final boolean isInterface) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append(OPCODES[opcode])
				.append("</span>")
				.append(' ');
		appendDescriptor(INTERNAL_NAME, owner);
		stringBuilder
				.append('.')
				.append("<span class=\"method-name\">")
				.append(name)
				.append("</span> ");
		appendDescriptor(METHOD_DESCRIPTOR, descriptor);

		if (isInterface) {
			stringBuilder.append(" (itf)");
		}

		stringBuilder.append('\n');
	}

	protected void visitInvokeDynamicInsn(
			final String name,
			final String descriptor,
			final Handle bootstrapMethodHandle,
			final Object... bootstrapMethodArguments) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("INVOKEDYNAMIC")
				.append("</span> ")
				.append("<span class=\"method-name\">")
				.append(name)
				.append("</span>");
		appendDescriptor(METHOD_DESCRIPTOR, descriptor);
		stringBuilder
				.append(" [")
				.append('\n')
				.append(tab3);
		appendHandle(bootstrapMethodHandle);
		stringBuilder
				.append('\n')
				.append(tab3)
				.append("<span class=\"comment\">")
				.append("// arguments:");
		if (bootstrapMethodArguments.length == 0) {
			stringBuilder
					.append(" none")
					.append("</span>");
		} else {
			stringBuilder.append("</span>\n");

			for (Object value : bootstrapMethodArguments) {
				stringBuilder.append(tab3);

				if (value instanceof String) {
					stringBuilder.append("<span class=\"string\">");
					Printer.appendString(stringBuilder, (String) value);
					stringBuilder.append("</span>");
				} else if (value instanceof Type) {
					Type type = (Type) value;

					if (type.getSort() == Type.METHOD) {
						appendDescriptor(METHOD_DESCRIPTOR, type.getDescriptor());
					} else {
						visitType(type);
					}
				} else if (value instanceof Handle) {
					appendHandle((Handle) value);
				} else {
					stringBuilder.append(value);
				}

				stringBuilder.append(", \n");
			}

			stringBuilder.setLength(stringBuilder.length() - 3);
		}

		stringBuilder
				.append('\n')
				.append(tab2)
				.append("]\n");
	}

	protected void visitJumpInsn(final int opcode, final Label label) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append(OPCODES[opcode])
				.append("</span>")
				.append(' ');
		appendLabel(label);
		stringBuilder.append('\n');
	}

	protected void visitLdcInsn(final Object value) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("LDC")
				.append("</span> ");
		if (value instanceof String) {
			stringBuilder.append("<span class=\"string\">");
			Printer.appendString(stringBuilder, (String) value);
			stringBuilder.append("</span>");
		} else if (value instanceof Type) {
			stringBuilder
					.append("<span class=\"class-name\">")
					.append(((Type) value).getDescriptor())
					.append("</span>")
					.append("<span class=\"field\">")
					.append(CLASS_SUFFIX)
					.append("</span> ");
		} else {
			stringBuilder
					.append("<span class=\"number\">")
					.append(value)
					.append("</span>");
		}

		stringBuilder.append('\n');
	}

	protected void visitIincInsn(final int varIndex, final int increment) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("IINC")
				.append("</span> ")
				.append(varIndex)
				.append(' ')
				.append(increment)
				.append('\n');
	}

	protected void visitTableSwitchInsn(
			final int min, final int max, final Label dflt, final Label... labels) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("TABLESWITCH")
				.append("</span>\n");
		for (int i = 0; i < labels.length; ++i) {
			stringBuilder
					.append(tab3)
					.append(min + i)
					.append(": ");
			appendLabel(labels[i]);
			stringBuilder.append('\n');
		}

		stringBuilder
				.append(tab3)
				.append("default: ");
		appendLabel(dflt);
		stringBuilder.append('\n');
	}

	protected void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("LOOKUPSWITCH")
				.append("</span>\n");
		for (int i = 0; i < labels.length; ++i) {
			stringBuilder
					.append(tab3)
					.append(keys[i])
					.append(": ");
			appendLabel(labels[i]);
			stringBuilder.append('\n');
		}

		stringBuilder
				.append(tab3)
				.append("default: ");
		appendLabel(dflt);
		stringBuilder.append('\n');
	}

	protected void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("MULTIANEWARRAY")
				.append("</span> ");
		appendDescriptor(FIELD_DESCRIPTOR, descriptor);
		stringBuilder
				.append(' ')
				.append(numDimensions)
				.append('\n');
	}

	protected void visitTryCatchBlock(
			final Label start, final Label end, final Label handler, final String type) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("TRYCATCHBLOCK")
				.append("</span> ");
		appendLabel(start);
		stringBuilder.append(' ');
		appendLabel(end);
		stringBuilder.append(' ');
		appendLabel(handler);
		stringBuilder.append(' ');
		appendDescriptor(INTERNAL_NAME, type);
		stringBuilder.append('\n');
	}

	public void visitTryCatchAnnotation(
			final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("TRYCATCHBLOCK @")
				.append("</span>");
		appendDescriptor(FIELD_DESCRIPTOR, descriptor);
		stringBuilder.append('(');

		stringBuilder.append('\n');
		stringBuilder.append(") : ");
		appendTypeReference(typeRef);
		stringBuilder
				.append(", ")
				.append(typePath)
				.append(visible ? "\n" : INVISIBLE);
	}

	protected void visitLocalVariable(
			final String name,
			final String descriptor,
			final String signature,
			final Label start,
			final Label end,
			final int index) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("LOCALVARIABLE")
				.append("</span> ")
				.append("<span class=\"variable\">")
				.append(name)
				.append("</span> ");
		appendDescriptor(FIELD_DESCRIPTOR, descriptor);
		stringBuilder.append(' ');
		appendLabel(start);
		stringBuilder.append(' ');
		appendLabel(end);
		stringBuilder
				.append(' ')
				.append("<span class=\"number\">")
				.append(index)
				.append("</span>")
				.append('\n');

		if (signature != null) {
			stringBuilder.append(tab2);
			appendDescriptor(FIELD_SIGNATURE, signature);
			stringBuilder.append(tab2);
			appendJavaDeclaration(name, signature);
		}
	}

	public void visitLocalVariableAnnotation(
			final int typeRef,
			final TypePath typePath,
			final Label[] start,
			final Label[] end,
			final int[] index,
			final String descriptor,
			final boolean visible) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"keyword\">")
				.append("LOCALVARIABLE @")
				.append("</span>");
		appendDescriptor(FIELD_DESCRIPTOR, descriptor);
		stringBuilder.append('(');

		stringBuilder.append('\n');
		stringBuilder.append(") : ");
		appendTypeReference(typeRef);
		stringBuilder
				.append(", ")
				.append(typePath);
		for (int i = 0; i < start.length; ++i) {
			stringBuilder.append(" [ ");
			appendLabel(start[i]);
			stringBuilder.append(" - ");
			appendLabel(end[i]);
			stringBuilder
					.append(" - ")
					.append(index[i])
					.append(" ]");
		}

		stringBuilder.append(visible ? "\n" : INVISIBLE);
	}

	protected void visitLineNumber(final int line, final Label start) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"variable\">")
				.append("LINENUMBER")
				.append("</span> ")
				.append("<span class=\"number\">")
				.append(line)
				.append("</span> ")
				.append(' ');
		appendLabel(start);
		stringBuilder.append('\n');
	}

	protected void visitMaxs(final int maxStack, final int maxLocals) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"variable\">")
				.append("MAXSTACK")
				.append("</span>")
				.append(" = ")
				.append("<span class=\"number\">")
				.append(maxStack)
				.append("</span>")
				.append('\n');

		stringBuilder.append('\n');
		stringBuilder
				.append(tab2)
				.append("<span class=\"variable\">")
				.append("MAXLOCALS")
				.append("</span>")
				.append(" = ")
				.append("<span class=\"number\">")
				.append(maxLocals)
				.append("</span>")
				.append('\n');
	}

	// -----------------------------------------------------------------------------------------------
	// Common methods
	// -----------------------------------------------------------------------------------------------

	public void visitAnnotation(BcAnnotation bcAnnotation, boolean visible) {
		annotation = true;
		stringBuilder
				.append(tab)
				.append("<span class=\"annotation\">")
				.append('@');
		appendDescriptor(FIELD_DESCRIPTOR, bcAnnotation.getDescriptor());
		stringBuilder
				.append("</span>")
				.append('(');

		visitAnnotationElements(bcAnnotation);

		stringBuilder.append(visible ? ")\n" : ")" + INVISIBLE);
		annotation = false;
	}

	public void visitTypeAnnotation(BcTypeAnnotation bcAnnotation, boolean visible) {
		annotation = true;
		stringBuilder
				.append(tab)
				.append("<span class=\"annotation\">")
				.append('@');

		appendDescriptor(FIELD_DESCRIPTOR, bcAnnotation.getDescriptor());
		stringBuilder
				.append("</span>")
				.append('(');

		visitAnnotationElements(bcAnnotation);

		stringBuilder.append(") : ");
		appendTypeReference(bcAnnotation.getTypeRef());
		stringBuilder
				.append(", ")
				.append(bcAnnotation.getTypePath())
				.append(visible ? '\n' : INVISIBLE);
		annotation = false;
	}

	private void visitAnnotationElements(BcAnnotation bcAnnotation) {
		for (BcAnnotationElement element : bcAnnotation.getElements()) {
			visitAnnotationElement(element.getName(), element.getValue());
		}
	}

	private void visitAnnotationElement(String name, Object value) {
		if (value instanceof String[] typeValue) {
			visitEnum(name, typeValue[0], typeValue[1]);
		} else if (value instanceof BcAnnotation innerAnnotation) {
			visitNestedAnnotation(name, innerAnnotation);
		} else if (value instanceof List<?> array) {
			visitArray(name);

			for (Object arrayValue : array) {
				visitAnnotationElement(null, arrayValue);
			}
		} else {
			visitInnerAnnotationValue(name, value);
		}
	}

	protected void visitAttribute(final Attribute attribute) {
		stringBuilder.append('\n');
		stringBuilder
				.append(tab)
				.append("<span class=\"keyword\">")
				.append("ATTRIBUTE")
				.append("</span> ");
		appendDescriptor(-1, attribute.type);

		if (attribute instanceof TextifierSupport) {
			if (labelNames == null) {
				labelNames = new HashMap<>();
			}

			((TextifierSupport) attribute).textify(stringBuilder, labelNames);
		} else {
			stringBuilder.append(" : unknown\n");
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Utility methods
	// -----------------------------------------------------------------------------------------------

	/**
	 * Appends a string representation of the given access flags to {@link #stringBuilder}.
	 *
	 * @param accessFlags some access flags.
	 */
	private void appendAccess(final int accessFlags) {
		stringBuilder.append("<span class=\"keyword\">");

		if ((accessFlags & Opcodes.ACC_PUBLIC) != 0) {
			stringBuilder.append("public ");
		}

		if ((accessFlags & Opcodes.ACC_PRIVATE) != 0) {
			stringBuilder.append("private ");
		}

		if ((accessFlags & Opcodes.ACC_PROTECTED) != 0) {
			stringBuilder.append("protected ");
		}

		if ((accessFlags & Opcodes.ACC_FINAL) != 0) {
			stringBuilder.append("final ");
		}

		if ((accessFlags & Opcodes.ACC_STATIC) != 0) {
			stringBuilder.append("static ");
		}

		if ((accessFlags & Opcodes.ACC_SYNCHRONIZED) != 0) {
			stringBuilder.append("synchronized ");
		}

		if ((accessFlags & Opcodes.ACC_VOLATILE) != 0) {
			stringBuilder.append("volatile ");
		}

		if ((accessFlags & Opcodes.ACC_TRANSIENT) != 0) {
			stringBuilder.append("transient ");
		}

		if ((accessFlags & Opcodes.ACC_ABSTRACT) != 0) {
			stringBuilder.append("abstract ");
		}

		if ((accessFlags & Opcodes.ACC_STRICT) != 0) {
			stringBuilder.append("strictfp ");
		}

		if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0) {
			stringBuilder.append("synthetic ");
		}

		if ((accessFlags & Opcodes.ACC_MANDATED) != 0) {
			stringBuilder.append("mandated ");
		}

		if ((accessFlags & Opcodes.ACC_ENUM) != 0) {
			stringBuilder.append("enum ");
		}

		stringBuilder.append("</span>");
	}

	/**
	 * Appends the hexadecimal value of the given access flags to {@link #stringBuilder}.
	 *
	 * @param accessFlags some access flags.
	 */
	private void appendRawAccess(final int accessFlags) {
		stringBuilder
				.append("<span class=\"comment\">")
				.append("// access flags 0x")
				.append(Integer.toHexString(accessFlags).toUpperCase())
				.append("</span>\n");
	}

	protected void appendDescriptor(final int type, final String value) {
		if (type == CLASS_SIGNATURE || type == FIELD_SIGNATURE || type == METHOD_SIGNATURE) {
			if (value != null) {
				stringBuilder
						.append("<span class=\"comment\">")
						.append("// signature ")
						.append(value)
						.append("</span>\n");
			}
		} else {
			stringBuilder
					.append(annotation ? "" : "<span class=\"class-name\">")
					.append(value)
					.append(annotation ? "" : "</span>");
		}
	}

	/**
	 * Appends the Java generic type declaration corresponding to the given signature.
	 *
	 * @param name a class, field or method name.
	 * @param signature a class, field or method signature.
	 */
	private void appendJavaDeclaration(final String name, final String signature) {
		TraceSignatureVisitor traceSignatureVisitor = new TraceSignatureVisitor(clsAccess);
		new SignatureReader(signature).accept(traceSignatureVisitor);
		stringBuilder
				.append("<span class=\"comment\">")
				.append("// declaration: ");
		if (traceSignatureVisitor.getReturnType() != null) {
			stringBuilder.append(traceSignatureVisitor.getReturnType());
			stringBuilder.append(' ');
		}

		stringBuilder.append(name);
		stringBuilder.append(traceSignatureVisitor.getDeclaration());

		if (traceSignatureVisitor.getExceptions() != null) {
			stringBuilder
					.append(" throws ")
					.append(traceSignatureVisitor.getExceptions());
		}

		stringBuilder.append("</span>\n");
	}

	protected void appendLabel(final Label label) {
		if (labelNames == null) {
			labelNames = new HashMap<>();
		}

		String name = labelNames.get(label);

		if (name == null) {
			name = "L" + labelNames.size();
			labelNames.put(label, name);
		}

		boolean number = numberPattern.matcher(name).matches();

		stringBuilder
				.append(number ? "<span class=\"number\">" : "<span class=\"variable\">")
				.append(name)
				.append("</span>");
	}

	protected void appendHandle(final Handle handle) {
		int tag = handle.getTag();
		stringBuilder
				.append("<span class=\"comment\">")
				.append("// handle kind 0x")
				.append(Integer.toHexString(tag))
				.append(" : ");
		boolean isMethodHandle = false;
		switch (tag) {
		case Opcodes.H_GETFIELD:
			stringBuilder.append("GETFIELD");
			break;
		case Opcodes.H_GETSTATIC:
			stringBuilder.append("GETSTATIC");
			break;
		case Opcodes.H_PUTFIELD:
			stringBuilder.append("PUTFIELD");
			break;
		case Opcodes.H_PUTSTATIC:
			stringBuilder.append("PUTSTATIC");
			break;
		case Opcodes.H_INVOKEINTERFACE:
			stringBuilder.append("INVOKEINTERFACE");
			isMethodHandle = true;
			break;
		case Opcodes.H_INVOKESPECIAL:
			stringBuilder.append("INVOKESPECIAL");
			isMethodHandle = true;
			break;
		case Opcodes.H_INVOKESTATIC:
			stringBuilder.append("INVOKESTATIC");
			isMethodHandle = true;
			break;
		case Opcodes.H_INVOKEVIRTUAL:
			stringBuilder.append("INVOKEVIRTUAL");
			isMethodHandle = true;
			break;
		case Opcodes.H_NEWINVOKESPECIAL:
			stringBuilder.append("NEWINVOKESPECIAL");
			isMethodHandle = true;
			break;
		default:
			throw new IllegalArgumentException();
		}

		stringBuilder.append("</span>\n");
		stringBuilder.append(tab3);
		appendDescriptor(INTERNAL_NAME, handle.getOwner());
		stringBuilder.append('.');
		stringBuilder.append(handle.getName());

		if (!isMethodHandle) {
			stringBuilder.append('(');
		}

		appendDescriptor(HANDLE_DESCRIPTOR, handle.getDesc());

		if (!isMethodHandle) {
			stringBuilder.append(')');
		}

		if (handle.isInterface()) {
			stringBuilder.append("itf");
		}
	}

	/**
	 * Appends a comma to {@link #stringBuilder} if the given number is strictly positive.
	 *
	 * @param numValues a number of 'values visited so far', for instance the number of annotation
	 *     values visited so far in an annotation visitor.
	 */
	private void maybeAppendComma(final int numValues) {
		if (numValues > 0) {
			stringBuilder.append(", ");
		}
	}

	/**
	 * Appends a string representation of the given type reference to {@link #stringBuilder}.
	 *
	 * @param typeRef a type reference. See {@link TypeReference}.
	 */
	private void appendTypeReference(final int typeRef) {
		TypeReference typeReference = new TypeReference(typeRef);

		stringBuilder.append("<span class=\"keyword\">");

		switch (typeReference.getSort()) {
		case TypeReference.CLASS_TYPE_PARAMETER:
			stringBuilder
					.append("CLASS_TYPE_PARAMETER")
					.append("</span> ")
					.append(typeReference.getTypeParameterIndex());
			break;
		case TypeReference.METHOD_TYPE_PARAMETER:
			stringBuilder
					.append("METHOD_TYPE_PARAMETER")
					.append("</span> ")
					.append(typeReference.getTypeParameterIndex());
			break;
		case TypeReference.CLASS_EXTENDS:
			stringBuilder
					.append("CLASS_EXTENDS")
					.append("</span> ")
					.append(typeReference.getSuperTypeIndex());
			break;
		case TypeReference.CLASS_TYPE_PARAMETER_BOUND:
			stringBuilder
					.append("CLASS_TYPE_PARAMETER_BOUND")
					.append("</span> ")
					.append(typeReference.getTypeParameterIndex())
					.append(", ")
					.append(typeReference.getTypeParameterBoundIndex());
			break;
		case TypeReference.METHOD_TYPE_PARAMETER_BOUND:
			stringBuilder
					.append("METHOD_TYPE_PARAMETER_BOUND")
					.append("</span> ")
					.append(typeReference.getTypeParameterIndex())
					.append(", ")
					.append(typeReference.getTypeParameterBoundIndex());
			break;
		case TypeReference.FIELD:
			stringBuilder
					.append("FIELD")
					.append("</span>");
			break;
		case TypeReference.METHOD_RETURN:
			stringBuilder
					.append("METHOD_RETURN")
					.append("</span>");
			break;
		case TypeReference.METHOD_RECEIVER:
			stringBuilder
					.append("METHOD_RECEIVER")
					.append("</span>");
			break;
		case TypeReference.METHOD_FORMAL_PARAMETER:
			stringBuilder
					.append("METHOD_FORMAL_PARAMETER")
					.append("</span> ")
					.append(typeReference.getFormalParameterIndex());
			break;
		case TypeReference.THROWS:
			stringBuilder
					.append("THROWS")
					.append("</span> ")
					.append(typeReference.getExceptionIndex());
			break;
		case TypeReference.LOCAL_VARIABLE:
			stringBuilder
					.append("LOCAL_VARIABLE")
					.append("</span>");
			break;
		case TypeReference.RESOURCE_VARIABLE:
			stringBuilder
					.append("RESOURCE_VARIABLE")
					.append("</span>");
			break;
		case TypeReference.EXCEPTION_PARAMETER:
			stringBuilder
					.append("EXCEPTION_PARAMETER")
					.append("</span> ")
					.append(typeReference.getTryCatchBlockIndex());
			break;
		case TypeReference.INSTANCEOF:
			stringBuilder
					.append("INSTANCEOF")
					.append("</span>");
			break;
		case TypeReference.NEW:
			stringBuilder
					.append("NEW")
					.append("</span>");
			break;
		case TypeReference.CONSTRUCTOR_REFERENCE:
			stringBuilder
					.append("CONSTRUCTOR_REFERENCE")
					.append("</span>");
			break;
		case TypeReference.METHOD_REFERENCE:
			stringBuilder
					.append("METHOD_REFERENCE")
					.append("</span>");
			break;
		case TypeReference.CAST:
			stringBuilder
					.append("CAST")
					.append("</span> ")
					.append(typeReference.getTypeArgumentIndex());
			break;
		case TypeReference.CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
			stringBuilder
					.append("CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT")
					.append("</span> ")
					.append(typeReference.getTypeArgumentIndex());
			break;
		case TypeReference.METHOD_INVOCATION_TYPE_ARGUMENT:
			stringBuilder
					.append("METHOD_INVOCATION_TYPE_ARGUMENT")
					.append("</span> ")
					.append(typeReference.getTypeArgumentIndex());
			break;
		case TypeReference.CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
			stringBuilder
					.append("CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT")
					.append("</span> ")
					.append(typeReference.getTypeArgumentIndex());
			break;
		case TypeReference.METHOD_REFERENCE_TYPE_ARGUMENT:
			stringBuilder
					.append("METHOD_REFERENCE_TYPE_ARGUMENT")
					.append("</span> ")
					.append(typeReference.getTypeArgumentIndex());
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Appends the given stack map frame types to {@link #stringBuilder}.
	 *
	 * @param numTypes the number of stack map frame types in 'frameTypes'.
	 * @param frameTypes an array of stack map frame types, in the format described in {@link
	 *     org.objectweb.asm.MethodVisitor#visitFrame}.
	 */
	private void appendFrameTypes(final int numTypes, final Object[] frameTypes) {
		for (int i = 0; i < numTypes; ++i) {
			if (i > 0) {
				stringBuilder.append(' ');
			}

			if (frameTypes[i] instanceof String) {
				String descriptor = (String) frameTypes[i];

				if (descriptor.charAt(0) == '[') {
					appendDescriptor(FIELD_DESCRIPTOR, descriptor);
				} else {
					appendDescriptor(INTERNAL_NAME, descriptor);
				}
			} else if (frameTypes[i] instanceof Integer) {
				stringBuilder
						.append("<span class=\"class-name\">")
						.append(FRAME_TYPES.get(((Integer) frameTypes[i]).intValue()))
						.append("</span>");
			} else {
				appendLabel((Label) frameTypes[i]);
			}
		}
	}

	/**
	 * Appends a quoted string to the given string builder.
	 *
	 * @param stringBuilder the buffer where the string must be added.
	 * @param string the string to be added.
	 */
	public static void appendString(final StringBuilder stringBuilder, final String string) {
		stringBuilder.append('\"');

		for (int i = 0; i < string.length(); ++i) {
			char c = string.charAt(i);

			if (c == '\n') {
				stringBuilder.append("\\n");
			} else if (c == '\r') {
				stringBuilder.append("\\r");
			} else if (c == '\\') {
				stringBuilder.append("\\\\");
			} else if (c == '"') {
				stringBuilder.append("\\\"");
			} else if (c < 0x20 || c > 0x7f) {
				stringBuilder.append("\\u");

				if (c < 0x10) {
					stringBuilder.append("000");
				} else if (c < 0x100) {
					stringBuilder.append("00");
				} else if (c < 0x1000) {
					stringBuilder.append('0');
				}

				stringBuilder.append(Integer.toString(c, 16));
			} else {
				stringBuilder.append(c);
			}
		}

		stringBuilder.append('\"');
	}

	/**
	 * The type of internal names (see {@link Type#getInternalName()}). See {@link #appendDescriptor}.
	 */
	public static final int INTERNAL_NAME = 0;

	/** The type of field descriptors. See {@link #appendDescriptor}. */
	public static final int FIELD_DESCRIPTOR = 1;

	/** The type of field signatures. See {@link #appendDescriptor}. */
	public static final int FIELD_SIGNATURE = 2;

	/** The type of method descriptors. See {@link #appendDescriptor}. */
	public static final int METHOD_DESCRIPTOR = 3;

	/** The type of method signatures. See {@link #appendDescriptor}. */
	public static final int METHOD_SIGNATURE = 4;

	/** The type of class signatures. See {@link #appendDescriptor}. */
	public static final int CLASS_SIGNATURE = 5;

	/** The type of method handle descriptors. See {@link #appendDescriptor}. */
	public static final int HANDLE_DESCRIPTOR = 9;

	private static final String CLASS_SUFFIX = ".class";
	private static final String DEPRECATED = "<span class=\"comment\">// DEPRECATED</span>\n";
	private static final String RECORD = "<span class=\"comment\">// RECORD</span>\n";
	private static final String INVISIBLE = " <span class=\"comment\">// invisible</span>\n";

	private static final List<String> FRAME_TYPES =
			Collections.unmodifiableList(Arrays.asList("T", "I", "F", "D", "J", "N", "U"));

	/** The indentation of class members at depth level 1 (e.g. fields, methods). */
	protected String tab = "   ";

	/** The indentation of class elements at depth level 2 (e.g. bytecode instructions in methods). */
	protected String tab2 = "      ";

	/** The indentation of class elements at depth level 3 (e.g. switch cases in methods). */
	protected String tab3 = "         ";

	/** The indentation of labels. */
	protected String ltab = "    ";

	/** The names of the labels. */
	protected Map<Label, String> labelNames;

	/** The access flags of the visited class. */
	private int clsAccess;

	/** The number of annotation values visited so far. */
	private int numAnnotationValues;

	/** Is the current element an annotation? */
	private boolean annotation;

	private static final Pattern numberPattern = Pattern.compile("-?\\d+(\\.\\d+)?");

	private final ClassInstance cls;
	private final NameType nameType;
	private final StringBuilder stringBuilder = new StringBuilder();
	private final HtmlModuleVisitor moduleVisitor = new HtmlModuleVisitor();

	/** The names of the Java Virtual Machine opcodes. */
	public static final String[] OPCODES = {
			"NOP", // 0 (0x0)
			"ACONST_NULL", // 1 (0x1)
			"ICONST_M1", // 2 (0x2)
			"ICONST_0", // 3 (0x3)
			"ICONST_1", // 4 (0x4)
			"ICONST_2", // 5 (0x5)
			"ICONST_3", // 6 (0x6)
			"ICONST_4", // 7 (0x7)
			"ICONST_5", // 8 (0x8)
			"LCONST_0", // 9 (0x9)
			"LCONST_1", // 10 (0xa)
			"FCONST_0", // 11 (0xb)
			"FCONST_1", // 12 (0xc)
			"FCONST_2", // 13 (0xd)
			"DCONST_0", // 14 (0xe)
			"DCONST_1", // 15 (0xf)
			"BIPUSH", // 16 (0x10)
			"SIPUSH", // 17 (0x11)
			"LDC", // 18 (0x12)
			"LDC_W", // 19 (0x13)
			"LDC2_W", // 20 (0x14)
			"ILOAD", // 21 (0x15)
			"LLOAD", // 22 (0x16)
			"FLOAD", // 23 (0x17)
			"DLOAD", // 24 (0x18)
			"ALOAD", // 25 (0x19)
			"ILOAD_0", // 26 (0x1a)
			"ILOAD_1", // 27 (0x1b)
			"ILOAD_2", // 28 (0x1c)
			"ILOAD_3", // 29 (0x1d)
			"LLOAD_0", // 30 (0x1e)
			"LLOAD_1", // 31 (0x1f)
			"LLOAD_2", // 32 (0x20)
			"LLOAD_3", // 33 (0x21)
			"FLOAD_0", // 34 (0x22)
			"FLOAD_1", // 35 (0x23)
			"FLOAD_2", // 36 (0x24)
			"FLOAD_3", // 37 (0x25)
			"DLOAD_0", // 38 (0x26)
			"DLOAD_1", // 39 (0x27)
			"DLOAD_2", // 40 (0x28)
			"DLOAD_3", // 41 (0x29)
			"ALOAD_0", // 42 (0x2a)
			"ALOAD_1", // 43 (0x2b)
			"ALOAD_2", // 44 (0x2c)
			"ALOAD_3", // 45 (0x2d)
			"IALOAD", // 46 (0x2e)
			"LALOAD", // 47 (0x2f)
			"FALOAD", // 48 (0x30)
			"DALOAD", // 49 (0x31)
			"AALOAD", // 50 (0x32)
			"BALOAD", // 51 (0x33)
			"CALOAD", // 52 (0x34)
			"SALOAD", // 53 (0x35)
			"ISTORE", // 54 (0x36)
			"LSTORE", // 55 (0x37)
			"FSTORE", // 56 (0x38)
			"DSTORE", // 57 (0x39)
			"ASTORE", // 58 (0x3a)
			"ISTORE_0", // 59 (0x3b)
			"ISTORE_1", // 60 (0x3c)
			"ISTORE_2", // 61 (0x3d)
			"ISTORE_3", // 62 (0x3e)
			"LSTORE_0", // 63 (0x3f)
			"LSTORE_1", // 64 (0x40)
			"LSTORE_2", // 65 (0x41)
			"LSTORE_3", // 66 (0x42)
			"FSTORE_0", // 67 (0x43)
			"FSTORE_1", // 68 (0x44)
			"FSTORE_2", // 69 (0x45)
			"FSTORE_3", // 70 (0x46)
			"DSTORE_0", // 71 (0x47)
			"DSTORE_1", // 72 (0x48)
			"DSTORE_2", // 73 (0x49)
			"DSTORE_3", // 74 (0x4a)
			"ASTORE_0", // 75 (0x4b)
			"ASTORE_1", // 76 (0x4c)
			"ASTORE_2", // 77 (0x4d)
			"ASTORE_3", // 78 (0x4e)
			"IASTORE", // 79 (0x4f)
			"LASTORE", // 80 (0x50)
			"FASTORE", // 81 (0x51)
			"DASTORE", // 82 (0x52)
			"AASTORE", // 83 (0x53)
			"BASTORE", // 84 (0x54)
			"CASTORE", // 85 (0x55)
			"SASTORE", // 86 (0x56)
			"POP", // 87 (0x57)
			"POP2", // 88 (0x58)
			"DUP", // 89 (0x59)
			"DUP_X1", // 90 (0x5a)
			"DUP_X2", // 91 (0x5b)
			"DUP2", // 92 (0x5c)
			"DUP2_X1", // 93 (0x5d)
			"DUP2_X2", // 94 (0x5e)
			"SWAP", // 95 (0x5f)
			"IADD", // 96 (0x60)
			"LADD", // 97 (0x61)
			"FADD", // 98 (0x62)
			"DADD", // 99 (0x63)
			"ISUB", // 100 (0x64)
			"LSUB", // 101 (0x65)
			"FSUB", // 102 (0x66)
			"DSUB", // 103 (0x67)
			"IMUL", // 104 (0x68)
			"LMUL", // 105 (0x69)
			"FMUL", // 106 (0x6a)
			"DMUL", // 107 (0x6b)
			"IDIV", // 108 (0x6c)
			"LDIV", // 109 (0x6d)
			"FDIV", // 110 (0x6e)
			"DDIV", // 111 (0x6f)
			"IREM", // 112 (0x70)
			"LREM", // 113 (0x71)
			"FREM", // 114 (0x72)
			"DREM", // 115 (0x73)
			"INEG", // 116 (0x74)
			"LNEG", // 117 (0x75)
			"FNEG", // 118 (0x76)
			"DNEG", // 119 (0x77)
			"ISHL", // 120 (0x78)
			"LSHL", // 121 (0x79)
			"ISHR", // 122 (0x7a)
			"LSHR", // 123 (0x7b)
			"IUSHR", // 124 (0x7c)
			"LUSHR", // 125 (0x7d)
			"IAND", // 126 (0x7e)
			"LAND", // 127 (0x7f)
			"IOR", // 128 (0x80)
			"LOR", // 129 (0x81)
			"IXOR", // 130 (0x82)
			"LXOR", // 131 (0x83)
			"IINC", // 132 (0x84)
			"I2L", // 133 (0x85)
			"I2F", // 134 (0x86)
			"I2D", // 135 (0x87)
			"L2I", // 136 (0x88)
			"L2F", // 137 (0x89)
			"L2D", // 138 (0x8a)
			"F2I", // 139 (0x8b)
			"F2L", // 140 (0x8c)
			"F2D", // 141 (0x8d)
			"D2I", // 142 (0x8e)
			"D2L", // 143 (0x8f)
			"D2F", // 144 (0x90)
			"I2B", // 145 (0x91)
			"I2C", // 146 (0x92)
			"I2S", // 147 (0x93)
			"LCMP", // 148 (0x94)
			"FCMPL", // 149 (0x95)
			"FCMPG", // 150 (0x96)
			"DCMPL", // 151 (0x97)
			"DCMPG", // 152 (0x98)
			"IFEQ", // 153 (0x99)
			"IFNE", // 154 (0x9a)
			"IFLT", // 155 (0x9b)
			"IFGE", // 156 (0x9c)
			"IFGT", // 157 (0x9d)
			"IFLE", // 158 (0x9e)
			"IF_ICMPEQ", // 159 (0x9f)
			"IF_ICMPNE", // 160 (0xa0)
			"IF_ICMPLT", // 161 (0xa1)
			"IF_ICMPGE", // 162 (0xa2)
			"IF_ICMPGT", // 163 (0xa3)
			"IF_ICMPLE", // 164 (0xa4)
			"IF_ACMPEQ", // 165 (0xa5)
			"IF_ACMPNE", // 166 (0xa6)
			"GOTO", // 167 (0xa7)
			"JSR", // 168 (0xa8)
			"RET", // 169 (0xa9)
			"TABLESWITCH", // 170 (0xaa)
			"LOOKUPSWITCH", // 171 (0xab)
			"IRETURN", // 172 (0xac)
			"LRETURN", // 173 (0xad)
			"FRETURN", // 174 (0xae)
			"DRETURN", // 175 (0xaf)
			"ARETURN", // 176 (0xb0)
			"RETURN", // 177 (0xb1)
			"GETSTATIC", // 178 (0xb2)
			"PUTSTATIC", // 179 (0xb3)
			"GETFIELD", // 180 (0xb4)
			"PUTFIELD", // 181 (0xb5)
			"INVOKEVIRTUAL", // 182 (0xb6)
			"INVOKESPECIAL", // 183 (0xb7)
			"INVOKESTATIC", // 184 (0xb8)
			"INVOKEINTERFACE", // 185 (0xb9)
			"INVOKEDYNAMIC", // 186 (0xba)
			"NEW", // 187 (0xbb)
			"NEWARRAY", // 188 (0xbc)
			"ANEWARRAY", // 189 (0xbd)
			"ARRAYLENGTH", // 190 (0xbe)
			"ATHROW", // 191 (0xbf)
			"CHECKCAST", // 192 (0xc0)
			"INSTANCEOF", // 193 (0xc1)
			"MONITORENTER", // 194 (0xc2)
			"MONITOREXIT", // 195 (0xc3)
			"WIDE", // 196 (0xc4)
			"MULTIANEWARRAY", // 197 (0xc5)
			"IFNULL", // 198 (0xc6)
			"IFNONNULL" // 199 (0xc7)
	};

	/**
	 * The names of the {@code operand} values of the {@link
	 * org.objectweb.asm.MethodVisitor#visitIntInsn} method when {@code opcode} is
	 * {@code NEWARRAY}.
	 */
	public static final String[] TYPES = {
			"",
			"",
			"",
			"",
			"T_BOOLEAN",
			"T_CHAR",
			"T_FLOAT",
			"T_DOUBLE",
			"T_BYTE",
			"T_SHORT",
			"T_INT",
			"T_LONG"
	};

	/**
	 * The names of the {@code tag} field values for
	 * {@link org.objectweb.asm.Handle}.
	 */
	public static final String[] HANDLE_TAG = {
			"",
			"H_GETFIELD",
			"H_GETSTATIC",
			"H_PUTFIELD",
			"H_PUTSTATIC",
			"H_INVOKEVIRTUAL",
			"H_INVOKESTATIC",
			"H_INVOKESPECIAL",
			"H_NEWINVOKESPECIAL",
			"H_INVOKEINTERFACE"
	};
}
