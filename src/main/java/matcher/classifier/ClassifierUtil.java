package matcher.classifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import matcher.Util;
import matcher.bcprovider.BcInstruction;
import matcher.bcprovider.BcMethod;
import matcher.bcprovider.SharedBcInstructions;
import matcher.bcprovider.instructions.BcFieldInstruction;
import matcher.bcprovider.instructions.BcIntInstruction;
import matcher.bcprovider.instructions.BcInvokeDynamicInstruction;
import matcher.bcprovider.instructions.BcInvokeMethodInstruction;
import matcher.bcprovider.instructions.BcJumpInstruction;
import matcher.bcprovider.instructions.BcLoadConstantInstruction;
import matcher.bcprovider.instructions.BcTypeInstruction;
import matcher.classifier.MatchingCache.CacheToken;
import matcher.type.ClassEnvironment;
import matcher.type.ClassInstance;
import matcher.type.FieldInstance;
import matcher.type.Matchable;
import matcher.type.MemberInstance;
import matcher.type.MethodInstance;
import matcher.type.MethodType;
import matcher.type.MethodVarInstance;

public class ClassifierUtil {
	public static boolean checkPotentialEquality(ClassInstance a, ClassInstance b) {
		if (a == b) return true;
		if (a.getMatch() != null) return a.getMatch() == b;
		if (b.getMatch() != null) return b.getMatch() == a;
		if (!a.isMatchable() || !b.isMatchable()) return false;
		if (a.isArray() != b.isArray()) return false;
		if (a.isArray() && !checkPotentialEquality(a.getElementClass(), b.getElementClass())) return false;
		if (!checkNameObfMatch(a, b)) return false;

		return true;
	}

	private static boolean checkNameObfMatch(Matchable<?> a, Matchable<?> b) {
		boolean nameObfA = a.isNameObfuscated();
		boolean nameObfB = b.isNameObfuscated();

		if (nameObfA && nameObfB) { // both obf
			return true;
		} else if (nameObfA != nameObfB) { // one obf
			return !a.getEnv().getGlobal().assumeBothOrNoneObfuscated;
		} else { // neither obf
			return a.getName().equals(b.getName());
		}
	}

	public static boolean checkPotentialEquality(MemberInstance<?> a, MemberInstance<?> b) {
		if (a instanceof MethodInstance) {
			return checkPotentialEquality((MethodInstance) a, (MethodInstance) b);
		} else {
			return checkPotentialEquality((FieldInstance) a, (FieldInstance) b);
		}
	}

	public static boolean checkPotentialEquality(MethodInstance a, MethodInstance b) {
		if (a == b) return true;
		if (a.getMatch() != null) return a.getMatch() == b;
		if (b.getMatch() != null) return b.getMatch() == a;
		if (!a.isMatchable() || !b.isMatchable()) return false;
		if (!checkPotentialEquality(a.getCls(), b.getCls())) return false;
		if (!checkNameObfMatch(a, b)) return false;
		if ((a.getId().startsWith("<") || b.getId().startsWith("<")) && !a.getName().equals(b.getName())) return false; // require <clinit> and <init> to match

		//MethodInstance hierarchyMatch = a.getHierarchyMatch();
		//if (hierarchyMatch != null && !hierarchyMatch.getAllHierarchyMembers().contains(b)) return false;
		if ((a.hasHierarchyMatch() || b.hasHierarchyMatch()) && !a.hasMatchedHierarchy(b)) return false;

		if (a.getType() == MethodType.LAMBDA_IMPL && b.getType() == MethodType.LAMBDA_IMPL) { // require same "outer method" for lambdas
			boolean found = false;

			maLoop: for (MethodInstance ma : a.getRefsIn()) {
				for (MethodInstance mb : b.getRefsIn()) {
					if (checkPotentialEquality(ma, mb)) {
						found = true;
						break maLoop;
					}
				}
			}

			if (!found) return false;
		}

		return true;
	}

	public static boolean checkPotentialEquality(FieldInstance a, FieldInstance b) {
		if (a == b) return true;
		if (a.getMatch() != null) return a.getMatch() == b;
		if (b.getMatch() != null) return b.getMatch() == a;
		if (!a.isMatchable() || !b.isMatchable()) return false;
		if (!checkPotentialEquality(a.getCls(), b.getCls())) return false;
		if (!checkNameObfMatch(a, b)) return false;

		return true;
	}

