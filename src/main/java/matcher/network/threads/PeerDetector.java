package matcher.network.threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import matcher.Matcher;
import matcher.Util;
import matcher.network.packet.PacketType;
import matcher.network.packet.p2p.PresenceAnnouncement;
import matcher.network.peer.UdpPeerConnection;
import matcher.network.NetworkConstants;
import matcher.network.NetworkHandler;

/**
 * Listens for UDP multicasts from other Matcher instances in a given
 * network interface's network to discover LAN peers.
 */
public class PeerDetector extends Thread {
	private static final AtomicInteger THREAD_ID = new AtomicInteger();
	private final NetworkHandler networkHandler;
	private final NetworkInterface networkInterface;
	private final InetAddress localAddress;
	private MulticastSocket socket;
	private boolean running;
	private int consecutiveFailedAccepts;
	private Exception lastError;

	public PeerDetector(
			NetworkHandler networkHandler,
			NetworkInterface networkInterface,
			InetAddress localAddress) throws IOException {
		super("PeerDetector #" + THREAD_ID.incrementAndGet());
		this.networkHandler = networkHandler;
		this.networkInterface = networkInterface;
		this.localAddress = localAddress;

		setDaemon(true);
	}

	@Override
	public void run() {
		running = true;
		byte[] buffer = new byte[1024];

		while (!isInterrupted()) {
			if (socket == null || socket.isClosed()) {
				try {
					if (!networkInterface.isUp()) {
						handleError(null, () -> Matcher.LOGGER.warn("Network interface {} is down, cannot search for peers at local address {}",
								networkInterface.getDisplayName(),
								localAddress));
						continue;
					}

					socket = new MulticastSocket(new InetSocketAddress(localAddress, NetworkConstants.MULTICAST_PORT));
					socket.setSoTimeout(NetworkConstants.MULTICAST_INTERVAL_MS * 2);
					socket.joinGroup(new InetSocketAddress(NetworkConstants.MULTICAST_ADDRESS, NetworkConstants.MULTICAST_PORT), networkInterface);
					consecutiveFailedAccepts = 0;
					Matcher.LOGGER.info("Searching for peers at local address {}:{} through network interface {}",
							localAddress,
							NetworkConstants.MULTICAST_PORT,
							networkInterface.getDisplayName());
				} catch (IOException e) {
					handleError(e, () -> {
						Matcher.LOGGER.warn("Error while binding PeerDetector socket to local address {}:{} through network interface {}",
								localAddress,
								NetworkConstants.MULTICAST_PORT,
								networkInterface.getDisplayName(),
								e);
					});
				}
			}

			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			try {
				socket.receive(packet);
			} catch (SocketTimeoutException ignored) {
				continue;
			} catch (IOException e) {
				handleError(e, () -> Matcher.LOGGER.warn("Error while searching for peers at local address {}:{} through network interface {}",
						localAddress,
						NetworkConstants.MULTICAST_PORT,
						networkInterface.getDisplayName(),
						e));
			}

			consecutiveFailedAccepts = 0;
			InetSocketAddress remoteAddress = (InetSocketAddress) packet.getSocketAddress();


			if (originatesFromUs(remoteAddress)
					|| networkHandler.getPacketMapper().getType(packet) != PacketType.PRESENCE_ANNOUNCEMENT) {
				continue;
			}

			UdpPeerConnection udpConnection = networkHandler.getConnections().incomingUdpConnsByRemoteAddrByNetItfAddrByNetItf
					.computeIfAbsent(networkInterface, k -> new ConcurrentHashMap<>())
					.computeIfAbsent(localAddress, k -> new ConcurrentHashMap<>())
					.get(remoteAddress);
			boolean isNewConn = udpConnection == null;

			if (isNewConn) {
				networkHandler.getConnections().incomingUdpConnsByRemoteAddrByNetItfAddrByNetItf
						.computeIfAbsent(networkInterface, k -> new ConcurrentHashMap<>())
						.computeIfAbsent(localAddress, k -> new ConcurrentHashMap<>())
						.put(remoteAddress, udpConnection = new UdpPeerConnection(
								networkHandler,
								networkInterface,
								localAddress,
								remoteAddress,
								socket,
								System.currentTimeMillis(),
								packet));
			}

			udpConnection.setUdpPacket(packet);
			PresenceAnnouncement.Data announcementData = udpConnection.getAnnouncementData();
			assert !announcementData.instanceIdentifier().equals(NetworkHandler.INSTANCE_ID);

			if (isNewConn) {
				boolean isNewPeer = !networkHandler.getConnections().peersById.containsKey(announcementData.instanceIdentifier());

				Matcher.LOGGER.info("Discovered new {}LAN peer {} (id: {}) at address {}:{} via local address {}:{} through network interface {}",
						isNewPeer ? "" : "connection to known ",
						announcementData.name(),
						announcementData.instanceIdentifier(),
						remoteAddress.getAddress(),
						announcementData.port(),
						localAddress,
						socket.getLocalPort(),
						networkInterface.getDisplayName());
				UdpPeerConnection finalUdpConnection = udpConnection;

				networkHandler.runOnThread(() -> {
					try {
						networkHandler.connectToPeer(finalUdpConnection);
					} catch (IOException e) {
						Matcher.LOGGER.warn("Failed to connect to peer {} (id: {}) at remote address {}:{} via local address {} through network interface {}",
								announcementData.name(),
								announcementData.instanceIdentifier(),
								remoteAddress.getAddress(),
								announcementData.port(),
								localAddress,
								networkInterface.getDisplayName(),
								e);
					}
				});
			}
		}

		Matcher.LOGGER.debug("PeerDetector at local address {}:{} through network interface {} stopped",
				localAddress,
				NetworkConstants.MULTICAST_PORT,
				networkInterface.getDisplayName());

		try {
			socket.leaveGroup(new InetSocketAddress(NetworkConstants.MULTICAST_ADDRESS, NetworkConstants.MULTICAST_PORT), networkInterface);
		} catch (IOException e) {
			Matcher.LOGGER.debug("Error while closing PeerDetector socket at local address {}:{} through network interface {}",
					localAddress,
					NetworkConstants.MULTICAST_PORT,
					networkInterface.getDisplayName(),
					e);
		}

		socket.close();
		running = false;
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

	private boolean originatesFromUs(InetSocketAddress remoteAddress) {
		PresenceAnnouncer announcer = networkHandler.getAnnouncersByAddressByNetworkItf()
				.getOrDefault(networkInterface, Map.of())
				.get(remoteAddress.getAddress());
		// Matcher.LOGGER.trace("{}, {}, {}, {}", networkInterface.getDisplayName(), localAddress, remoteAddress, announcer);
		return announcer != null && remoteAddress.getPort() == announcer.getPort();
	}

	@Override
	public void interrupt() {
		super.interrupt();
		running = false;
	}

	public boolean isRunning() {
		return running;
	}
}
