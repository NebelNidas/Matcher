package matcher.network.packet.p2p;

import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.p2p.Pong.Data;

public record Pong(PacketType type, Data data) implements Packet<Data> {
	public Pong() {
		this(PacketType.PONG, new Data());
	}

	public record Data() { }
}
