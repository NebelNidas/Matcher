package matcher.network.peer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import matcher.Matcher;
import matcher.network.NetworkHandler;
import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.s2c.KickS2C;

public class Peer {
	public Peer(NetworkHandler networkHandler) {
		this.networkHandler = networkHandler;

		udpConnections = Collections.synchronizedList(new ArrayList<>());
		tcpConnections = Collections.synchronizedList(new ArrayList<>());
	}

	public Peer(NetworkHandler networkHandler, @Nullable UdpPeerConnection udpConnection, @Nullable TcpPeerConnection tcpConnection) {
		this(networkHandler);

		if (udpConnection != null) {
			udpConnections.add(udpConnection);
		}

		if (tcpConnection != null) {
			tcpConnections.add(tcpConnection);
		}
	}

	public boolean send(Packet<?> packet) {
		sortTcpConnections();
		boolean sent = false;

		for (TcpPeerConnection tcpConnection : tcpConnections) {
			try {
				tcpConnection.send0(packet);
				sent = true;
				break;
			} catch (IOException e) {
				// try next connection
			}
		}

		return sent;
	}

	public void kick(@Nullable String reason) {
		send(new KickS2C(new KickS2C.Data("Admin", reason)));

		for (TcpPeerConnection tcpConnection : tcpConnections) {
			tcpConnection.terminateConnection();
		}
	}

	public List<UdpPeerConnection> getUdpConnections() {
		return Collections.unmodifiableList(udpConnections);
	}

	public void addUdpConnection(UdpPeerConnection udpConnection) {
		udpConnections.add(udpConnection);
	}

	public List<TcpPeerConnection> getTcpConnections() {
		return Collections.unmodifiableList(tcpConnections);
	}

	public boolean addTcpConnection(TcpPeerConnection tcpConnection) {
		return tcpConnections.add(tcpConnection);
	}

	public boolean removeTcpConnection(TcpPeerConnection tcpConnection) {
		return tcpConnections.remove(tcpConnection);
	}

	public String getInstanceId() {
		if (udpConnections.isEmpty()) {
			return null;
		}

		if (instanceId == null) {
			instanceId = udpConnections.get(0).getAnnouncementData().instanceIdentifier();
		}

		if (networkHandler.getMatcher().debugMode) {
			for (UdpPeerConnection udpConnection : udpConnections) {
				assert udpConnection.getAnnouncementData().instanceIdentifier().equals(instanceId);
			}
		}

		return instanceId;
	}

	public String getAnnouncedName() {
		sortUdpConnections();
		return udpConnections.get(0).getAnnouncementData().name();
	}

	private void sortUdpConnections() {
		udpConnections.sort(MOST_RECENTLY_ACTIVE_COMPARATOR);
	}

	private void sortTcpConnections() {
		tcpConnections.sort(MOST_RECENTLY_ACTIVE_COMPARATOR);
	}

	private static final Comparator<PeerConnection> MOST_RECENTLY_ACTIVE_COMPARATOR =
			(a, b) -> Long.compare(b.getLastSignOfLifeTime(), a.getLastSignOfLifeTime());
	private final NetworkHandler networkHandler;
	private final List<UdpPeerConnection> udpConnections;
	private final List<TcpPeerConnection> tcpConnections;
	private String instanceId;
}
