package matcher.gui.plugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import matcher.cli.MatcherCli;
import matcher.gui.cli.GuiPluginCliParameterProvider;

public class PluginLoader {
	public static void run(String[] args) {
		List<Path> pluginPaths = new ArrayList<>();
		pluginPaths.add(Paths.get("plugins"));

		MatcherCli cli = new MatcherCli(true);
		cli.registerParameterProvider(new GuiPluginCliParameterProvider(pluginPaths));
		cli.processArgs(args);

		List<URL> urls = new ArrayList<>();

		for (Path path : pluginPaths) {
			try {
				if (Files.isDirectory(path)) {
					Stream<Path> stream = Files.list(path);
					urls.addAll(stream
							.filter(p -> p.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".jar"))
							.map(p -> {
								try {
									return p.toUri().toURL();
								} catch (MalformedURLException e) {
									throw new RuntimeException(e);
								}
							})
							.collect(Collectors.toList()));
					stream.close();
				} else if (path.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
					urls.add(path.toUri().toURL());
				} else {
					System.err.println("No plugin(s) found at " + path.toFile().getCanonicalPath());
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]));

		ServiceLoader<Plugin> pluginLoader = ServiceLoader.load(Plugin.class, cl);

		for (Plugin p : pluginLoader) {
			p.init(apiVersion);
		}
	}

	private static final int apiVersion = 0;
}
