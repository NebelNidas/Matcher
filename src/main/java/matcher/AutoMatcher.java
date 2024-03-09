package matcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import matcher.config.Config;
import matcher.config.ProjectConfig;
import matcher.mapping.MappingField;
import matcher.mapping.Mappings;
import matcher.serdes.MatchesIo;
import matcher.type.ClassEnvironment;
import net.fabricmc.mappingio.MappingReader;

public class AutoMatcher {
	public void start(
			List<Path> inputsA,
			List<Path> inputsB,
			List<Path> classpathA,
			List<Path> classpathB,
			List<Path> sharedClasspath,
			boolean inputsBeforeClasspath,
			String nonObfuscatedClassPatternA,
			String nonObfuscatedClassPatternB,
			String nonObfuscatedMemberPatternA,
			String nonObfuscatedMemberPatternB,
			Path mappingsPathA,
			Path mappingsPathB,
			boolean dontSaveUnmappedMatches,
			int passes,
			Path outputFile) {
		Matcher.init();
		ClassEnvironment env = new ClassEnvironment();
		Matcher matcher = new Matcher(env);
		ProjectConfig config = new ProjectConfig.Builder(inputsA, inputsB)
				.classPathA(new ArrayList<>(classpathA))
				.classPathB(new ArrayList<>(classpathB))
				.sharedClassPath(new ArrayList<>(sharedClasspath))
				.inputsBeforeClassPath(inputsBeforeClasspath)
				.mappingsPathA(mappingsPathA)
				.mappingsPathB(mappingsPathB)
				.saveUnmappedMatches(!dontSaveUnmappedMatches)
				.nonObfuscatedClassPatternA(nonObfuscatedClassPatternA)
				.nonObfuscatedClassPatternB(nonObfuscatedClassPatternB)
				.nonObfuscatedMemberPatternA(nonObfuscatedMemberPatternA)
				.nonObfuscatedMemberPatternB(nonObfuscatedMemberPatternB)
				.build();

		Config.setProjectConfig(config);
		matcher.init(config, (progress) -> { });

		if (config.getMappingsPathA() != null) {
			Path mappingsPath = config.getMappingsPathA();

			try {
				List<String> namespaces = MappingReader.getNamespaces(mappingsPath, null);
				Mappings.load(mappingsPath, null,
						namespaces.get(0), namespaces.get(1),
						MappingField.PLAIN, MappingField.MAPPED,
						env.getEnvA(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (config.getMappingsPathB() != null) {
			Path mappingsPath = config.getMappingsPathB();

			try {
				List<String> namespaces = MappingReader.getNamespaces(mappingsPath, null);
				Mappings.load(mappingsPath, null,
						namespaces.get(0), namespaces.get(1),
						MappingField.PLAIN, MappingField.MAPPED,
						env.getEnvB(), true);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		for (int i = 0; i < passes; i++) {
			matcher.autoMatchAll((progress) -> { });
		}

		try {
			Files.deleteIfExists(outputFile);
			MatchesIo.write(matcher, outputFile);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		Matcher.LOGGER.info("Auto-matching done!");
	}
}
