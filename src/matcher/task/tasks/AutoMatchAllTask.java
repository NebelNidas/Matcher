package matcher.task.tasks;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import matcher.Matcher;
import matcher.classifier.ClassifierLevel;
import matcher.task.Task;
import matcher.task.TaskGroup;
import matcher.type.MatchType;

public class AutoMatchAllTask extends TaskGroup<Set<MatchType>> {
	public AutoMatchAllTask(Matcher matcher) {
		super(ID, null);
		this.matcher = matcher;

		setReturnValueSupplier(() -> {
			// We could register the subtasks earlier, but it's a design decision
			// that child tasks only show once the parent is started
			registerTasks();

			for (Task<?> task : subTasks) {
				task.run();
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

	private void registerTasks() {
		var task = new AutoMatchClassesTask(matcher, ClassifierLevel.Initial);
		task.addOnSuccess((matchedAny) -> {
			matchedAnyClasses.set(matchedAnyClasses.get() | matchedAny);
		});
		addSubTask(task, true);
		addSubTask(task, true);

		Task<Boolean> levelTask = new TaskGroup<>(ID + ":intermediate", null) {{
				setReturnValueSupplier(() -> {
					autoMatchMembers(ClassifierLevel.Intermediate, this);
					return matchedAnyMembers.getAndSet(false);
				});
			}};
		addSubTask(levelTask, true);

		levelTask = new TaskGroup<Boolean>(ID + ":full", null) {{
				setReturnValueSupplier(() -> {
					autoMatchMembers(ClassifierLevel.Full, this);
					return matchedAnyMembers.getAndSet(false);
				});
			}};
		addSubTask(levelTask, false);

		levelTask = new TaskGroup<Boolean>(ID + ":extra", null) {{
				setReturnValueSupplier(() -> {
					autoMatchMembers(ClassifierLevel.Extra, this);
					return matchedAnyMembers.getAndSet(false);
				});
			}};
		addSubTask(levelTask, false);

		var argVarTask = new TaskGroup<Boolean>(ID + ":args-and-vars", null) {{
				setReturnValueSupplier(() -> {
					autoMatchVars(this);
					return matchedAnyVars.get();
				});
			}};
		addSubTask(argVarTask, true);
	}

	private void autoMatchMembers(ClassifierLevel level, TaskGroup<Boolean> parentTask) {
		AtomicBoolean methodsDone = new AtomicBoolean(false);
		AtomicBoolean fieldsDone = new AtomicBoolean(false);

		// Methods
		var methodTask = new AutoMatchMethodsTask(matcher, level);
		methodTask.addOnSuccess((result) -> matchedAnyMembers.set(matchedAnyMembers.get() | result));
		methodTask.addOnFinish(() -> {
			methodsDone.set(true);

			autoMatchMembers0(methodsDone.get(), fieldsDone.get(), level, parentTask);
		});
		parentTask.addSubTask(methodTask, false);

		// Fields
		var fieldTask = new AutoMatchFieldsTask(matcher, level);
		fieldTask.addOnSuccess((result) -> matchedAnyMembers.set(matchedAnyMembers.get() | result));
		fieldTask.addOnFinish(() -> {
			fieldsDone.set(true);

			autoMatchMembers0(methodsDone.get(), fieldsDone.get(), level, parentTask);
		});
		parentTask.addSubTask(fieldTask, false);
	}

	private void autoMatchMembers0(boolean methodsDone, boolean fieldsDone, ClassifierLevel level, TaskGroup<Boolean> parentTask) {
		if (!methodsDone || !fieldsDone
				|| (!matchedAnyMembers.get() && !matchedClassesBefore.get())) {
			return;
		}

		var task = new AutoMatchClassesTask(matcher, level);
		task.addOnSuccess((matchedAny) -> {
			matchedClassesBefore.set(matchedAny);
			matchedAnyMembers.set(matchedAnyMembers.get() | matchedAny);

			if (matchedAnyMembers.get()) {
				autoMatchMembers(level, parentTask);
			}
		});
		matchedAnyMembers.set(false);
		parentTask.addSubTask(task, fieldsDone);
	}

	private void autoMatchVars(TaskGroup<Boolean> parentTask) {
		AtomicBoolean argsDone = new AtomicBoolean(false);
		AtomicBoolean varsDone = new AtomicBoolean(false);

		// Args
		var argTask = new AutoMatchMethodVarsTask(matcher, ClassifierLevel.Full, true);
		argTask.addOnSuccess((result) -> matchedAnyVars.set(matchedAnyVars.get() | result));
		argTask.addOnFinish(() -> {
			argsDone.set(true);

			autoMatchVars0(argsDone.get(), varsDone.get(), parentTask);
		});
		parentTask.addSubTask(argTask, false);

		// Vars
		var varTask = new AutoMatchMethodVarsTask(matcher, ClassifierLevel.Full, false);
		varTask.addOnSuccess((result) -> matchedAnyVars.set(matchedAnyVars.get() | result));
		varTask.addOnFinish(() -> {
			varsDone.set(true);

			autoMatchVars0(argsDone.get(), varsDone.get(), parentTask);
		});
		parentTask.addSubTask(varTask, false);
	}

	private void autoMatchVars0(boolean argsDone, boolean varsDone, TaskGroup<Boolean> parentTask) {
		if (!argsDone || !varsDone) {
			return;
		} else if (!matchedAnyVars.get()) {
			matcher.getEnv().getCache().clear();
			return;
		}

		matchedAnyVars.set(false);
		autoMatchVars(parentTask);
	}

	public static final String ID = "auto-match-all";
	private final Matcher matcher;
	AtomicBoolean matchedClassesBefore = new AtomicBoolean(true);
	AtomicBoolean matchedAnyClasses = new AtomicBoolean(false);
	AtomicBoolean matchedAnyMembers = new AtomicBoolean(false);
	AtomicBoolean matchedAnyVars = new AtomicBoolean(false);
}
