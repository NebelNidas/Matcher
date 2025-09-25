package matcher.network;

import java.net.DatagramPacket;
import java.net.InetAddress;

import matcher.network.packet.p2p.PresenceAnnouncement;

public class LanPeer {
	private final NetworkHandler networkHandler;
	private final InetAddress address;
	private long lastSeen;
	private DatagramPacket udpPacket;
	private PresenceAnnouncement pojoPacket;

	LanPeer(NetworkHandler networkHandler, InetAddress address, long lastSeen, DatagramPacket udpPacket) {
		this.networkHandler = networkHandler;
		this.address = address;
		this.lastSeen = lastSeen;
		this.udpPacket = udpPacket;
	}

	public InetAddress getAddress() {
		return address;
	}

	public long getLastSeen() {
		return lastSeen;
	}

	void setLastSeen(long time) {
		this.lastSeen = time;
	}

	public PresenceAnnouncement getAnnouncement() {
		if (pojoPacket == null) {
			pojoPacket = networkHandler.getPacketMapper().fromUdpPacket(udpPacket, PresenceAnnouncement.class);
		}

		return pojoPacket;
	}

	public DatagramPacket getUdpPacket() {
		return udpPacket;
	}

	void setUdpPacket(DatagramPacket udpPacket) {
		this.udpPacket = udpPacket;
		this.pojoPacket = null;
	}
}
