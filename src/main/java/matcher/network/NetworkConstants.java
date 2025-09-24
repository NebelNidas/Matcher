package matcher.network;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;

public class NetworkConstants {
	public static final SemanticVersion PROTOCOL_VERSION;
	public static final String MULTICAST_ADDRESS = "224.0.2.56";
	public static final int MULTICAST_PORT = 4445;
	public static final int MULTICAST_INTERVAL_MS = 3000;

	static {
		try {
			PROTOCOL_VERSION = SemanticVersion.parse("1.0.0");
		} catch (VersionParsingException e) {
			throw new RuntimeException(e);
		}
	}
}
