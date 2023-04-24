package matcher.gui.cli;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.beust.jcommander.Parameter;

import matcher.cli.CliParameterProvider;

public class GuiPluginCliParameterProvider implements CliParameterProvider {
	public GuiPluginCliParameterProvider(List<Path> pluginPaths) {
		this.pluginPaths = pluginPaths;
	}

	@Parameter(names = {BuiltinGuiCliArgs.ADDITIONAL_PLUGINS})
	List<Path> additionalPlugins = Collections.emptyList();

	@Override
	public Object getDataHolder() {
		return this;
	}

	@Override
	public void processArgs() {
		pluginPaths.addAll(additionalPlugins);
	}

	private final List<Path> pluginPaths;
}
