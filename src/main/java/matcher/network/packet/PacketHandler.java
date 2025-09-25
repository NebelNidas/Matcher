package matcher.network.packet;

import matcher.network.ConnectedLanPeer;

public interface PacketHandler<T extends Packet<?>> {
	PacketType getPacketType();
	void handlePacket(String json, ConnectedLanPeer client) throws Exception;
}
