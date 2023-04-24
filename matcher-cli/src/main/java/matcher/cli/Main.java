package matcher.cli;

import matcher.config.Config;

public class Main {
	public static void main(String[] args) {
		Config.init();

		MatcherCli matcherCli = new MatcherCli(false);
		matcherCli.registerCommandProvider(new AutomatchCliCommandProvider());
		matcherCli.processArgs(args);
	}
}
