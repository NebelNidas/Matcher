package matcher.network.threads;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import javafx.beans.value.ObservableStringValue;

import javafx.beans.value.WeakChangeListener;

import matcher.Matcher;
import matcher.Util;
import matcher.network.NetworkConstants;
import matcher.network.NetworkHandler;
import matcher.network.packet.p2p.PresenceAnnouncement;

/**
 * Sends UDP multicasts to notify other Matcher instances in a given
 * network interface's network of this instance's presence and how
 * to connect to it.
 */
public class PresenceAnnouncer extends Thread {
	private static final AtomicInteger THREAD_ID = new AtomicInteger();
	private final NetworkHandler networkHandler;
	private final NetworkInterface networkInterface;
	private final ObservableStringValue name;
	private final InetAddress address;
	private DatagramSocket socket;
	private int port;
	private int tcpPortToAnnounce;
	private DatagramPacket packet;
	private boolean running;
	private int consecutiveFailedAccepts;
	private Exception lastError;

	public PresenceAnnouncer(
			NetworkHandler networkHandler,
			NetworkInterface networkInterface,
			InetAddress address,
			ObservableStringValue name) {
		super("PresenceAnnouncer #" + THREAD_ID.incrementAndGet());
		this.networkHandler = networkHandler;
		this.networkInterface = networkInterface;
		this.address = address;
		this.name = name;

		setDaemon(true);
		name.addListener(new WeakChangeListener<>((obs, oldName, newName) -> {
			if (running && !oldName.equals(newName)) {
				regeneratePacket();
			}
		}));
	}

	@Override
	public void run() {
		running = true;

		synchronized (this) {
			if (tcpPortToAnnounce <= 0) {
				// Wait until the corresponding PeerAcceptor is ready and tells us the TCP port to announce
				try {
					wait();
					assert tcpPortToAnnounce > 0;
				} catch (InterruptedException e) {
					return;
				}
			}
		}

		regeneratePacket();

		while (!isInterrupted()) {
			if (socket == null || socket.isClosed()) {
				try {
					if (!networkInterface.isUp()) {
						handleError(null, () -> Matcher.LOGGER.warn("Network interface {} is down, cannot announce presence at address {}",
								networkInterface.getDisplayName(),
								address));
					}

					socket = new DatagramSocket(0, address);
					port = socket.getLocalPort();
					consecutiveFailedAccepts = 0;
					Matcher.LOGGER.info("Announcing address {}:{} through network interface {} (multicast port: {})",
							address,
							tcpPortToAnnounce,
							networkInterface.getDisplayName(),
							socket.getLocalPort());
				} catch (SocketException e) {
					handleError(e, () -> {
						Matcher.LOGGER.warn("Error while binding PresenceAnnouncer socket at address {} through network interface {}",
								address,
								networkInterface.getDisplayName(),
								e);
					});
				}
			}

			try {
				socket.send(packet);
				consecutiveFailedAccepts = 0;
			} catch (IOException e) {
				handleError(e, () -> Matcher.LOGGER.warn("Error while announcing presence at address {}:{} through network interface {}",
					address,
					port,
					networkInterface.getDisplayName(),
					e));
			}

			try {
				Thread.sleep(NetworkConstants.MULTICAST_INTERVAL_MS);
			} catch (InterruptedException ignored) {
				// ignore
			}
		}

		Matcher.LOGGER.debug("PresenceAnnouncer at address {}:{} through network interface {} stopped",
				address,
				port,
				networkInterface.getDisplayName());

		if (!socket.isClosed()) {
			socket.close();
		}

		port = 0;
		running = false;
	}

	private void regeneratePacket() {
		PresenceAnnouncement packet = new PresenceAnnouncement(new PresenceAnnouncement.Data(
				NetworkConstants.PROTOCOL_VERSION,
				NetworkHandler.INSTANCE_ID,
				name.get(),
				tcpPortToAnnounce,
				true));
		byte[] packetBytes = networkHandler.getPacketMapper().toBytes(packet);
		this.packet = new DatagramPacket(packetBytes, packetBytes.length, NetworkConstants.MULTICAST_ADDRESS, NetworkConstants.MULTICAST_PORT);
	}

	private void handleError(Exception error, Runnable logAction) {
		if (consecutiveFailedAccepts == 0 || !Util.equals(error, lastError)) {
			logAction.run();
		}

		consecutiveFailedAccepts++;
		lastError = error;

		if (consecutiveFailedAccepts >= 2) {
			int secondsToWait = Math.min(60, (consecutiveFailedAccepts - 1) * 10);
			Matcher.LOGGER.debug("Waiting {} seconds before retrying", secondsToWait);
			LockSupport.parkNanos(secondsToWait * 1_000_000_000L);
		}
	}

	@Override
	public void interrupt() {
		super.interrupt();
		running = false;
	}

	public InetAddress getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	void setTcpPortToAnnounce(int tcpPortToAnnounce) {
		assert tcpPortToAnnounce > 0;
		assert this.tcpPortToAnnounce <= 0;

		synchronized (this) {
			this.tcpPortToAnnounce = tcpPortToAnnounce;
			notifyAll();
		}
	}

	public boolean isRunning() {
		return running;
	}
}
