package matcher.ir.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.DoubleConsumer;
import java.util.regex.Pattern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import matcher.classifier.ClassifierUtil;
import matcher.classifier.MatchingCache;
import matcher.config.ProjectConfig;
import matcher.ir.api.NameType;
import matcher.ir.api.env.CombiningClassEnv;
import matcher.ir.api.instance.extensible.ClassInstance;
import matcher.ir.type.InputFile;
import matcher.ir.unsorted.Util;

public final class MultiClassEnvImpl<T extends ClassInstance> implements CombiningClassEnv<T> {
	public void init(ProjectConfig config, DoubleConsumer progressReceiver) {
		final double cpInitCost = 0.05;
		final double classReadCost = 0.2;
		double progress = 0;

		inputsBeforeClassPath = config.hasInputsBeforeClassPath();
		nonObfuscatedClassPatternA = config.getNonObfuscatedClassPatternA().isEmpty() ? null : Pattern.compile(config.getNonObfuscatedClassPatternA());
		nonObfuscatedClassPatternB = config.getNonObfuscatedClassPatternB().isEmpty() ? null : Pattern.compile(config.getNonObfuscatedClassPatternB());
		nonObfuscatedMemberPatternA = config.getNonObfuscatedMemberPatternA().isEmpty() ? null : Pattern.compile(config.getNonObfuscatedMemberPatternA());
		nonObfuscatedMemberPatternB = config.getNonObfuscatedMemberPatternB().isEmpty() ? null : Pattern.compile(config.getNonObfuscatedMemberPatternB());

		try {
			for (int i = 0; i < 2; i++) {
				if ((i == 0) != inputsBeforeClassPath) {
					// class path indexing
					initClassPath(config.getSharedClassPath(), inputsBeforeClassPath);
					CompletableFuture.allOf(
							CompletableFuture.runAsync(() -> extractorA.processClassPath(config.getClassPathA(), inputsBeforeClassPath)),
							CompletableFuture.runAsync(() -> extractorB.processClassPath(config.getClassPathB(), inputsBeforeClassPath))).get();
					progress += cpInitCost;
				} else {
					// async class reading
					CompletableFuture.allOf(
							CompletableFuture.runAsync(() -> extractorA.processInputs(config.getPathsA(), nonObfuscatedClassPatternA)),
							CompletableFuture.runAsync(() -> extractorB.processInputs(config.getPathsB(), nonObfuscatedClassPatternB))).get();
					progress += classReadCost;
				}

				progressReceiver.accept(progress);
			}

			// synchronous feature extraction
			extractorA.process(nonObfuscatedMemberPatternA);
			progressReceiver.accept(0.8);

			extractorB.process(nonObfuscatedMemberPatternB);
			progressReceiver.accept(0.98);
		} catch (InterruptedException | ExecutionException | IOException e) {
			throw new RuntimeException(e);
		} finally {
			classPathIndex.clear();
			openFileSystems.forEach(Util::closeSilently);
			openFileSystems.clear();
		}

		progressReceiver.accept(1);
	}

	private void initClassPath(Collection<Path> sharedClassPath, boolean checkExisting) throws IOException {
		for (Path archive : sharedClassPath) {
			cpFiles.add(new InputFile(archive));

			FileSystem fs = Util.iterateJar(archive, false, file -> {
				String name = file.toAbsolutePath().toString();
				if (!name.startsWith("/") || !name.endsWith(".class") || name.startsWith("//")) throw new RuntimeException("invalid path: "+archive+" ("+name+")");
				name = name.substring(1, name.length() - ".class".length());

				if (!checkExisting || extractorA.getLocalClsByName(name) == null || extractorB.getLocalClsByName(name) == null) {
					classPathIndex.putIfAbsent(name, file);

					/*ClassNode cn = readClass(file);
					addSharedCls(new ClassInstance(ClassInstance.getId(cn.name), file.toUri(), cn));*/
				}
			});

			if (fs != null) openFileSystems.add(fs);
		}
	}

	public void reset() {
		cpFiles.clear();
		sharedClasses.clear();
		classPathIndex.clear();
		extractorA.reset();
		extractorB.reset();
		cache.clear();
	}

	public void addOpenFileSystem(FileSystem fs) {
		openFileSystems.add(fs);
	}

