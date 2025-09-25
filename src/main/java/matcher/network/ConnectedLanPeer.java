package matcher.network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.Socket;

import org.jetbrains.annotations.Nullable;

import matcher.Matcher;
import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.s2c.KickS2C;

public class ConnectedLanPeer extends LanPeer {
	ConnectedLanPeer(NetworkHandler networkHandler, Socket socket, long lastSeen, DatagramPacket udpPacket) {
		super(networkHandler, socket.getInetAddress(), lastSeen, udpPacket);

		this.networkHandler = networkHandler;
		this.socket = socket;
	}

	public void send(Packet<?> packet) {
		if (socket.isClosed()) {
			return;
		}

		try {
			new PrintWriter(socket.getOutputStream(), true).println(networkHandler.getPacketMapper().toString(packet));
		} catch (IOException e) {
			if (packet.type() != PacketType.KICK_S2C) {
				kick("Server error: " + e.getMessage());
				Matcher.LOGGER.error("Failed to send {} packet to {}:{}", packet.getClass().getSimpleName(), socket.getInetAddress(), socket.getPort(), e);
			}
		}
	}

	public void kick(@Nullable String reason) {
		KickS2C packet = new KickS2C(new KickS2C.Data("Admin", reason));
		send(packet);

		try {
			networkHandler.getConnections().tcpPeersByAddress.remove(socket.getRemoteSocketAddress());
			socket.close();
		} catch (IOException e) {
			String username = getAnnouncement().data().name();
			Matcher.LOGGER.error("Exception while closing connection with {} ({}:{})", username, socket.getInetAddress(), socket.getPort(), e);
		}
	}

	public Socket getSocket() {
		return socket;
	}

	private final NetworkHandler networkHandler;
	private final Socket socket;
}
