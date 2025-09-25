package matcher.network.packet.p2p;

import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.p2p.Ping.Data;

public record Ping(PacketType type, Data data) implements Packet<Data> {
	public Ping() {
		this(PacketType.PING, new Data());
	}

	public record Data() { }
}
