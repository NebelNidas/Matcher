package matcher.gui.plugin;

public interface Plugin {
	String getName();
	String getVersion();
	void init(int pluginApiVersion);
}
