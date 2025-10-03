package matcher.network.packet;

import matcher.network.peer.Peer;
import matcher.network.peer.TcpPeerConnection;

public interface PacketHandler<T extends Packet<?>> {
	PacketType getPacketType();
	void handlePacket(String json, Peer peer, TcpPeerConnection connection) throws Exception;
}
