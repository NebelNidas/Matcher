package matcher.network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Socket;

import matcher.Matcher;
import matcher.network.packet.Packet;
import matcher.network.packet.PacketMapper;
import matcher.network.packet.PacketType;
import matcher.network.packet.s2c.KickS2C;

import org.jetbrains.annotations.Nullable;

public class ConnectedLanPeer extends LanPeer {
	ConnectedLanPeer(NetworkHandler networkHandler, Socket socket, long lastSeen, DatagramPacket udpPacket) {
		super(socket.getInetAddress(), lastSeen, udpPacket);

		this.networkHandler = networkHandler;
		this.socket = socket;
	}

	public void send(Packet<?> packet) {
		if (socket.isClosed()) {
			return;
		}

		try {
			new DataOutputStream(socket.getOutputStream()).writeUTF(mapper.toString(packet));
		} catch (IOException e) {
			if (packet.type() != PacketType.KICK_S2C) {
				kick("Server error: " + e.getMessage());
				Matcher.LOGGER.error("Failed to send {} packet to {}:{}", packet.getClass().getSimpleName(), socket.getInetAddress(), socket.getPort(), e);
			}
		}
	}

	void kick(@Nullable String reason) {
		KickS2C packet = new KickS2C(new KickS2C.Data("Admin", reason));
		send(socket, packet);

		try {
			networkHandler.getConnections().tcpPeersByAddress.remove(socket.getRemoteSocketAddress());
			socket.close();
		} catch (IOException e) {
			String username = getAnnouncement().data().name();
			Matcher.LOGGER.error("Exception while closing connection with {} ({}:{})", username, socket.getInetAddress(), socket.getPort(), e);
		}
	}

	private static final PacketMapper mapper = PacketMapper.getInstance();
	private final NetworkHandler networkHandler;
	private Socket socket;
}
