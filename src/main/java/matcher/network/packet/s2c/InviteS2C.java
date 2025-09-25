package matcher.network.packet.s2c;

import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.s2c.InviteS2C.Data;

public record InviteS2C(PacketType type, Data data) implements Packet<Data> {
	public InviteS2C(Data data) {
		this(PacketType.INVITE_S2C, data);
	}

	public record Data(String invitor, String info) { }
}
