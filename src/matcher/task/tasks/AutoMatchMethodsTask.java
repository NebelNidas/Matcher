package matcher.task.tasks;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;

import matcher.Matcher;
import matcher.classifier.ClassifierLevel;
import matcher.classifier.MethodClassifier;
import matcher.task.Task;
import matcher.type.MethodInstance;

public class AutoMatchMethodsTask extends Task<Boolean> {
	public AutoMatchMethodsTask(Matcher matcher, ClassifierLevel level) {
		super(ID, null);

		this.matcher = matcher;
		this.level = level;

		setAction(this::autoMatchMethods);
	}

	private boolean autoMatchMethods(DoubleConsumer progressReceiver) {
		AtomicInteger totalUnmatched = new AtomicInteger();
		Map<MethodInstance, MethodInstance> matches = matcher.match(level, Matcher.absMethodAutoMatchThreshold, Matcher.relMethodAutoMatchThreshold,
				cls -> cls.getMethods(), MethodClassifier::rank, MethodClassifier.getMaxScore(level),
				progressReceiver, totalUnmatched);

		for (Map.Entry<MethodInstance, MethodInstance> entry : matches.entrySet()) {
			matcher.match(entry.getKey(), entry.getValue());
		}

		System.out.println("Auto matched "+matches.size()+" methods ("+totalUnmatched.get()+" unmatched)");

		return !matches.isEmpty();
	}

	public static final String ID = "auto-match-methods";
	private final Matcher matcher;
	private final ClassifierLevel level;
}