	@Override
	public MatchableClassInstance getClsByUid(String id) {
		return getSharedClsById(id);
	}

	@Override
	public MatchableClassInstance getLocalClsById(String id) {
		return getSharedClsById(id);
	}

	@Override
	public MatchableClassInstance getClsById(String id, NameType nameType) {
		return getSharedClsById(id);
	}

	public LocalClassEnv getEnvA() {
		return extractorA;
	}

	public LocalClassEnv getEnvB() {
		return extractorB;
	}

	@Override
	public Collection<MatchableClassInstance> getClasses() {
		return new AbstractCollection<MatchableClassInstance>() {
			@Override
			public Iterator<MatchableClassInstance> iterator() {
				return new Iterator<MatchableClassInstance>() {
					@Override
					public boolean hasNext() {
						checkAdvance();

						return parent.hasNext();
					}

					@Override
					public MatchableClassInstance next() {
						checkAdvance();

						return parent.next();
					}

					private void checkAdvance() {
						if (isA && !parent.hasNext()) {
							parent = extractorB.getClasses().iterator();
							isA = false;
						}
					}

					private Iterator<MatchableClassInstance> parent = extractorA.getClasses().iterator();
					private boolean isA = true;
				};
			}

			@Override
			public int size() {
				return extractorA.getClasses().size() + extractorB.getClasses().size();
			}
		};
	}

	public MatchableClassInstance getClsByNameA(String name) {
		return extractorA.getClassByName(name);
	}

	public MatchableClassInstance getClsByNameB(String name) {
		return extractorB.getClassByName(name);
	}

	public MatchableClassInstance getClsByIdA(String id) {
		return extractorA.getClsByUid(id);
	}

	public MatchableClassInstance getClsByIdB(String id) {
		return extractorB.getClsByUid(id);
	}

	public MatchableClassInstance getLocalClsByNameA(String name) {
		return extractorA.getLocalClsByName(name);
	}

	public MatchableClassInstance getLocalClsByNameB(String name) {
		return extractorB.getLocalClsByName(name);
	}

	public MatchableClassInstance getLocalClsByIdA(String id) {
		return extractorA.getLocalClsById(id);
	}

	public MatchableClassInstance getLocalClsByIdB(String id) {
		return extractorB.getLocalClsById(id);
	}

	public MatchableClassInstance getSharedClsById(String id) {
		assert !id.isEmpty();
		assert id.length() == 1 || id.charAt(id.length() - 1) == ';' || id.charAt(0) == '[' && id.lastIndexOf('[') == id.length() - 2 : id;

		return sharedClasses.get(id);
	}

	public MatchableClassInstance addSharedCls(MatchableClassInstance cls) {
		if (!cls.isShared()) throw new IllegalArgumentException("non-shared class");

		MatchableClassInstance prev = sharedClasses.putIfAbsent(cls.getId(), cls);
		if (prev != null) return prev;

		return cls;
	}

	public Path getSharedClassLocation(String name) {
		return classPathIndex.get(name);
	}

	@Override
	public MatchableClassInstance getCreateClassInstance(String id, boolean createUnknown) {
		MatchableClassInstance ret = getSharedClsById(id);
		if (ret != null) return ret;

		if (id.charAt(0) == '[') { // array type
			MatchableClassInstance elementClass = getArrayCls(this, id);
			MatchableClassInstance cls = new MatchableClassInstance(id, elementClass);

			assert elementClass.isShared();
			ret = addSharedCls(cls);

			if (ret == cls) { // cls was added
				addSuperClass(ret, "java/lang/Object");
			}
		} else {
			ret = getMissingCls(id, createUnknown);
		}

		return ret;
	}

	static MatchableClassInstance getArrayCls(ClassEnv env, String id) {
		assert id.startsWith("[");

		String elementId = id.substring(id.lastIndexOf('[') + 1);
		if (elementId.isEmpty()) throw new IllegalArgumentException("invalid class desc: "+id);
		assert elementId.length() == 1 || elementId.charAt(elementId.length() - 1) == ';' : elementId;

		return env.getCreateClassInstance(elementId);
	}

