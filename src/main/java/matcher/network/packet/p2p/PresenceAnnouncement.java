package matcher.network.packet.p2p;

import net.fabricmc.loader.api.SemanticVersion;

import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.p2p.PresenceAnnouncement.Data;

public record PresenceAnnouncement(PacketType type, Data data) implements Packet<Data> {
	public PresenceAnnouncement(Data data) {
		this(PacketType.PRESENCE_ANNOUNCEMENT, data);
	}

	public record Data(
			SemanticVersion protocolVersion,
			String instanceIdentifier,
			String name,
			int port,
			boolean acceptsAnyTasks) { }
}