	public static boolean checkPotentialEquality(MethodVarInstance a, MethodVarInstance b) {
		if (a == b) return true;
		if (a.getMatch() != null) return a.getMatch() == b;
		if (b.getMatch() != null) return b.getMatch() == a;
		if (!a.isMatchable() || !b.isMatchable()) return false;
		if (a.isArg() != b.isArg()) return false;
		if (!checkPotentialEquality(a.getMethod(), b.getMethod())) return false;
		if (!checkNameObfMatch(a, b)) return false;

		return true;
	}

	public static boolean checkPotentialEqualityNullable(ClassInstance a, ClassInstance b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;

		return checkPotentialEquality(a, b);
	}

	public static boolean checkPotentialEqualityNullable(MethodInstance a, MethodInstance b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;

		return checkPotentialEquality(a, b);
	}

	public static boolean checkPotentialEqualityNullable(FieldInstance a, FieldInstance b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;

		return checkPotentialEquality(a, b);
	}

	public static boolean checkPotentialEqualityNullable(MethodVarInstance a, MethodVarInstance b) {
		if (a == null && b == null) return true;
		if (a == null || b == null) return false;

		return checkPotentialEquality(a, b);
	}

	public static double compareCounts(int countA, int countB) {
		int delta = Math.abs(countA - countB);
		if (delta == 0) return 1;

		return 1 - (double) delta / Math.max(countA, countB);
	}

	public static <T> double compareSets(Set<T> setA, Set<T> setB, boolean readOnly) {
		if (readOnly) setB = Util.copySet(setB);

		int oldSize = setB.size();
		setB.removeAll(setA);

		int matched = oldSize - setB.size();
		int total = setA.size() - matched + oldSize;

		return total == 0 ? 1 : (double) matched / total;
	}

	public static double compareClassSets(Set<ClassInstance> setA, Set<ClassInstance> setB, boolean readOnly) {
		return compareIdentitySets(setA, setB, readOnly, ClassifierUtil::checkPotentialEquality);
	}

	public static double compareMethodSets(Set<MethodInstance> setA, Set<MethodInstance> setB, boolean readOnly) {
		return compareIdentitySets(setA, setB, readOnly, ClassifierUtil::checkPotentialEquality);
	}

	public static double compareFieldSets(Set<FieldInstance> setA, Set<FieldInstance> setB, boolean readOnly) {
		return compareIdentitySets(setA, setB, readOnly, ClassifierUtil::checkPotentialEquality);
	}

	private static <T extends Matchable<T>> double compareIdentitySets(Set<T> setA, Set<T> setB, boolean readOnly, BiPredicate<T, T> comparator) {
		if (setA.isEmpty() || setB.isEmpty()) {
			return setA.isEmpty() && setB.isEmpty() ? 1 : 0;
		}

		if (readOnly) {
			setA = Util.newIdentityHashSet(setA);
			setB = Util.newIdentityHashSet(setB);
		}

		final int total = setA.size() + setB.size();
		final boolean assumeBothOrNoneObfuscated = setA.iterator().next().getEnv().getGlobal().assumeBothOrNoneObfuscated;
		int unmatched = 0;

		// precise matches, nameObfuscated a
		for (Iterator<T> it = setA.iterator(); it.hasNext(); ) {
			T a = it.next();

			if (setB.remove(a)) {
				it.remove();
			} else if (a.getMatch() != null) {
				if (!setB.remove(a.getMatch())) {
					unmatched++;
				}

				it.remove();
			} else if (assumeBothOrNoneObfuscated && !a.isNameObfuscated()) {
				unmatched++;
				it.remove();
			}
		}

		// nameObfuscated b
		if (assumeBothOrNoneObfuscated) {
			for (Iterator<T> it = setB.iterator(); it.hasNext(); ) {
				T b = it.next();

				if (!b.isNameObfuscated()) {
					unmatched++;
					it.remove();
				}
			}
		}

		for (Iterator<T> it = setA.iterator(); it.hasNext(); ) {
			T a = it.next();

			assert a.getMatch() == null && (!assumeBothOrNoneObfuscated || a.isNameObfuscated());
			boolean found = false;

			for (T b : setB) {
				if (comparator.test(a, b)) {
					found = true;
					break;
				}
			}

			if (!found) {
				unmatched++;
				it.remove();
			}
		}

		for (T b : setB) {
			boolean found = false;

			for (T a : setA) {
				if (comparator.test(a, b)) {
					found = true;
					break;
				}
			}

			if (!found) {
				unmatched++;
			}
		}

		assert unmatched <= total;

		return (double) (total - unmatched) / total;
	}