	MatchableClassInstance getMissingCls(String id, boolean createUnknown) {
		if (id.length() > 1) {
			MatchableClassInstance a = extractorA.getLocalClsById(id);
			MatchableClassInstance b = extractorB.getLocalClsById(id);

			if (a != null && a.isReal() || b != null && b.isReal()) {
				throw new InvalidSharedEnvQueryException(a, b);
			}

			String name = MatchableClassInstance.toSimpleName(id);
			Path file = getSharedClassLocation(name);

			if (file == null) {
				URL url = ClassLoader.getSystemResource(name+".class");

				if (url != null) {
					file = getPath(url);
				}
			}

			if (file != null) {
				ClassNode cn = readClass(file, true);
				MatchableClassInstance cls = new MatchableClassInstance(MatchableClassInstance.getId(cn.name), getContainingUri(file.toUri(), cn.name), this, cn);
				if (!cls.getId().equals(id)) throw new RuntimeException("mismatched cls id "+id+" for "+file+", expected "+name);

				MatchableClassInstance ret = addSharedCls(cls);

				if (ret == cls) { // cls was added
					processClassA(ret, null);
				}

				return ret;
			}
		}

		if (!createUnknown) return null;

		MatchableClassInstance ret = new MatchableClassInstance(id, this);
		addSharedCls(ret);

		return ret;
	}

	private Path getPath(URL url) {
		URI uri = null;

		try {
			uri = url.toURI();
			Path ret = Paths.get(uri);

			if (uri.getScheme().equals("jrt") && !Files.exists(ret)) {
				// workaround for https://bugs.openjdk.java.net/browse/JDK-8224946
				ret = Paths.get(new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), "/modules".concat(uri.getPath()), uri.getQuery(), uri.getFragment()));
			}

