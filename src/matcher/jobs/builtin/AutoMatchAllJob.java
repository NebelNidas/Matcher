package matcher.jobs.builtin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;

import matcher.Matcher;
import matcher.classifier.ClassifierLevel;
import matcher.jobs.Job;
import matcher.jobs.JobState;
import matcher.type.MatchType;

public class AutoMatchAllJob extends Job<Set<MatchType>> {
	public AutoMatchAllJob(Matcher matcher) {
		super(ID);
		this.matcher = matcher;
	}

	@Override
	protected Set<MatchType> execute(DoubleConsumer progress) {
		for (Job<?> job : getSubJobs()) {
			if (state == JobState.CANCELING) {
				break;
			}

			job.run();
		}

		matcher.getEnv().getCache().clear();

		Set<MatchType> matchedTypes = new HashSet<>();

		if (matchedAnyClassesOverall.get()) {
			matchedTypes.add(MatchType.Class);
		}

		if (matchedAnyMembersOverall.get()) {
			matchedTypes.add(MatchType.Method);
			matchedTypes.add(MatchType.Field);
		}

		if (matchedAnyLocalsOverall.get()) {
			matchedTypes.add(MatchType.MethodVar);
		}

		return matchedTypes;
	}

	@Override
	protected void registerSubJobs() {
		var matchClassesJob = new AutoMatchClassesJob(matcher, ClassifierLevel.Initial);
		matchClassesJob.addCompletionListener((matchedAnyClasses, error) -> {
			matchedAnyClassesOverall.set(matchedAnyClassesOverall.get() | matchedAnyClasses.orElse(false));
		});
		addSubJob(matchClassesJob, true);

		matchClassesJob = new AutoMatchClassesJob(matcher, ClassifierLevel.Initial);
		matchClassesJob.addCompletionListener((matchedAnyClasses, error) -> {
			matchedAnyClassesOverall.set(matchedAnyClassesOverall.get() | matchedAnyClasses.orElse(false));
		});
		addSubJob(matchClassesJob, false);

		Job<Boolean> job = new Job<>(ID + ":intermediate") {
			@Override
			protected Boolean execute(DoubleConsumer progress) {
				autoMatchMembers(ClassifierLevel.Intermediate, this);
				return matchedAnyMembersOverall.getAndSet(false);
			}
		};
		addSubJob(job, false);

		job = new Job<>(ID + ":full") {
			@Override
			protected Boolean execute(DoubleConsumer progress) {
				autoMatchMembers(ClassifierLevel.Full, this);
				return matchedAnyMembersOverall.getAndSet(false);
			};
		};
		addSubJob(job, false);

		job = new Job<>(ID + ":extra") {
			@Override
			protected Boolean execute(DoubleConsumer progress) {
				autoMatchMembers(ClassifierLevel.Extra, this);
				return matchedAnyMembersOverall.getAndSet(false);
			};
		};
		addSubJob(job, false);

		job = new Job<Boolean>(ID + ":args-and-vars") {
			@Override
			protected Boolean execute(DoubleConsumer progress) {
				autoMatchLocals(this);
				return matchedAnyLocalsOverall.get();
			};
		};
		addSubJob(job, false);
	}

	private void autoMatchMembers(ClassifierLevel level, Job<Boolean> parentJob) {
		if (parentJob.getState() == JobState.CANCELING) {
			return;
		}

		// Methods
		var methodJob = new AutoMatchMethodsJob(matcher, level);
		methodJob.addCompletionListener((matchedAnyMethods, error) -> {
			matchedAnyMembersOverall.set(matchedAnyMembersOverall.get() | matchedAnyMethods.orElse(false));
		});
		parentJob.addSubJob(methodJob, false);

		// Fields
		var fieldJob = new AutoMatchFieldsJob(matcher, level);
		fieldJob.addCompletionListener((matchedAnyFields, error) -> {
			matchedAnyMembersOverall.set(matchedAnyMembersOverall.get() | matchedAnyFields.orElse(false));
		});
		parentJob.addSubJob(fieldJob, false);

		// Run subjobs
		methodJob.run();
		fieldJob.run();
		autoMatchMembers0(level, parentJob);
	}

	private void autoMatchMembers0(ClassifierLevel level, Job<Boolean> parentJob) {
		if (parentJob.getState() == JobState.CANCELING) {
			return;
		}

		if (!matchedAnyMembersOverall.get()) {
			return;
		}

		var job = new AutoMatchClassesJob(matcher, level);
		job.addCompletionListener((matchedAnyClasses, error) -> {
			matchedAnyClassesBefore.set(matchedAnyClasses.orElse(false));
			matchedAnyMembersOverall.set(matchedAnyMembersOverall.get() | matchedAnyClasses.orElse(false));

			if (matchedAnyMembersOverall.get()) {
				autoMatchMembers(level, parentJob);
			}
		});
		matchedAnyMembersOverall.set(false);
		parentJob.addSubJob(job, false);
		job.run();
	}

	private void autoMatchLocals(Job<Boolean> parentJob) {
		do {
			matchedAnyLocalsOverall.set(false);

			// Args
			var argJob = new AutoMatchMethodVarsJob(matcher, ClassifierLevel.Full, true);
			argJob.addCompletionListener((matchedAnyArgs, error) -> {
				matchedAnyLocalsOverall.set(matchedAnyLocalsOverall.get() | matchedAnyArgs.orElse(false));
				System.out.println("Matching args finished");
			});
			parentJob.addSubJob(argJob, false);

			// Vars
			var varJob = new AutoMatchMethodVarsJob(matcher, ClassifierLevel.Full, false);
			varJob.addCompletionListener((matchedAnyVars, error) -> {
				matchedAnyLocalsOverall.set(matchedAnyLocalsOverall.get() | matchedAnyVars.orElse(false));
				System.out.println("Matching vars finished");
			});
			parentJob.addSubJob(varJob, false);

			// Run subjobs
			argJob.run();
			varJob.run();
		} while (matchedAnyLocalsOverall.get() && parentJob.getState() != JobState.CANCELING);
	}

	public static final String ID = "auto-match-all";
	private final Matcher matcher;
	private final AtomicBoolean matchedAnyClassesBefore = new AtomicBoolean(true);
	private final AtomicBoolean matchedAnyClassesOverall = new AtomicBoolean(false);
	private final AtomicBoolean matchedAnyMembersOverall = new AtomicBoolean(false);
	private final AtomicBoolean matchedAnyLocalsOverall = new AtomicBoolean(false);
}