	public static double compareClassLists(List<ClassInstance> listA, List<ClassInstance> listB) {
		return compareLists(listA, listB, (a, b) -> ClassifierUtil.checkPotentialEquality(a, b) ? COMPARED_SIMILAR : COMPARED_DISTINCT);
	}

	public static double compareInsns(MethodInstance a, MethodInstance b) {
		if (a.getBcMethod() == null || b.getBcMethod() == null) return 1;

		List<BcInstruction> ilA = a.getBcMethod().getInstructions();
		List<BcInstruction> ilB = b.getBcMethod().getInstructions();

		return compareLists(ilA, ilB, (inA, inB) -> compareInsns(inA, inB, ilA, ilB, (list, item) -> list.indexOf(item), a, b, a.getEnv().getGlobal()));
	}

	public static double compareInsns(List<BcInstruction> listA, List<BcInstruction> listB, ClassEnvironment env) {
		return compareLists(listA, listB, (inA, inB) -> compareInsns(inA, inB, listA, listB, (list, item) -> list.indexOf(item), null, null, env));
	}

	private static <T> int compareInsns(BcInstruction insnA, BcInstruction insnB, List<T> listA, List<T> listB, ToIntBiFunction<List<T>, BcInstruction> posProvider,
			MethodInstance mthA, MethodInstance mthB, ClassEnvironment env) {
		if (insnA.getOpcode() != insnB.getOpcode()) return COMPARED_DISTINCT;

		switch (insnA.getInstructionType()) {
		case SharedBcInstructions.INT: {
			BcIntInstruction a = (BcIntInstruction) insnA;
			BcIntInstruction b = (BcIntInstruction) insnB;

			return a.getOperand() == b.getOperand() ? COMPARED_SIMILAR : COMPARED_DISTINCT;
		}
		case SharedBcInstructions.LOCAL_VARIABLE: {
			VarInsnNode a = (VarInsnNode) insnA;
			VarInsnNode b = (VarInsnNode) insnB;

			if (mthA != null && mthB != null) {
				MethodVarInstance varA = mthA.getArgOrVar(a.var, posProvider.applyAsInt(listA, insnA));
				MethodVarInstance varB = mthB.getArgOrVar(b.var, posProvider.applyAsInt(listB, insnB));

				if (varA != null && varB != null) {
					if (!checkPotentialEquality(varA, varB)) {
						return COMPARED_DISTINCT;
					} else {
						return checkPotentialEquality(varA.getType(), varB.getType()) ? COMPARED_SIMILAR : COMPARED_POSSIBLE;
					}
				}
			}

			break;
		}
		case SharedBcInstructions.TYPE: {
			BcTypeInstruction a = (BcTypeInstruction) insnA;
			BcTypeInstruction b = (BcTypeInstruction) insnB;
			ClassInstance clsA = env.getClsByNameA(a.getType());
			ClassInstance clsB = env.getClsByNameB(b.getType());

			return checkPotentialEqualityNullable(clsA, clsB) ? COMPARED_SIMILAR : COMPARED_DISTINCT;
		}
		case SharedBcInstructions.FIELD: {
			BcFieldInstruction a = (BcFieldInstruction) insnA;
			BcFieldInstruction b = (BcFieldInstruction) insnB;
			ClassInstance clsA = env.getClsByNameA(a.getOwner());
			ClassInstance clsB = env.getClsByNameB(b.getOwner());

			if (clsA == null && clsB == null) return COMPARED_SIMILAR;
			if (clsA == null || clsB == null) return COMPARED_DISTINCT;

			FieldInstance fieldA = clsA.resolveField(a.getName(), a.getDescriptor());
			FieldInstance fieldB = clsB.resolveField(b.getName(), b.getDescriptor());

			return checkPotentialEqualityNullable(fieldA, fieldB) ? COMPARED_SIMILAR : COMPARED_DISTINCT;
		}
		case SharedBcInstructions.INVOKE_METHOD: {
			BcInvokeMethodInstruction a = (BcInvokeMethodInstruction) insnA;
			BcInvokeMethodInstruction b = (BcInvokeMethodInstruction) insnB;

			return compareMethods(a.getOwner(), a.getName(), a.getDescriptor(), Util.isCallToInterface(a),
					b.getOwner(), b.getName(), b.getDescriptor(), Util.isCallToInterface(b),
					env) ? COMPARED_SIMILAR : COMPARED_DISTINCT;
		}
		case SharedBcInstructions.INVOKE_DYNAMIC: {
			BcInvokeDynamicInstruction a = (BcInvokeDynamicInstruction) insnA;
			BcInvokeDynamicInstruction b = (BcInvokeDynamicInstruction) insnB;
			Handle bsmA = a.getBootstrapMethodHandle();
			Handle bsmB = b.getBootstrapMethodHandle();

			if (!bsmA.equals(bsmB)) return COMPARED_DISTINCT;

			if (Util.isJavaLambdaMetafactory(bsmA)) {
				Handle implA = (Handle) a.getBootstrapMethodArgs().get(1);
				Handle implB = (Handle) b.getBootstrapMethodArgs().get(1);

				if (implA.getTag() != implB.getTag()) return COMPARED_DISTINCT;

				switch (implA.getTag()) {
				case Opcodes.H_INVOKEVIRTUAL:
				case Opcodes.H_INVOKESTATIC:
				case Opcodes.H_INVOKESPECIAL:
				case Opcodes.H_NEWINVOKESPECIAL:
				case Opcodes.H_INVOKEINTERFACE:
					return compareMethods(implA.getOwner(), implA.getName(), implA.getDesc(), Util.isCallToInterface(implA),
							implB.getOwner(), implB.getName(), implB.getDesc(), Util.isCallToInterface(implB),
							env) ? COMPARED_SIMILAR : COMPARED_DISTINCT;
				default:
					System.out.println("unexpected impl tag: "+implA.getTag());
				}
			} else if (!Util.isIrrelevantBsm(bsmA)) {
				System.out.printf("unknown invokedynamic bsm: %s/%s%s (tag=%d iif=%b)%n", bsmA.getOwner(), bsmA.getName(), bsmA.getDesc(), bsmA.getTag(), bsmA.isInterface());
			}

			// TODO: implement
			break;
		}
		case SharedBcInstructions.JUMP: {
			BcJumpInstruction a = (BcJumpInstruction) insnA;
			BcJumpInstruction b = (BcJumpInstruction) insnB;

			// check if the 2 jumps have the same direction
			int dirA = Integer.signum(posProvider.applyAsInt(listA, a.getLabel()) - posProvider.applyAsInt(listA, a));
			int dirB = Integer.signum(posProvider.applyAsInt(listB, b.getLabel()) - posProvider.applyAsInt(listB, b));

			return dirA == dirB ? COMPARED_SIMILAR : COMPARED_DISTINCT;
		}
		case SharedBcInstructions.LABEL: {
			// TODO: implement
			break;
		}
		case SharedBcInstructions.LOAD_CONSTANT: {
			BcLoadConstantInstruction a = (BcLoadConstantInstruction) insnA;
			BcLoadConstantInstruction b = (BcLoadConstantInstruction) insnB;
			Class<?> typeClsA = a.getConstant().getClass();

			if (typeClsA != b.getConstant().getClass()) return COMPARED_DISTINCT;

			if (typeClsA == Type.class) {
				Type typeA = (Type) a.getConstant();
				Type typeB = (Type) b.getConstant();

				if (typeA.getSort() != typeB.getSort()) return COMPARED_DISTINCT;

				switch (typeA.getSort()) {
				case Type.ARRAY:
				case Type.OBJECT:
					return checkPotentialEqualityNullable(env.getClsByIdA(typeA.getDescriptor()), env.getClsByIdB(typeB.getDescriptor())) ? COMPARED_SIMILAR : COMPARED_DISTINCT;
				case Type.METHOD:
					// TODO: implement
					break;
				}
			} else {
				return a.getConstant().equals(b.getConstant()) ? COMPARED_SIMILAR : COMPARED_DISTINCT;
			}

			break;
		}
		case AbstractInsnNode.IINC_INSN: {
			IincInsnNode a = (IincInsnNode) insnA;
			IincInsnNode b = (IincInsnNode) insnB;

			if (a.incr != b.incr) return COMPARED_DISTINCT;

			if (mthA != null && mthB != null) {
				MethodVarInstance varA = mthA.getArgOrVar(a.var, posProvider.applyAsInt(listA, insnA));
				MethodVarInstance varB = mthB.getArgOrVar(b.var, posProvider.applyAsInt(listB, insnB));

				if (varA != null && varB != null) {
					return checkPotentialEquality(varA, varB) ? COMPARED_SIMILAR : COMPARED_DISTINCT;
				}
			}

			break;
		}
		case AbstractInsnNode.TABLESWITCH_INSN: {
			TableSwitchInsnNode a = (TableSwitchInsnNode) insnA;
			TableSwitchInsnNode b = (TableSwitchInsnNode) insnB;

			return a.min == b.min && a.max == b.max ? COMPARED_SIMILAR : COMPARED_DISTINCT;
		}
		case AbstractInsnNode.LOOKUPSWITCH_INSN: {
			LookupSwitchInsnNode a = (LookupSwitchInsnNode) insnA;
			LookupSwitchInsnNode b = (LookupSwitchInsnNode) insnB;

			return a.keys.equals(b.keys) ? COMPARED_SIMILAR : COMPARED_DISTINCT;
		}
		case AbstractInsnNode.MULTIANEWARRAY_INSN: {
			MultiANewArrayInsnNode a = (MultiANewArrayInsnNode) insnA;
			MultiANewArrayInsnNode b = (MultiANewArrayInsnNode) insnB;

			if (a.dims != b.dims) return COMPARED_DISTINCT;

			ClassInstance clsA = env.getClsByNameA(a.desc);
			ClassInstance clsB = env.getClsByNameB(b.desc);

			return checkPotentialEqualityNullable(clsA, clsB) ? COMPARED_SIMILAR : COMPARED_DISTINCT;
		}
		case AbstractInsnNode.FRAME: {
			// TODO: implement
			break;
		}
		case AbstractInsnNode.LINE: {
			// TODO: implement
			break;
		}
		}

		return COMPARED_SIMILAR;
	}

