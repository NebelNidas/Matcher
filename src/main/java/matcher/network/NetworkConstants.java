package matcher.network;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.VersionParsingException;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkConstants {
	public static final SemanticVersion PROTOCOL_VERSION;
	public static final InetAddress MULTICAST_ADDRESS;
	public static final int MULTICAST_PORT = 4445;
	public static final int MULTICAST_INTERVAL_MS = 3000;

	static {
		try {
			PROTOCOL_VERSION = SemanticVersion.parse("1.0.0");
		} catch (VersionParsingException e) {
			throw new RuntimeException(e);
		}

		try {
			MULTICAST_ADDRESS = InetAddress.getByName("224.0.2.56");
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
}
