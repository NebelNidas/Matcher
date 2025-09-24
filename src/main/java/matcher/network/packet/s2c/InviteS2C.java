package matcher.network.packet.s2c;

import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.s2c.InviteS2C.Data;

public record InviteS2C(Data data) implements Packet<Data> {
	@Override
	public PacketType type() {
		return PacketType.INVITE_S2C;
	}

	public record Data(String invitor, String info) { }
}