	private static boolean compareMethods(String ownerA, String nameA, String descA, boolean toIfA, String ownerB, String nameB, String descB, boolean toIfB, ClassEnvironment env) {
		ClassInstance clsA = env.getClsByNameA(ownerA);
		ClassInstance clsB = env.getClsByNameB(ownerB);

		if (clsA == null && clsB == null) return true;
		if (clsA == null || clsB == null) return false;

		return compareMethods(clsA, nameA, descA, toIfA, clsB, nameB, descB, toIfB);
	}

	private static boolean compareMethods(ClassInstance ownerA, String nameA, String descA, boolean toIfA, ClassInstance ownerB, String nameB, String descB, boolean toIfB) {
		MethodInstance methodA = ownerA.resolveMethod(nameA, descA, toIfA);
		MethodInstance methodB = ownerB.resolveMethod(nameB, descB, toIfB);

		if (methodA == null && methodB == null) return true;
		if (methodA == null || methodB == null) return false;

		return checkPotentialEquality(methodA, methodB);
	}

	private static <T> double compareLists(List<T> listA, List<T> listB, ElementComparator<T> elementComparator) {
		final int sizeA = listA.size();
		final int sizeB = listB.size();

		if (sizeA == 0 && sizeB == 0) return 1;
		if (sizeA == 0 || sizeB == 0) return 0;

		if (sizeA == sizeB) {
			boolean match = true;

			for (int i = 0; i < sizeA; i++) {
				if (elementComparator.compare(listA.get(i), listB.get(i)) != COMPARED_SIMILAR) {
					match = false;
					break;
				}
			}

			if (match) return 1;
		}

		// levenshtein distance as per wp (https://en.wikipedia.org/wiki/Levenshtein_distance#Iterative_with_two_matrix_rows)
		int[] v0 = new int[sizeB + 1];
		int[] v1 = new int[sizeB + 1];

		for (int i = 1; i < v0.length; i++) {
			v0[i] = i * COMPARED_DISTINCT;
		}

		for (int i = 0; i < sizeA; i++) {
			v1[0] = (i + 1) * COMPARED_DISTINCT;

			for (int j = 0; j < sizeB; j++) {
				int cost = elementComparator.compare(listA.get(i), listB.get(i));
				v1[j + 1] = Math.min(Math.min(v1[j] + COMPARED_DISTINCT, v0[j + 1] + COMPARED_DISTINCT), v0[j] + cost);
			}

			for (int j = 0; j < v0.length; j++) {
				v0[j] = v1[j];
			}
		}

		int distance = v1[sizeB];
		int upperBound = Math.max(sizeA, sizeB) * COMPARED_DISTINCT;
		assert distance >= 0 && distance <= upperBound;

		return 1 - (double) distance / upperBound;
	}

