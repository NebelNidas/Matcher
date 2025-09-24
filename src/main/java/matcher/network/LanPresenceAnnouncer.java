package matcher.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.value.ObservableStringValue;

import matcher.Matcher;
import matcher.network.packet.PacketMapper;
import matcher.network.packet.broadcast.PresenceAnnouncement;

/**
 * Sends UDP multicasts to notify other Matcher instances in the same
 * local network of this instance's presence and how to connect to it.
 */
public class LanPresenceAnnouncer extends Thread {
	private static final AtomicInteger THREAD_ID = new AtomicInteger();
	private final int tcpPortToAnnounce;
	private final ObservableStringValue name;
	private final DatagramSocket socket;
	private final InetAddress multicastAddress;
	private boolean running;
	private DatagramPacket packet;

	public LanPresenceAnnouncer(int tcpPortToAnnounce, ObservableStringValue name) throws IOException {
		super("PresenceAnnouncer #" + THREAD_ID.incrementAndGet());
		this.tcpPortToAnnounce = tcpPortToAnnounce;
		this.name = name;
		this.socket = new DatagramSocket();
		this.multicastAddress = InetAddress.getByName(NetworkConstants.MULTICAST_ADDRESS);

		setDaemon(true);
		name.addListener((obs, oldName, newName) -> {
			if (running && !oldName.equals(newName)) {
				regeneratePacket();
			}
		});
	}

	private void regeneratePacket() {
		PresenceAnnouncement packet = new PresenceAnnouncement(new PresenceAnnouncement.Data(
				NetworkConstants.PROTOCOL_VERSION,
				name.get(),
				tcpPortToAnnounce,
				true));
		byte[] packetBytes = PacketMapper.getInstance().toBytes(packet);
		this.packet = new DatagramPacket(packetBytes, packetBytes.length, multicastAddress, NetworkConstants.MULTICAST_PORT);
	}

	@Override
	public void run() {
		running = true;
		regeneratePacket();
		Matcher.LOGGER.info("Announcing presence on the network (name: {}, multicast port: {}; interval: {} ms)",
				name.get(),
				socket.getLocalPort(),
				NetworkConstants.MULTICAST_INTERVAL_MS);

		while (!isInterrupted()) {
			try {
				socket.send(packet);
			} catch (IOException e) {
				Matcher.LOGGER.warn("Error while announcing presence on the network", e);
				break;
			}

			try {
				Thread.sleep(NetworkConstants.MULTICAST_INTERVAL_MS);
			} catch (InterruptedException ignored) {
			}
		}
	}

	@Override
	public void interrupt() {
		super.interrupt();
		running = false;
	}

	int getPort() {
		return socket.getLocalPort();
	}

	public boolean isRunning() {
		return running;
	}
}
