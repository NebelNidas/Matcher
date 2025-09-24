package matcher.jobs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;

import job4j.JobState;

import matcher.Matcher;
import matcher.classifier.ClassClassifier;
import matcher.classifier.ClassifierLevel;
import matcher.classifier.RankResult;
import matcher.type.ClassEnvironment;
import matcher.type.ClassInstance;

public class AutoMatchClassesLocalJob extends MatcherJob<Map<ClassInstance, ClassInstance>> {
	public AutoMatchClassesLocalJob(Matcher matcher, ClassifierLevel level, List<ClassInstance> classes) {
		super(JobCategories.AUTOMATCH_CLASSES_LOCAL);

		this.matcher = matcher;
		this.level = level;
		this.classes = classes;
	}

	@Override
	protected Map<ClassInstance, ClassInstance> execute(DoubleConsumer progressReceiver) {
		ClassEnvironment env = matcher.getEnv();
		boolean assumeBothOrNoneObfuscated = env.assumeBothOrNoneObfuscated;
		Predicate<ClassInstance> filter = cls -> cls.isReal() && (!assumeBothOrNoneObfuscated || cls.isNameObfuscated()) && !cls.hasMatch() && cls.isMatchable();

		ClassInstance[] cmpClasses = env.getClassesB().stream()
				.filter(filter)
				.toArray(ClassInstance[]::new);

		double maxScore = ClassClassifier.getMaxScore(level);
		double maxMismatch = maxScore - Matcher.getRawScore(Matcher.absClassAutoMatchThreshold * (1 - Matcher.relClassAutoMatchThreshold), maxScore);
		Map<ClassInstance, ClassInstance> matches = new ConcurrentHashMap<>(classes.size());

		Matcher.runInParallel(classes, cls -> {
			if (state == JobState.CANCELING) {
				return;
			}

			List<RankResult<ClassInstance>> ranking = ClassClassifier.rank(cls, cmpClasses, level, env, maxMismatch);

			if (Matcher.checkRank(ranking, Matcher.absClassAutoMatchThreshold, Matcher.relClassAutoMatchThreshold, maxScore)) {
				ClassInstance match = ranking.get(0).getSubject();

				matches.put(cls, match);
			}
		}, progressReceiver);

		return matches;
	}

	private final Matcher matcher;
	private final ClassifierLevel level;
	private final List<ClassInstance> classes;
}