	public static int[] mapInsns(MethodInstance a, MethodInstance b) {
		if (a.getBcMethod() == null || b.getBcMethod() == null) return null;

		List<BcInstruction> ilA = a.getBcMethod().getInstructions();
		List<BcInstruction> ilB = b.getBcMethod().getInstructions();

		if (ilA.size() * ilB.size() < 1000) {
			return mapInsns(ilA, ilB, a, b, a.getEnv().getGlobal());
		} else {
			return a.getEnv().getGlobal().getCache().compute(ilMapCacheToken, a, b, (mA, mB) -> mapInsns(mA.getBcMethod().getInstructions(), mB.getBcMethod().getInstructions(), mA, mB, mA.getEnv().getGlobal()));
		}
	}

	public static int[] mapInsns(List<BcInstruction> listA, List<BcInstruction> listB, MethodInstance mthA, MethodInstance mthB, ClassEnvironment env) {
		return mapLists(listA, listB, (inA, inB) -> compareInsns(inA, inB, listA, listB, (list, item) -> list.indexOf(item), mthA, mthB, env));
	}

	private static <T> int[] mapLists(List<T> listA, List<T> listB, ElementComparator<T> elementComparator) {
		final int sizeA = listA.size();
		final int sizeB = listB.size();

		if (sizeA == 0 && sizeB == 0) return new int[0];

		final int[] ret = new int[sizeA];

		if (sizeA == 0 || sizeB == 0) {
			Arrays.fill(ret, -1);

			return ret;
		}

		if (sizeA == sizeB) {
			boolean match = true;

			for (int i = 0; i < sizeA; i++) {
				if (elementComparator.compare(listA.get(i), listB.get(i)) != COMPARED_SIMILAR) {
					match = false;
					break;
				}
			}

			if (match) {
				for (int i = 0; i < ret.length; i++) {
					ret[i] = i;
				}

				return ret;
			}
		}

		// levenshtein distance as per wp (https://en.wikipedia.org/wiki/Levenshtein_distance#Iterative_with_two_matrix_rows)
		int size = sizeA + 1;
		int[] v = new int[size * (sizeB + 1)];

		for (int i = 1; i <= sizeA; i++) {
			v[i + 0] = i * COMPARED_DISTINCT;
		}

		for (int j = 1; j <= sizeB; j++) {
			v[0 + j * size] = j * COMPARED_DISTINCT;
		}

		for (int j = 1; j <= sizeB; j++) {
			for (int i = 1; i <= sizeA; i++) {
				int cost = elementComparator.compare(listA.get(i-1), listB.get(j-1));

				v[i + j * size] = Math.min(Math.min(v[i - 1 + j * size] + COMPARED_DISTINCT,
						v[i + (j - 1) * size] + COMPARED_DISTINCT),
						v[i - 1 + (j - 1) * size] + cost);
			}
		}

		/*for (int j = 0; j <= sizeB; j++) {
			for (int i = 0; i <= sizeA; i++) {
				System.out.printf("%2d ", v[i + j * size]);
			}

			System.out.println();
		}*/

		int i = sizeA;
		int j = sizeB;
		//boolean valid = true;

		while (i > 0 || j > 0) {
			int c = v[i + j * size];
			int delCost = i > 0 ? v[i - 1 + j * size] : Integer.MAX_VALUE;
			int insCost = j > 0 ? v[i + (j - 1) * size] : Integer.MAX_VALUE;
			int keepCost = j > 0 && i > 0 ? v[i - 1 + (j - 1) * size] : Integer.MAX_VALUE;

			if (keepCost <= delCost && keepCost <= insCost) {
				if (c - keepCost >= COMPARED_DISTINCT) {
					assert c - keepCost == COMPARED_DISTINCT;
					//System.out.printf("%d/%d rep %s -> %s%n", i-1, j-1, toString(elementRetriever.apply(listA, i - 1)), toString(elementRetriever.apply(listB, j - 1)));
					ret[i - 1] = -1;
				} else {
					//System.out.printf("%d/%d eq %s - %s%n", i-1, j-1, toString(elementRetriever.apply(listA, i - 1)), toString(elementRetriever.apply(listB, j - 1)));
					ret[i - 1] = j - 1;

					/*U e = elementRetriever.apply(listA, i - 1);

					if (e instanceof AbstractInsnNode
							&& ((AbstractInsnNode) e).getOpcode() != ((AbstractInsnNode) elementRetriever.apply(listB, j - 1)).getOpcode()) {
						valid = false;
					}*/
				}

				i--;
				j--;
			} else if (delCost < insCost) {
				//System.out.printf("%d/%d del %s%n", i-1, j-1, toString(elementRetriever.apply(listA, i - 1)));
				ret[i - 1] = -1;
				i--;
			} else {
				//System.out.printf("%d/%d ins %s%n", i-1, j-1, toString(elementRetriever.apply(listB, j - 1)));
				j--;
			}
		}

		/*if (!valid) {
			assert valid;
		}*/

		return ret;
	}

