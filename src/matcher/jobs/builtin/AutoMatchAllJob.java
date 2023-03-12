package matcher.jobs.builtin;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import matcher.Matcher;
import matcher.classifier.ClassifierLevel;
import matcher.jobs.Job;
import matcher.jobs.JobGroup;
import matcher.type.MatchType;

public class AutoMatchAllJob extends JobGroup<Set<MatchType>> {
	public AutoMatchAllJob(Matcher matcher) {
		super(ID, null);
		this.matcher = matcher;

		setReturnValueSupplier(() -> {
			// We could register the subjobs earlier, but it's a design decision
			// that child jobs only show once the parent is started
			registerJobs();

			for (Job<?> job : subJobs) {
				job.run();
			}

			matcher.getEnv().getCache().clear();

			Set<MatchType> matchedTypes = new HashSet<>();

			if (matchedAnyClasses.get()) {
				matchedTypes.add(MatchType.Class);
			}

			if (matchedAnyMembers.get()) {
				matchedTypes.add(MatchType.Method);
				matchedTypes.add(MatchType.Field);
			}

			if (matchedAnyVars.get()) {
				matchedTypes.add(MatchType.MethodVar);
			}

			return matchedTypes;
		});
	}

	private void registerJobs() {
		var job = new AutoMatchClassesJob(matcher, ClassifierLevel.Initial);
		job.addOnSuccess((matchedAny) -> {
			matchedAnyClasses.set(matchedAnyClasses.get() | matchedAny);
		});
		addSubJob(job, true);
		addSubJob(job, true);

		Job<Boolean> levelJob = new JobGroup<>(ID + ":intermediate", null) {{
			setReturnValueSupplier(() -> {
				autoMatchMembers(ClassifierLevel.Intermediate, this);
				return matchedAnyMembers.getAndSet(false);
			});
		}};
		addSubJob(levelJob, true);

		levelJob = new JobGroup<Boolean>(ID + ":full", null) {{
			setReturnValueSupplier(() -> {
				autoMatchMembers(ClassifierLevel.Full, this);
				return matchedAnyMembers.getAndSet(false);
			});
		}};
		addSubJob(levelJob, false);

		levelJob = new JobGroup<Boolean>(ID + ":extra", null) {{
			setReturnValueSupplier(() -> {
				autoMatchMembers(ClassifierLevel.Extra, this);
				return matchedAnyMembers.getAndSet(false);
			});
		}};
		addSubJob(levelJob, false);

		var argVarJob = new JobGroup<Boolean>(ID + ":args-and-vars", null) {{
			setReturnValueSupplier(() -> {
				autoMatchVars(this);
				return matchedAnyVars.get();
			});
		}};
		addSubJob(argVarJob, true);
	}

	private void autoMatchMembers(ClassifierLevel level, JobGroup<Boolean> parentJob) {
		AtomicBoolean methodsDone = new AtomicBoolean(false);
		AtomicBoolean fieldsDone = new AtomicBoolean(false);

		// Methods
		var methodJob = new AutoMatchMethodsJob(matcher, level);
		methodJob.addOnSuccess((result) -> matchedAnyMembers.set(matchedAnyMembers.get() | result));
		methodJob.addOnFinish(() -> {
			methodsDone.set(true);
			System.out.println("Matching methods finished");

			autoMatchMembers0(methodsDone.get(), fieldsDone.get(), level, parentJob);
		});
		parentJob.addSubJob(methodJob, false);

		// Fields
		var fieldJob = new AutoMatchFieldsJob(matcher, level);
		fieldJob.addOnSuccess((result) -> matchedAnyMembers.set(matchedAnyMembers.get() | result));
		fieldJob.addOnFinish(() -> {
			fieldsDone.set(true);
			System.out.println("Matching fields finished");

			autoMatchMembers0(methodsDone.get(), fieldsDone.get(), level, parentJob);
		});
		parentJob.addSubJob(fieldJob, false);

		for (Job<?> job : parentJob.getSubJobs()) {
			job.run();
		}
	}

	private void autoMatchMembers0(boolean methodsDone, boolean fieldsDone, ClassifierLevel level, JobGroup<Boolean> parentJob) {
		if (!methodsDone || !fieldsDone
				|| (!matchedAnyMembers.get() && !matchedClassesBefore.get())) {
			return;
		}

		var job = new AutoMatchClassesJob(matcher, level);
		job.addOnSuccess((matchedAny) -> {
			matchedClassesBefore.set(matchedAny);
			matchedAnyMembers.set(matchedAnyMembers.get() | matchedAny);

			if (matchedAnyMembers.get()) {
				autoMatchMembers(level, parentJob);
			}
		});
		matchedAnyMembers.set(false);
		parentJob.addSubJob(job, fieldsDone);
		job.run();
	}

	private void autoMatchVars(JobGroup<Boolean> parentJob) {
		AtomicBoolean argsDone = new AtomicBoolean(false);
		AtomicBoolean varsDone = new AtomicBoolean(false);

		// Args
		var argJob = new AutoMatchMethodVarsJob(matcher, ClassifierLevel.Full, true);
		argJob.addOnSuccess((result) -> matchedAnyVars.set(matchedAnyVars.get() | result));
		argJob.addOnFinish(() -> {
			argsDone.set(true);
			System.out.println("Matching args finished");

			autoMatchVars0(argsDone.get(), varsDone.get(), parentJob);
		});
		parentJob.addSubJob(argJob, false);

		// Vars
		var varJob = new AutoMatchMethodVarsJob(matcher, ClassifierLevel.Full, false);
		varJob.addOnSuccess((result) -> matchedAnyVars.set(matchedAnyVars.get() | result));
		varJob.addOnFinish(() -> {
			varsDone.set(true);
			System.out.println("Matching vars finished");

			autoMatchVars0(argsDone.get(), varsDone.get(), parentJob);
		});
		parentJob.addSubJob(varJob, false);

		for (Job<?> job : parentJob.getSubJobs()) {
			job.run();
		}
	}

	private void autoMatchVars0(boolean argsDone, boolean varsDone, JobGroup<Boolean> parentJob) {
		if (!argsDone || !varsDone) {
			return;
		} else if (!matchedAnyVars.get()) {
			matcher.getEnv().getCache().clear();
			return;
		}

		matchedAnyVars.set(false);
		autoMatchVars(parentJob);
	}

	public static final String ID = "auto-match-all";
	private static final Supplier<Set<MatchType>> returnValue = ;
	private final Matcher matcher;
	private final AtomicBoolean matchedClassesBefore = new AtomicBoolean(true);
	private final AtomicBoolean matchedAnyClasses = new AtomicBoolean(false);
	private final AtomicBoolean matchedAnyMembers = new AtomicBoolean(false);
	private final AtomicBoolean matchedAnyVars = new AtomicBoolean(false);
}
