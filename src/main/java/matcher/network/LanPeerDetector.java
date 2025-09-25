package matcher.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import matcher.Matcher;

public class LanPeerDetector extends Thread {
	private static final AtomicInteger THREAD_ID = new AtomicInteger();
	private final NetworkHandler networkHandler;
	private final Predicate<InetSocketAddress> isOwnSendingSocket;
	private final InetAddress multicastAddress;
	private final MulticastSocket socket;
	private boolean running;

	public LanPeerDetector(NetworkHandler networkHandler, Predicate<InetSocketAddress> isOwnSendingSocket) throws IOException {
		super("PeerDetector #" + THREAD_ID.incrementAndGet());
		this.setDaemon(true);
		this.networkHandler = networkHandler;
		this.isOwnSendingSocket = isOwnSendingSocket;
		this.socket = new MulticastSocket(NetworkConstants.MULTICAST_PORT);
		this.multicastAddress = InetAddress.getByName(NetworkConstants.MULTICAST_ADDRESS);
		this.socket.setSoTimeout(NetworkConstants.MULTICAST_INTERVAL_MS * 2);
		this.socket.joinGroup(multicastAddress);
	}

	@Override
	public void run() {
		running = true;
		byte[] buffer = new byte[1024];

		while (!isInterrupted()) {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			try {
				socket.receive(packet);
			} catch (SocketTimeoutException ignored) {
				continue;
			} catch (IOException e) {
				running = false;
				Matcher.LOGGER.error("Error while searching for peers on the network", e);
				break;
			}

			InetSocketAddress address = (InetSocketAddress) packet.getSocketAddress();

			if (isOwnSendingSocket.test(address)) {
				continue;
			}

			LanPeer peer = networkHandler.getConnections().udpPeersByAddress.get(address);
			boolean isNewPeer = peer == null;

			if (isNewPeer) {
				networkHandler.getConnections().udpPeersByAddress.put(address, peer = new LanPeer(networkHandler, address.getAddress(), System.currentTimeMillis(), packet));
			}

			peer.setLastSeen(System.currentTimeMillis());
			peer.setUdpPacket(packet);

			if (isNewPeer) {
				Matcher.LOGGER.info("Discovered new LAN peer: {}:{} ({})",
						address.getAddress(),
						peer.getAnnouncement().data().port(),
						peer.getAnnouncement().data().name());
				try {
					networkHandler.connectToPeer(peer);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		try {
			socket.leaveGroup(multicastAddress);
		} catch (IOException ignored) {
			// ignore
		}

		socket.close();
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
