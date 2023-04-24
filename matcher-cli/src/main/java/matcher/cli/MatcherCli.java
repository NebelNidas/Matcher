package matcher.cli;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beust.jcommander.JCommander;

public class MatcherCli {
	public static final Logger LOGGER = LoggerFactory.getLogger("Matcher CLI");
	private final List<CliParameterProvider> paramProviders = new ArrayList<>(5);
	private final List<CliCommandProvider> commandProviders = new ArrayList<>(5);

	public void registerParameterProvider(CliParameterProvider paramProvider) {
		paramProviders.add(paramProvider);
	}

	public void registerCommandProvider(CliCommandProvider commandProvider) {
		commandProviders.add(commandProvider);
	}

	public void processArgs(String[] args) {
		JCommander.Builder jcBuilder = JCommander.newBuilder();

		for (CliParameterProvider paramProvider : paramProviders) {
			jcBuilder.addObject(paramProvider.getDataHolder());
		}

		for (CliCommandProvider commandProvider : commandProviders) {
			jcBuilder.addCommand(commandProvider.getCommandName(), commandProvider.getDataHolder());
		}

		JCommander jCommander = jcBuilder.build();
		jCommander.parse(args);

		for (CliParameterProvider paramProvider : paramProviders) {
			paramProvider.processArgs();
		}

		for (CliCommandProvider commandProvider : commandProviders) {
			if (commandProvider.getCommandName().equals(jCommander.getParsedCommand())) {
				commandProvider.processArgs();
				break;
			}
		}
	}
}
