package matcher.network;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.value.ObservableStringValue;

import matcher.Matcher;
import matcher.network.packet.p2p.PresenceAnnouncement;

/**
 * Sends UDP multicasts to notify other Matcher instances in the same
 * local network of this instance's presence and how to connect to it.
 */
public class LanPresenceAnnouncer extends Thread {
	private final NetworkHandler networkHandler;
	private final NetworkInterface networkInterface;
	private final List<InetAddress> addressesToTry;
	private final int tcpPortToAnnounce;
	private final ObservableStringValue name;
	private final InetAddress multicastAddress;
	private DatagramSocket socket;
	private boolean running;
	private DatagramPacket packet;
	private int port;

	public LanPresenceAnnouncer(
			NetworkHandler networkHandler,
			NetworkInterface networkInterface,
			List<InetAddress> addressesToTry,
			int tcpPortToAnnounce,
			ObservableStringValue name) throws IOException {
		super("PresenceAnnouncer on network interface \"" + networkInterface.getDisplayName() + "\"");
		this.networkHandler = networkHandler;
		this.networkInterface = networkInterface;
		this.addressesToTry = new ArrayList<>(addressesToTry);
		this.tcpPortToAnnounce = tcpPortToAnnounce;
		this.name = name;
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
		byte[] packetBytes = networkHandler.getPacketMapper().toBytes(packet);
		this.packet = new DatagramPacket(packetBytes, packetBytes.length, multicastAddress, NetworkConstants.MULTICAST_PORT);
	}

	@Override
	public void run() {
		running = true;

		if (!tryNextBind()) {
			return;
		}

		regeneratePacket();
		Matcher.LOGGER.info("Announcing presence on the network (username: {}, multicast port: {}; interval: {} ms)",
				name.get(),
				socket.getLocalPort(),
				NetworkConstants.MULTICAST_INTERVAL_MS);

		while (!isInterrupted()) {
			try {
				socket.send(packet);
			} catch (BindException e) {
				if (!tryNextBind()) {
					break;
				}
			} catch (IOException e) {
				Matcher.LOGGER.warn("Error while announcing presence on the network", e);
				break;
			}

			try {
				Thread.sleep(NetworkConstants.MULTICAST_INTERVAL_MS);
			} catch (InterruptedException ignored) {
				// ignore
			}
		}
	}

	private boolean tryNextBind() {
		if (addressesToTry.isEmpty()) {
			Matcher.LOGGER.warn("Failed to announce presence on the network: None of the current network interface's addresses could be bound to a socket.");
			networkHandler.getPeerDetectorsByNetworkItf().remove(networkInterface);
			return false;
		} else {
			if (socket != null) {
				socket.close();
			}

			InetAddress nextAddress = addressesToTry.remove(0);

			if (nextAddress.isLoopbackAddress()) {
				return tryNextBind();
			}

			try {
				socket = new DatagramSocket(port, nextAddress);
				port = socket.getLocalPort();
			} catch (SocketException e) {
				tryNextBind();
			}
		}

		return true;
	}

	@Override
	public void interrupt() {
		super.interrupt();
		running = false;
	}

	int getPort() {
		return port;
	}

	public boolean isRunning() {
		return running;
	}
}
