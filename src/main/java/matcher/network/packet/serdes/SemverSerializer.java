package matcher.network.packet.serdes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import net.fabricmc.loader.api.SemanticVersion;

import java.io.IOException;

public class SemverSerializer extends StdSerializer<SemanticVersion> {
	public SemverSerializer() {
		super(SemanticVersion.class);
	}

	@Override
	public void serialize(SemanticVersion version, JsonGenerator generator, SerializerProvider provider) throws IOException {
		generator.writeString(version.getFriendlyString());
	}

	public static class Module extends SimpleModule {
		public Module() {
			super("SemverSerializerModule");
			addSerializer(SemanticVersion.class, new SemverSerializer());
		}
	}
}
