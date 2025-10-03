package matcher.network.packet.handler;

import matcher.network.peer.Peer;
import matcher.network.peer.TcpPeerConnection;
import matcher.network.NetworkHandler;
import matcher.network.packet.PacketHandler;
import matcher.network.packet.PacketType;
import matcher.network.packet.p2p.Ping;
import matcher.network.packet.p2p.Pong;

public class PingHandler implements PacketHandler<Ping> {
	private final NetworkHandler networkHandler;

	public PingHandler(NetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public PacketType getPacketType() {
		return PacketType.PING;
	}

	@Override
	public void handlePacket(String json, Peer peer, TcpPeerConnection connection) {
		peer.send(new Pong());
	}
}
