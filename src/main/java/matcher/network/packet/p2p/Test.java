package matcher.network.packet.p2p;

import net.fabricmc.loader.api.SemanticVersion;

import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;

public record Test(PacketType type, Object data) implements Packet<Object> {
	public Test() {
		this(PacketType.TEST, null);
	}
}
