package matcher.ir.impl;

import org.jetbrains.annotations.NotNull;

import matcher.ir.api.instance.extensible.ClassInstance;
import matcher.ir.api.instance.extensible.ClassInstance.ClassInstanceCandidate;
import matcher.ir.api.instance.info.ClassNameInfo;

public class ClassNameInfoImpl implements ClassNameInfo {
	private final ClassInstanceImpl cls;
	private final String internalName;
	private final Lazy<String> internalNameWithDots;

	ClassNameInfoImpl(ClassInstanceImpl owner, String internalName) {
		this.cls = owner;
		this.internalName = internalName;
		internalNameWithDots = Lazy.of(this::computeInternalNameWithDots);
	}

	@Override
	@NotNull
	public ClassInstanceImpl getOwner() {
		return cls;
	}

	private String computeInternalNameWithDots() {
		StringBuilder sb = new StringBuilder(internalName.substring(internalName.lastIndexOf('$') + 1));
		ClassInstance<?, ?, ?, ?, ?> outer = cls;
		ClassInstanceCandidate<?, ?, ?, ?, ?> outerCandidate;

		while ((outerCandidate = outer.getLikeliestOuterClass()) != null) {
			outer = outerCandidate.instance();
			sb.insert(0, '.');
			sb.insert(0, outer.getNameInfo().getName());
		}

		sb.insert(0, getPackageName(true));
		return sb.toString();
	}

	@Override
	@NotNull
	public String getInternalName(boolean dotsForInnerClasses) {
		if (!dotsForInnerClasses || cls.getInnerClassStatus().isInnerClass()) {
			return internalName;
		}

		return internalNameWithDots.get();
	}
}
