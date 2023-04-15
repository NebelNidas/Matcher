package matcher.gui.tab;

import matcher.NameType;
import matcher.gui.Gui;
import matcher.gui.ISelectionProvider;
import matcher.srcprocess.HtmlUtil;
import matcher.type.ClassInstance;
import matcher.type.FieldInstance;
import matcher.type.MethodInstance;

public class BytecodeTab extends WebViewTab {
	public BytecodeTab(Gui gui, ISelectionProvider selectionProvider, boolean unmatchedTmp) {
		super("bytecode", "ui/templates/CodeViewTemplate.htm");

		this.gui = gui;
		this.selectionProvider = selectionProvider;
		this.unmatchedTmp = unmatchedTmp;
	}

	@Override
	public void onClassSelect(ClassInstance cls) {
		update(cls, false);
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
		if (cls == null) {
			displayText("no class selected");
		} else {
			String bytecodeHtml;
			NameType nameType = gui.getNameType().withUnmatchedTmp(unmatchedTmp);
			bytecodeHtml = new BytecodeToHtmlConverter(cls, nameType).convert();

			double prevScroll = isRefresh ? getScrollTop() : 0;

			displayHtml(bytecodeHtml);

			if (isRefresh && prevScroll > 0) {
				setScrollTop(prevScroll);
			}
		}
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
}
