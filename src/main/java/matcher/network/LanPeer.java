package matcher.network;

import java.net.DatagramPacket;
import java.net.InetAddress;

import matcher.network.packet.PacketMapper;
import matcher.network.packet.broadcast.PresenceAnnouncement;

public class LanPeer {
	private final InetAddress address;
	private long lastSeen;
	private DatagramPacket udpPacket;
	private PresenceAnnouncement pojoPacket;

	LanPeer(InetAddress address, long lastSeen, DatagramPacket udpPacket) {
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
			pojoPacket = PacketMapper.getInstance().fromUdpPacket(udpPacket, PresenceAnnouncement.class);
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