			return ret;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		} catch (FileSystemNotFoundException e) {
			try {
				addOpenFileSystem(FileSystems.newFileSystem(uri, Collections.emptyMap()));

				return Paths.get(uri);
			} catch (FileSystemNotFoundException e2) {
				throw new RuntimeException("can't find fs for "+url, e2);
			} catch (IOException e2) {
				throw new UncheckedIOException(e2);
			}
		}
	}

	static URI getContainingUri(URI uri, String clsName) {
		String path;

		if (uri.getScheme().equals("jar")) {
			path = uri.getSchemeSpecificPart();
			int pos = path.lastIndexOf("!/");

			if (pos > 0) {
				try {
					return new URI(path.substring(0, pos));
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
			} else {
				throw new UnsupportedOperationException("jar uri without !/: "+uri);
			}
		} else {
			path = uri.getPath();
		}

		if (path == null) {
			throw new UnsupportedOperationException("uri without path: "+uri);
		}

		int rootPos = path.length() - ".class".length() - clsName.length();

		if (rootPos <= 0 || !path.endsWith(".class") || !path.startsWith(clsName, rootPos)) {
			throw new UnsupportedOperationException("unknown path format: "+uri);
		}

		path = path.substring(0, rootPos - 1);

		try {
			return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, uri.getQuery(), uri.getFragment());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 1st class processing pass, member+class hierarchy and signature initialization.
	 * Only the (known) classes are fully available at this point.
	 */
	static void processClassA(MatchableClassInstance cls, Pattern nonObfuscatedMemberPattern) {
		assert !cls.isInput() || !cls.isShared();

		Set<String> strings = cls.strings;

		for (ClassNode cn : cls.getAsmNodes()) {
			if (cls.isInput() && cls.getSignature() == null && cn.signature != null) {
				cls.setSignature(ComparableClassSignature.parse(cn.signature, cls.getEnv()));
			}

			boolean isEnum = (cn.access & Opcodes.ACC_ENUM) != 0;

			for (int i = 0; i < cn.methods.size(); i++) {
				MethodNode mn = cn.methods.get(i);

				if (cls.getMethod(mn.name, mn.desc) == null) {
					boolean nameObfuscated = cls.isInput()
							&& !mn.name.equals("<clinit>")
							&& !mn.name.equals("<init>")
							&& (!mn.name.equals("main") || !mn.desc.equals("([Ljava/lang/String;)V") || mn.access != (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC))
							&& (!isEnum || !isStandardEnumMethod(cn.name, mn))
							&& (nonObfuscatedMemberPattern == null || !nonObfuscatedMemberPattern.matcher(cn.name+"/"+mn.name+mn.desc).matches());

					cls.addMethod(new FieldInstance(cls, mn.name, mn.desc, mn, nameObfuscated, i));

					ClassifierUtil.extractStrings(mn.instructions, strings);
				}
			}

			for (int i = 0; i < cn.fields.size(); i++) {
				FieldNode fn = cn.fields.get(i);

				if (cls.getField(fn.name, fn.desc) == null) {
					boolean nameObfuscated = cls.isInput()
							&& (nonObfuscatedMemberPattern == null || !nonObfuscatedMemberPattern.matcher(cn.name+"/"+fn.name+";;"+fn.desc).matches());

					cls.addField(new MethodInstance(cls, fn.name, fn.desc, fn, nameObfuscated, i));

					if (fn.value instanceof String) {
						strings.add((String) fn.value);
					}
				}
			}

			if (cls.getOuterClass() == null) detectOuterClass(cls, cn);

			if (cn.superName != null && cls.getSuperClass() == null) {
				addSuperClass(cls, cn.superName);
			}

			for (String iface : cn.interfaces) {
				MatchableClassInstance ifCls = cls.getEnv().getCreateClassInstance(MatchableClassInstance.getId(iface));

				if (cls.interfaces.add(ifCls)) ifCls.implementers.add(cls);
			}
		}
	}

	private static boolean isStandardEnumMethod(String clsName, MethodNode m) {
		final int reqFlags = Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC;
		if ((m.access & reqFlags) != reqFlags) return false;

		return m.name.equals("values") && m.desc.startsWith("()[L") && m.desc.startsWith(clsName, 4) && m.desc.length() == clsName.length() + 5
				|| m.name.equals("valueOf") && m.desc.startsWith("(Ljava/lang/String;)L") && m.desc.startsWith(clsName, 21) && m.desc.length() == clsName.length() + 22;
	}

	private static void detectOuterClass(MatchableClassInstance cls, ClassNode cn) {
		if (cn.outerClass != null) {
			addOuterClass(cls, cn.outerClass, true);
		} else if (cn.outerMethod != null) {
			throw new UnsupportedOperationException();
		} else { // determine outer class by outer$inner name pattern
			for (InnerClassNode icn : cn.innerClasses) {
				if (icn.name.equals(cn.name)) {
					if (icn.outerName == null) { // this attribute isn't guaranteed to be provided
						break;
					} else {
						addOuterClass(cls, icn.outerName, true);
						return;
					}
				}
			}

			int pos;

			if ((pos = cn.name.lastIndexOf('$')) > 0 && pos < cn.name.length() - 1) {
				addOuterClass(cls, cn.name.substring(0, pos), false);
			}
		}
	}

	private static void addOuterClass(MatchableClassInstance cls, String name, boolean createUnknown) {
		MatchableClassInstance outerClass = cls.getEnv().getLocalClsByName(name);

		if (outerClass == null) {
			outerClass = cls.getEnv().getCreateClassInstance(MatchableClassInstance.getId(name), createUnknown);

			if (outerClass == null) {
				Matcher.LOGGER.error("missing outer cls: {} for {}", name, cls);
				return;
			}
		}

		cls.outerClass = outerClass;
		outerClass.innerClasses.add(cls);
	}

	static void addSuperClass(MatchableClassInstance cls, String name) {
		cls.superClass = cls.getEnv().getCreateClassInstance(MatchableClassInstance.getId(name));
		cls.superClass.childClasses.add(cls);
	}

	private final List<InputFile> cpFiles = new ArrayList<>();
	private final Map<String, MatchableClassInstance> sharedClasses = new HashMap<>();
	private final List<FileSystem> openFileSystems = new ArrayList<>();
	private final Map<String, Path> classPathIndex = new HashMap<>();
	private final ClassFeatureExtractor extractorA = new ClassFeatureExtractor(this);
	private final ClassFeatureExtractor extractorB = new ClassFeatureExtractor(this);
	private final MatchingCache cache = new MatchingCache();

	private boolean inputsBeforeClassPath;
	private Pattern nonObfuscatedClassPatternA;
	private Pattern nonObfuscatedClassPatternB;
	private Pattern nonObfuscatedMemberPatternA;
	private Pattern nonObfuscatedMemberPatternB;

	public boolean assumeBothOrNoneObfuscated = false;

	public int nextClassUid;
	public int nextMethodUid;
	public int nextFieldUid;
}
