package matcher.task.tasks;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;

import matcher.Matcher;
import matcher.classifier.ClassifierLevel;
import matcher.classifier.FieldClassifier;
import matcher.task.Task;
import matcher.type.FieldInstance;

public class AutoMatchFieldsTask extends Task<Boolean> {
	public AutoMatchFieldsTask(Matcher matcher, ClassifierLevel level) {
		super(ID, null);

		this.matcher = matcher;
		this.level = level;

		setAction(this::autoMatchFields);
	}

	public boolean autoMatchFields(DoubleConsumer progressReceiver) {
		AtomicInteger totalUnmatched = new AtomicInteger();
		double maxScore = FieldClassifier.getMaxScore(level);

		Map<FieldInstance, FieldInstance> matches = matcher.match(level,
				Matcher.absFieldAutoMatchThreshold, Matcher.relFieldAutoMatchThreshold,
				cls -> cls.getFields(), FieldClassifier::rank, maxScore,
				progressReceiver, totalUnmatched);

		for (Map.Entry<FieldInstance, FieldInstance> entry : matches.entrySet()) {
			matcher.match(entry.getKey(), entry.getValue());
		}

		System.out.println("Auto matched "+matches.size()+" fields ("+totalUnmatched.get()+" unmatched)");

		return !matches.isEmpty();
	}

	public static final String ID = "auto-match-fields";
	private final Matcher matcher;
	private final ClassifierLevel level;
}