	public interface ElementComparator<T> {
		int compare(T a, T b);
	}

	public static final int COMPARED_SIMILAR = 0;
	public static final int COMPARED_POSSIBLE = 1;
	public static final int COMPARED_DISTINCT = 2;

	private static String toString(Object node) {
		if (node instanceof AbstractInsnNode) {
			Textifier textifier = new Textifier();
			MethodVisitor visitor = new TraceMethodVisitor(textifier);

			((AbstractInsnNode) node).accept(visitor);

			return textifier.getText().get(0).toString().trim();
		} else {
			return Objects.toString(node);
		}
	}

	public static <T extends Matchable<T>> List<RankResult<T>> rank(T src, T[] dsts, Collection<IClassifier<T>> classifiers, BiPredicate<T, T> potentialEqualityCheck, ClassEnvironment env, double maxMismatch) {
		List<RankResult<T>> ret = new ArrayList<>(dsts.length);

		for (T dst : dsts) {
			RankResult<T> result = rank(src, dst, classifiers, potentialEqualityCheck, env, maxMismatch);
			if (result != null) ret.add(result);
		}

		ret.sort(Comparator.<RankResult<T>, Double>comparing(RankResult::getScore).reversed());

		return ret;
	}

	public static <T extends Matchable<T>> List<RankResult<T>> rankParallel(T src, T[] dsts, Collection<IClassifier<T>> classifiers, BiPredicate<T, T> potentialEqualityCheck, ClassEnvironment env, double maxMismatch) {
		return Arrays.stream(dsts)
				.parallel()
				.map(dst -> rank(src, dst, classifiers, potentialEqualityCheck, env, maxMismatch))
				.filter(Objects::nonNull)
				.sorted(Comparator.<RankResult<T>, Double>comparing(RankResult::getScore).reversed())
				.collect(Collectors.toList());
	}

