package matcher.gui.tab;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import java.util.function.DoubleConsumer;

import javafx.application.Platform;

import matcher.NameType;
import matcher.gui.Gui;
import matcher.gui.ISelectionProvider;
import matcher.jobs.Job;
import matcher.jobs.JobState;
import matcher.srcprocess.HtmlUtil;
import matcher.srcprocess.SrcDecorator;
import matcher.srcprocess.SrcDecorator.SrcParseException;
import matcher.type.ClassInstance;
import matcher.type.FieldInstance;
import matcher.type.MatchType;
import matcher.type.MethodInstance;

public class SourcecodeTab extends WebViewTab {
	public SourcecodeTab(Gui gui, ISelectionProvider selectionProvider, boolean unmatchedTmp) {
		super("source", "ui/templates/CodeViewTemplate.htm");

		this.gui = gui;
		this.selectionProvider = selectionProvider;
		this.unmatchedTmp = unmatchedTmp;

		init();
	}

	private void init() {
		displayText("no class selected");
	}

	@Override
	public void onClassSelect(ClassInstance cls) {
		update(cls, false);
	}

	@Override
	public void onMatchChange(Set<MatchType> types) {
		ClassInstance cls = selectionProvider.getSelectedClass();

		if (cls != null) {
			update(cls, true);
		}
	}

	@Override
	public void onViewChange(ViewChangeCause cause) {
		ClassInstance cls = selectionProvider.getSelectedClass();

		if ((cls != null
				&& (cause == ViewChangeCause.NAME_TYPE_CHANGED
						|| cause == ViewChangeCause.DECOMPILER_CHANGED))
				|| cause == ViewChangeCause.THEME_CHANGED) {
			update(cls, true);
		}
	}

	private void update(ClassInstance cls, boolean isRefresh) {
		cancelWebViewTasks();

		final int cDecompId = ++decompId;

		if (cls == null) {
			displayText("no class selected");
			return;
		}

		if (!isRefresh) {
			displayText("decompiling...");
		}

		NameType nameType = gui.getNameType().withUnmatchedTmp(unmatchedTmp);

		var decompileJob = new Job<String>("decompile") {
			@Override
			protected String execute(DoubleConsumer progress) {
				return SrcDecorator.decorate(gui.getEnv().decompile(gui.getDecompiler().get(), cls, nameType), cls, nameType);
			}
		};
		decompileJob.dontPrintStacktraceOnError();
		decompileJob.addCompletionListener((code, error) -> Platform.runLater(() -> {
			if (cDecompId == decompId) {
				if (code.isEmpty() && decompileJob.getState() == JobState.CANCELED) {
					// The job got canceled before any code was generated. Ignore any errors.
					return;
				}

				if (error.isPresent()) {
					StringWriter sw = new StringWriter();
					error.get().printStackTrace(new PrintWriter(sw));

					if (error.get() instanceof SrcParseException) {
						SrcParseException parseExc = (SrcParseException) error.get();
						displayText("parse error: "+parseExc.problems+"\ndecompiled source:\n"+parseExc.source);
					} else {
						displayText("decompile error: "+sw.toString());
					}
				} else if (code.isPresent()) {
					double prevScroll = isRefresh ? getScrollTop() : 0;

					displayHtml(code.get());

					if (isRefresh && prevScroll > 0) {
						setScrollTop(prevScroll);
					}
				}
			}
		}));
		decompileJob.run();
	}

	@Override
	public void onMethodSelect(MethodInstance method) {
		if (method != null) select(HtmlUtil.getId(method));
	}

	@Override
	public void onFieldSelect(FieldInstance field) {
		if (field != null) select(HtmlUtil.getId(field));
	}

	private final Gui gui;
	private final ISelectionProvider selectionProvider;
	private final boolean unmatchedTmp;

	private int decompId;
}
