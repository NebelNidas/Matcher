package matcher.network.packet.c2s;

import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.c2s.InviteResponseC2S.Data;

public record InviteResponseC2S(PacketType type, Data data) implements Packet<Data> {
	public InviteResponseC2S(Data data) {
		this(PacketType.INVITE_RESPONSE_C2S, data);
	}

	public record Data(boolean accept) { }
}
