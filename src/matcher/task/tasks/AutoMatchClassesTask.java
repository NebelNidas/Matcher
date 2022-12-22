package matcher.task.tasks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import matcher.Matcher;
import matcher.classifier.ClassClassifier;
import matcher.classifier.ClassifierLevel;
import matcher.classifier.RankResult;
import matcher.task.Task;
import matcher.type.ClassEnvironment;
import matcher.type.ClassInstance;

public class AutoMatchClassesTask extends Task<Boolean> {
	public AutoMatchClassesTask(Matcher matcher, ClassifierLevel level) {
		super(ID, null);

		this.matcher = matcher;
		this.level = level;

		setAction(this::autoMatchClasses);
	}

	public boolean autoMatchClasses(DoubleConsumer progress) {
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
			List<RankResult<ClassInstance>> ranking = ClassClassifier.rank(cls, cmpClasses, level, env, maxMismatch);

			if (Matcher.checkRank(ranking, Matcher.absClassAutoMatchThreshold, Matcher.relClassAutoMatchThreshold, maxScore)) {
				ClassInstance match = ranking.get(0).getSubject();

				matches.put(cls, match);
			}
		}, progress);

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
