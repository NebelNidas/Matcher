package matcher.network.packet.handler;

import matcher.network.ConnectedLanPeer;
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
	public void handlePacket(String json, ConnectedLanPeer client) {
		client.send(new Pong());
	}
}
