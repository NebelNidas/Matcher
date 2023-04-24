package matcher.gui;

import javafx.application.Application;

import matcher.config.Config;
import matcher.gui.plugin.PluginLoader;

public class Main {
	public static void main(String[] args) {
		Config.init();
		PluginLoader.run(args);
		Application.launch(MatcherGui.class, args);
	}
}
