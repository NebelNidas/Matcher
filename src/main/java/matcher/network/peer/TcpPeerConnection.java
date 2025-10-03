package matcher.network.peer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;

import matcher.network.NetworkHandler;
import org.jetbrains.annotations.Nullable;

import matcher.Matcher;
import matcher.network.packet.Packet;

public class TcpPeerConnection implements PeerConnection {
	public TcpPeerConnection(
			NetworkHandler networkHandler,
			NetworkInterface networkInterface,
			InetAddress networkInterfaceAddress,
			Socket socket,
			boolean initiatedByUs,
			long lastSignOfLifeTime) {
		this.networkHandler = networkHandler;
		this.networkInterface = networkInterface;
		this.networkInterfaceAddress = networkInterfaceAddress;
		this.remoteSocketAddress = (InetSocketAddress) socket.getRemoteSocketAddress();
		this.socket = socket;
		this.initiatedByUs = initiatedByUs;
		this.lastSignOfLifeTime = lastSignOfLifeTime;
	}

	public void send(Packet<?> packet) throws IOException {
		if (peer != null) {
			peer.send(packet);
		} else {
			send0(packet);
		}
	}

	void send0(Packet<?> packet) throws IOException {
		assert !socket.isClosed();

		try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
			writer.println(networkHandler.getPacketMapper().toString(packet));
		}
	}

	public void terminateConnection() {
		try {
			socket.close();
		} catch (IOException e) {
			Matcher.LOGGER.error("Failed to close socket to address {}:{} at network interface {}",
					remoteSocketAddress.getAddress(),
					remoteSocketAddress.getPort(),
					networkInterface.getDisplayName(),
					e);
		}

		if (peer != null) {
			peer.removeTcpConnection(this);

			if (peer.getTcpConnections().isEmpty() && peer.getUdpConnections().isEmpty()) {
				if (!networkHandler.getConnections().pendingPeers.remove(peer)) {
					networkHandler.getConnections().peersById.remove(peer.getInstanceId());
				}
			}

			peer = null;
		}

		networkHandler.getConnections().tcpConnsByRemoteAddrByNetItfAddrByNetItf
				.get(networkInterface)
				.get(networkInterfaceAddress)
				.remove(remoteSocketAddress);
	}

	@Override
	public NetworkInterface getNetworkInterface() {
		return networkInterface;
	}

	@Override
	public InetAddress getNetworkInterfaceAddress() {
		return networkInterfaceAddress;
	}

	public Socket getSocket() {
		return socket;
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		return remoteSocketAddress;
	}

	@Override
	public long getLastSignOfLifeTime() {
		return lastSignOfLifeTime;
	}

	public void setLastSignOfLifeTime(long time) {
		this.lastSignOfLifeTime = time;
	}

	public boolean isInitiatedByUs() {
		return initiatedByUs;
	}

	@Override
	public @Nullable Peer getPeer() {
		return peer;
	}

	public void setPeer(@Nullable Peer peer) {
		if (this.peer != null) {
			throw new IllegalStateException("Peer is already set");
		}

		this.peer = peer;
	}

	public boolean equalsConnectionWise(TcpPeerConnection other) {
		if (this == other) {
			return true;
		}

		return remoteSocketAddress.equals(other.remoteSocketAddress)
				&& networkInterface.equals(other.networkInterface)
				&& networkInterfaceAddress.equals(other.networkInterfaceAddress);
	}

	private final NetworkHandler networkHandler;
	private final NetworkInterface networkInterface;
	private final InetAddress networkInterfaceAddress;
	private final InetSocketAddress remoteSocketAddress;
	private final Socket socket;
	private final boolean initiatedByUs;
	private long lastSignOfLifeTime;
	private @Nullable Peer peer;
}
