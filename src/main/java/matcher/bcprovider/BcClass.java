package matcher.bcprovider;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BcClass extends Annotatable {
	/**
	 * A fully qualified class name has the format of {@code my/package/MyClass$MyInnerClass}.
	 */
	@NotNull
	String getInternalName();

	/**
	 * The class's access flags. See {@link SharedBcAccessFlags} and the respective
	 * bytecode backends' additional flags.
	 */
	int getAccess();

	/**
	 * Classfile version or Dex version.
	 */
	int getVersion();

	/**
	 * The class's fields, first the static ones, then all instance fields.
	 */
	@NotNull
	List<? extends BcField> getFields();

	/**
	 * The class's methods, first all direct ones, then the virtual methods.
	 */
	@NotNull
	List<? extends BcMethod> getMethods();

	@Nullable
	String getSourceFile();

	@Nullable
	String getOuterClass();

	@Nullable
	String getOuterMethod();

	@Nullable
	String getOuterMethodDesc();

	@NotNull
	List<String> getInnerClasses();

	@NotNull
	List<String> getInterfaces();

	@Nullable
	String getSuperName();

	@Nullable
	String getSignature();

	@NotNull
	BcClass getCopy();

	@NotNull
	BcClass getRemappedCopy(BcClassRemapNameProvider renameProvider);
}
