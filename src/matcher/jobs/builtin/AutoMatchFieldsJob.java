package matcher.jobs.builtin;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;

import matcher.Matcher;
import matcher.classifier.ClassifierLevel;
import matcher.classifier.FieldClassifier;
import matcher.jobs.Job;
import matcher.type.FieldInstance;

public class AutoMatchFieldsJob extends Job<Boolean> {
	public AutoMatchFieldsJob(Matcher matcher, ClassifierLevel level) {
		super(ID);

		this.matcher = matcher;
		this.level = level;
	}

	@Override
	protected Boolean execute(DoubleConsumer progressReceiver) {
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
