package matcher.bcprovider;

public class SharedBcInstructions {
	/** The type of {@link InsnNode} instructions. */
	public static final int NOP = 10000;

	/** The type of {@link IntInsnNode} instructions. */
	public static final int INT = 10001;

	/** The type of {@link VarInsnNode} instructions. */
	public static final int LOCAL_VARIABLE = 10002;

	/** The type of {@link TypeInsnNode} instructions. */
	public static final int TYPE = 10003;

	/** The type of {@link FieldInsnNode} instructions. */
	public static final int FIELD = 10004;

	/** The type of {@link MethodInsnNode} instructions. */
	public static final int INVOKE_METHOD = 10005;

	/** The type of {@link InvokeDynamicInsnNode} instructions. */
	public static final int INVOKE_DYNAMIC = 10006;

	/** The type of {@link JumpInsnNode} instructions. */
	public static final int JUMP = 10007;

	/** The type of {@link LabelNode} "instructions". */
	public static final int LABEL = 10008;

	/** The type of {@link LdcInsnNode} instructions. */
	public static final int LOAD_CONSTANT = 10009;

	/** The type of {@link IincInsnNode} instructions. */
	public static final int INCREMENT_INTEGER = 10010;

	/** The type of {@link TableSwitchInsnNode} instructions. */
	public static final int TABLESWITCH = 10011;

	/** The type of {@link LookupSwitchInsnNode} instructions. */
	public static final int LOOKUPSWITCH = 10012;

	/** The type of {@link MultiANewArrayInsnNode} instructions. */
	public static final int MULTIANEWARRAY = 10013;

	/** The type of {@link FrameNode} "instructions". */
	public static final int FRAME = 10014;

	/** The type of {@link LineNumberNode} "instructions". */
	public static final int LINE = 10015;
}
