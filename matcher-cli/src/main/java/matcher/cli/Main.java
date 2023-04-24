package matcher.cli;

import matcher.config.Config;

public class Main {
	public static void main(String[] args) {
		Config.init(args);

		MatcherCli matcherCli = new MatcherCli();
		matcherCli.registerCommandProvider(new AutomatchCliCommandProvider());
		matcherCli.processArgs(args);
	}
}