	private static <T extends Matchable<T>> RankResult<T> rank(T src, T dst, Collection<IClassifier<T>> classifiers, BiPredicate<T, T> potentialEqualityCheck, ClassEnvironment env, double maxMismatch) {
		assert src.getEnv() != dst.getEnv();

		if (!potentialEqualityCheck.test(src, dst)) return null;

		double score = 0;
		double mismatch = 0;
		List<ClassifierResult<T>> results = new ArrayList<>(classifiers.size());

		for (IClassifier<T> classifier : classifiers) {
			double cScore = classifier.getScore(src, dst, env);
			assert cScore > -epsilon && cScore < 1 + epsilon : "invalid score from "+classifier.getName()+": "+cScore;

			double weight = classifier.getWeight();
			double weightedScore = cScore * weight;

			mismatch += weight - weightedScore;
			if (mismatch >= maxMismatch) return null;

			score += weightedScore;
			results.add(new ClassifierResult<>(classifier, cScore));
		}

		return new RankResult<>(dst, score, results);
	}

	public static void extractStrings(Collection<BcInstruction> il, Set<String> out) {
		extractStrings(il.iterator(), out);
	}

	private static void extractStrings(Iterator<BcInstruction> it, Set<String> out) {
		while (it.hasNext()) {
			BcInstruction insn = it.next();

			if (insn instanceof BcLoadConstantInstruction ldcInsn) {
				if (ldcInsn.getConstant() instanceof String) {
					out.add((String) ldcInsn.getConstant());
				}
			}
		}
	}

