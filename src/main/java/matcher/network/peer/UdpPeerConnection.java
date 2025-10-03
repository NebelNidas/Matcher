package matcher.network.peer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

import matcher.network.NetworkHandler;
import matcher.network.packet.p2p.PresenceAnnouncement;

import org.jetbrains.annotations.Nullable;

public class UdpPeerConnection implements PeerConnection {
	public UdpPeerConnection(
			NetworkHandler networkHandler,
			NetworkInterface networkInterface,
			InetAddress networkInterfaceAddress,
			InetSocketAddress socketAddress,
			DatagramSocket socket,
			long packetReceiveTime,
			DatagramPacket lastAnnouncementPacket) {
		this.networkHandler = networkHandler;
		this.networkInterface = networkInterface;
		this.networkInterfaceAddress = networkInterfaceAddress;
		this.socketAddress = socketAddress;
		this.socket = socket;
		this.packetReceiveTime = packetReceiveTime;
		this.udpPacket = lastAnnouncementPacket;
	}

	@Override
	public NetworkInterface getNetworkInterface() {
		return networkInterface;
	}

	@Override
	public InetAddress getNetworkInterfaceAddress() {
		return networkInterfaceAddress;
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		return socketAddress;
	}

	public DatagramSocket getSocket() {
		return socket;
	}

	@Override
	public long getLastSignOfLifeTime() {
		return packetReceiveTime;
	}

	public DatagramPacket getUdpPacket() {
		return udpPacket;
	}

	public void setUdpPacket(DatagramPacket udpPacket) {
		this.udpPacket = udpPacket;

		pojoPacket = null;
		packetReceiveTime = System.currentTimeMillis();
	}

	public PresenceAnnouncement.Data getAnnouncementData() {
		if (pojoPacket == null) {
			pojoPacket = networkHandler.getPacketMapper().fromUdpPacket(udpPacket, PresenceAnnouncement.class);
		}

		return pojoPacket.data();
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

	private final NetworkHandler networkHandler;
	private final NetworkInterface networkInterface;
	private final InetAddress networkInterfaceAddress;
	private final InetSocketAddress socketAddress;
	private final DatagramSocket socket;
	private long packetReceiveTime;
	private DatagramPacket udpPacket;
	private PresenceAnnouncement pojoPacket;
	private @Nullable Peer peer;
}
