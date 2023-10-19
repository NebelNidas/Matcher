package matcher.ir.api.instance.info;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class ClassNames {
	@Nullable
	public static String toFullyQualifiedName(String internalName) {
		return internalName == null ? null : internalName.replace('/', '.');
	}

	@Nullable
	public static String toContextualName(String internalName) {
		if (internalName == null) return null;
		int packagePos = internalName.lastIndexOf('/');

		if (packagePos > 0) {
			return internalName.substring(packagePos + 1);
		}

		return internalName;
	}

	@Nullable
	public static String toSimpleName(String internalNameWithDotsForInners) {
		if (internalNameWithDotsForInners == null) return null;
		int innerClassPos = internalNameWithDotsForInners.lastIndexOf('.');

		if (innerClassPos > 0) {
			return internalNameWithDotsForInners.substring(innerClassPos + 1);
		}

		return internalNameWithDotsForInners;
	}

	@Nullable
	public static String toDescriptor(String internalName) {
		if (internalName == null) return null;
		assert internalName.charAt(internalName.length() - 1) != ';' || internalName.charAt(0) == '[' : internalName;

		if (internalName.charAt(0) == '[') {
			assert internalName.charAt(internalName.length() - 1) == ';' || internalName.lastIndexOf('[') == internalName.length() - 2;
			return internalName;
		}

		return "L" + internalName + ";";
	}

	@Nullable
	public static String getPackageName(String internalName, boolean slashesAsSeparator) {
		if (internalName == null) return null;
		int packagePos = internalName.lastIndexOf('/');

		if (packagePos > 0) {
			internalName = internalName.substring(0, packagePos);

			return slashesAsSeparator ? internalName : internalName.replace('/', '.');
		}

		return null;
	}
}