	public static void extractNumbers(BcMethod bcMethod, Set<Integer> ints, Set<Long> longs, Set<Float> floats, Set<Double> doubles) {
		for (Iterator<BcInstruction> it = bcMethod.getInstructions().iterator(); it.hasNext(); ) {
			BcInstruction insn = it.next();

			if (insn instanceof BcLoadConstantInstruction ldcInsn) {
				handleNumberValue(ldcInsn.getConstant(), ints, longs, floats, doubles);
			} else if (insn instanceof BcIntInstruction intInsn) {
				ints.add(intInsn.getOperand());
			}
		}
	}

	public static void handleNumberValue(Object number, Set<Integer> ints, Set<Long> longs, Set<Float> floats, Set<Double> doubles) {
		if (number == null) return;

		if (number instanceof Integer) {
			ints.add((Integer) number);
		} else if (number instanceof Long) {
			longs.add((Long) number);
		} else if (number instanceof Float) {
			floats.add((Float) number);
		} else if (number instanceof Double) {
			doubles.add((Double) number);
		}
	}

	public static <T extends Matchable<T>> double classifyPosition(T a, T b,
			ToIntFunction<T> positionSupplier,
			BiFunction<T, Integer, T> siblingSupplier,
			Function<T, T[]> siblingsSupplier) {
		int posA = positionSupplier.applyAsInt(a);
		int posB = positionSupplier.applyAsInt(b);
		T[] siblingsA = siblingsSupplier.apply(a);
		T[] siblingsB = siblingsSupplier.apply(b);

		if (posA == posB && siblingsA.length == siblingsB.length) return 1;
		if (posA == -1 || posB == -1) return posA == posB ? 1 : 0;

		// try to find the index range enclosed by other mapped members and compare relative to it
		int startPosA = 0;
		int startPosB = 0;
		int endPosA = siblingsA.length;
		int endPosB = siblingsB.length;

		if (posA > 0) {
			for (int i = posA - 1; i >= 0; i--) {
				T c = siblingSupplier.apply(a, i);
				T match = c.getMatch();

				if (match != null) {
					startPosA = i + 1;
					startPosB = positionSupplier.applyAsInt(match) + 1;
					break;
				}
			}
		}

		if (posA < endPosA - 1) {
			for (int i = posA + 1; i < endPosA; i++) {
				T c = siblingSupplier.apply(a, i);
				T match = c.getMatch();

				if (match != null) {
					endPosA = i;
					endPosB = positionSupplier.applyAsInt(match);
					break;
				}
			}
		}

		if (startPosB >= endPosB || startPosB > posB || endPosB <= posB) {
			startPosA = startPosB = 0;
			endPosA = siblingsA.length;
			endPosB = siblingsB.length;
		}

		double relPosA = getRelativePosition(posA - startPosA, endPosA - startPosA);
		assert relPosA >= 0 && relPosA <= 1;
		double relPosB = getRelativePosition(posB - startPosB, endPosB - startPosB);
		assert relPosB >= 0 && relPosB <= 1;

		return 1 - Math.abs(relPosA - relPosB);
	}

	private static double getRelativePosition(int position, int size) {
		if (size == 1) return 0.5;
		assert size > 1;

		return (double) position / (size - 1);
	}

	private static final double epsilon = 1e-6;

	private static final CacheToken<int[]> ilMapCacheToken = new CacheToken<>();
}
