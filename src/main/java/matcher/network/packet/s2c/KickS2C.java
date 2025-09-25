package matcher.network.packet.s2c;

import org.jetbrains.annotations.Nullable;

import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.s2c.KickS2C.Data;

public record KickS2C(PacketType type, Data data) implements Packet<Data> {
	public KickS2C(Data data) {
		this(PacketType.KICK_S2C, data);
	}

	public record Data(String initiator, @Nullable String reason) { }
}
