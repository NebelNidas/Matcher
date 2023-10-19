package matcher.ir.api.instance.info;

import org.jetbrains.annotations.Range;

import matcher.ir.api.env.ClassEnv;

public interface InnerClassStatusInfo {
	/**
	 * @return The probability of the owning class being a subclass, calculated
	 * according to the originating {@link ClassEnv}'s settings.
	 */
	@Range(from = 0, to = 1)
	float getProbability(boolean normalized);

	/**
	 * @return Whether or not the probability is high enough for this instance to
	 * be counted as an inner class, determined by the originating {@link ClassEnv}'s settings.
	 */
	boolean isInnerClass();

	// /**
	//  * Only looks at the internal name of the class and trusts
	//  * dollar signs to be exclusively used for inner class segmentation.
	//  * Many obfuscators break this assumption.
	//  */
	// boolean ofName();

	// /**
	//  * Only looks at the {@code InnerClasses} attribute of the owning class.
	//  */
	// boolean ofOwnAttributes();

	// /**
	//  * Only looks at the {@code InnerClasses} attributes of classes on the classpath,
	//  * and returns whether or not at least one of them has a matching attribute.
	//  */
	// boolean ofClasspathAttributes();

	// /**
	//  * Scans every classpath entry for certain patterns usually associated with inner/outer classes,
	//  * like bridge methods, contained synthetic fields etc. Very expensive.
	//  */
	// @Range(from = 0, to = 1)
	// float ofDetailedAnalysis();

	// @Range(from = 0, to = 1)
	// float ofCombination(int flags, boolean normalize);

	// int NAME = 1;
	// int OWN_ATTRIBUTES = 2;
	// int CLASSPATH_ATTRIBUTES = 4;
	// int DETAILED_ANALYSIS = 8;
}
