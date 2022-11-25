package matcher.task.tasks;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.DoubleConsumer;

import matcher.Matcher;
import matcher.config.Config;
import matcher.config.UidConfig;
import matcher.task.Task;
import matcher.type.ClassEnv;
import matcher.type.ClassEnvironment;
import matcher.type.ClassInstance;
import matcher.type.FieldInstance;
import matcher.type.MatchableKind;
import matcher.type.MethodInstance;
import matcher.type.MethodVarInstance;

public class ImportMatchesTask extends Task<Void> {
	public ImportMatchesTask(Matcher matcher) {
		super(ID, null);

		this.matcher = matcher;

		setAction((progress) -> {
			importMatches(progress);
			return null;
		});
	}

	private void importMatches(DoubleConsumer progressConsumer) {
		UidConfig config = Config.getUidConfig();
		if (!config.isValid()) return;

		try {
			HttpURLConnection conn = (HttpURLConnection) new URL("https",
					config.getAddress().getHostString(),
					config.getAddress().getPort(),
					String.format("/%s/matches/%s/%s", config.getProject(), config.getVersionA(), config.getVersionB())).openConnection();
			conn.setRequestProperty("X-Token", config.getToken());

			progressConsumer.accept(0.5);

			try (DataInputStream is = new DataInputStream(conn.getInputStream())) {
				ClassEnvironment env = matcher.getEnv();
				int typeOrdinal;
				MatchableKind type;

				while ((typeOrdinal = is.read()) != -1) {
					type = MatchableKind.VALUES[typeOrdinal];
					int uid = is.readInt();
					String idA = is.readUTF();
					String idB = is.readUTF();

					ClassInstance clsA = getCls(env.getEnvA(), idA, type);
					ClassInstance clsB = getCls(env.getEnvB(), idB, type);
					if (clsA == null || clsB == null) continue;

					switch (type) {
					case CLASS:
						matcher.match(clsA, clsB);
						break;
					case METHOD:
					case METHOD_ARG:
					case METHOD_VAR: {
						MethodInstance methodA = getMethod(clsA, idA, type);
						MethodInstance methodB = getMethod(clsB, idB, type);
						if (methodA == null || methodB == null) break;

						if (type == MatchableKind.METHOD) {
							matcher.match(methodA, methodB);
						} else {
							idA = idA.substring(idA.lastIndexOf(')') + 1);
							idB = idB.substring(idB.lastIndexOf(')') + 1);

							MethodVarInstance varA = methodA.getVar(idA, type == MatchableKind.METHOD_ARG);
							MethodVarInstance varB = methodB.getVar(idB, type == MatchableKind.METHOD_ARG);

							if (varA != null && varB != null) {
								matcher.match(varA, varB);
							}
						}

						break;
					}
					case FIELD: {
						FieldInstance fieldA = getField(clsA, idA);
						FieldInstance fieldB = getField(clsB, idB);
						if (fieldA == null || fieldB == null) break;

						matcher.match(fieldA, fieldB);
						break;
					}
					}
				}
			}

			progressConsumer.accept(1);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private ClassInstance getCls(ClassEnv env, String fullId, MatchableKind type) {
		if (type == MatchableKind.CLASS) {
			return env.getLocalClsById(fullId);
		} else if (type == MatchableKind.FIELD) {
			int pos = fullId.lastIndexOf('/', fullId.lastIndexOf(";;") - 2);

			return env.getLocalClsById(fullId.substring(0, pos));
		} else {
			int pos = fullId.lastIndexOf('/', fullId.lastIndexOf('(') - 1);

			return env.getLocalClsById(fullId.substring(0, pos));
		}
	}

	private MethodInstance getMethod(ClassInstance cls, String fullId, MatchableKind type) {
		int end = type == MatchableKind.METHOD ? fullId.length() : fullId.lastIndexOf(')') + 1;

		return cls.getMethod(fullId.substring(fullId.lastIndexOf('/', fullId.lastIndexOf('(', end - 1) - 1) + 1, end));
	}

	private FieldInstance getField(ClassInstance cls, String fullId) {
		return cls.getField(fullId.substring(fullId.lastIndexOf('/', fullId.lastIndexOf(";;") - 2) + 1));
	}

	public static final String ID = "import-matches";
	private final Matcher matcher;
}
