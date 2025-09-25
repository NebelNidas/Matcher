package matcher.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import matcher.Matcher;

public class LanPeerDetector extends Thread {
	private final NetworkHandler networkHandler;
	private final NetworkInterface networkInterface;
	private final List<InetAddress> addressesToTry;
	private final BiPredicate<NetworkInterface, InetSocketAddress> isOwnSendingSocket;
	private final InetAddress multicastAddress;
	private MulticastSocket socket;
	private boolean running;

	public LanPeerDetector(
			NetworkHandler networkHandler,
			NetworkInterface networkInterface,
			List<InetAddress> addressesToTry,
			BiPredicate<NetworkInterface, InetSocketAddress> isOwnSendingSocket) throws IOException {
		super("PeerDetector on network interface \"" + networkInterface.getDisplayName() + "\"");
		this.networkHandler = networkHandler;
		this.networkInterface = networkInterface;
		this.addressesToTry = new ArrayList<>(addressesToTry);
		this.isOwnSendingSocket = isOwnSendingSocket;
		this.multicastAddress = InetAddress.getByName(NetworkConstants.MULTICAST_ADDRESS);

		setDaemon(true);
	}

	@Override
	public void run() {
		running = true;
		byte[] buffer = new byte[1024];

		if (!tryNextBind()) {
			return;
		}

		while (!isInterrupted()) {
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

			try {
				socket.receive(packet);
			} catch (SocketTimeoutException ignored) {
				continue;
			} catch (IOException e) {
				if (!tryNextBind()) {
					return;
				}
			}

			InetSocketAddress address = (InetSocketAddress) packet.getSocketAddress();

			if (isOwnSendingSocket.test(networkInterface, address)) {
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

	private boolean tryNextBind() {
		if (addressesToTry.isEmpty()) {
			Matcher.LOGGER.warn("Unable to search fo peers on the network: None of the current network interface's addresses could be bound to a socket.");
			networkHandler.getAnnouncersByNetworkItf().remove(networkInterface);
			running = false;
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
				socket = new MulticastSocket(new InetSocketAddress(nextAddress, NetworkConstants.MULTICAST_PORT));
				socket.setSoTimeout(NetworkConstants.MULTICAST_INTERVAL_MS * 2);
				socket.joinGroup(multicastAddress);
			} catch (IOException e) {
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

	public boolean isRunning() {
		return running;
	}
}
