package matcher.jobs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import job4j.Job;
import job4j.JobState;
import matcher.Matcher;
import matcher.classifier.ClassClassifier;
import matcher.classifier.ClassifierLevel;
import matcher.classifier.RankResult;
import matcher.type.ClassEnvironment;
import matcher.type.ClassInstance;

public class AutoMatchClassesJob extends Job<Boolean> {
	public AutoMatchClassesJob(Matcher matcher, ClassifierLevel level) {
		super(ID);

		this.matcher = matcher;
		this.level = level;
	}

	@Override
	protected Boolean execute(DoubleConsumer progressReceiver) {
		ClassEnvironment env = matcher.getEnv();
		boolean assumeBothOrNoneObfuscated = env.assumeBothOrNoneObfuscated;
		Predicate<ClassInstance> filter = cls -> cls.isReal() && (!assumeBothOrNoneObfuscated || cls.isNameObfuscated()) && !cls.hasMatch() && cls.isMatchable();

		List<ClassInstance> classes = env.getClassesA().stream()
				.filter(filter)
				.collect(Collectors.toList());

		ClassInstance[] cmpClasses = env.getClassesB().stream()
				.filter(filter)
				.collect(Collectors.toList()).toArray(new ClassInstance[0]);

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

		Matcher.sanitizeMatches(matches);

		for (Map.Entry<ClassInstance, ClassInstance> entry : matches.entrySet()) {
			matcher.match(entry.getKey(), entry.getValue());
		}

		System.out.println("Auto matched "+matches.size()+" classes ("+(classes.size() - matches.size())+" unmatched, "+env.getClassesA().size()+" total)");

		return !matches.isEmpty();
	}

	public static final String ID = "auto-match-classes";
	private final Matcher matcher;
	private final ClassifierLevel level;
}
