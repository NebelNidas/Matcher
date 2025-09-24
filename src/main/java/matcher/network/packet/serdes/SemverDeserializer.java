package matcher.network.packet.serdes;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import com.fasterxml.jackson.databind.module.SimpleModule;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;

import java.io.IOException;

public class SemverDeserializer extends StdDeserializer<SemanticVersion> {
	public SemverDeserializer() {
		super(SemanticVersion.class);
	}

	@Override
	public SemanticVersion deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		try {
			return SemanticVersion.parse(parser.getValueAsString());
		} catch (VersionParsingException e) {
			throw new IOException(e);
		}
	}

	public static class Module extends SimpleModule {
		public Module() {
			super("SemverDeserializerModule");
			addDeserializer(SemanticVersion.class, new SemverDeserializer());
		}
	}
}
