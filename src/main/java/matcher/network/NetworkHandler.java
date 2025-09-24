package matcher.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import org.jetbrains.annotations.Nullable;

import matcher.Matcher;
import matcher.Util;
import matcher.network.packet.PacketMapper;
import matcher.network.packet.PacketType;

public class NetworkHandler {
	private static final ExecutorService connectionThreadPool = Executors.newCachedThreadPool();
	private static final PacketMapper mapper = PacketMapper.getInstance();
	private final Matcher matcher;
	private final ConnectionStore connections;
	private final SimpleStringProperty hostname;
	private final Set<String> hostAddresses;
	private @Nullable LanPresenceAnnouncer announcer;
	private @Nullable ServerSocket serverSocket;
	private @Nullable LanPeerDetector peerDetector;
	private Thread taskExecutorThread;
	private Thread connectionListenerThread;
	private BlockingQueue<Runnable> tasks = new LinkedBlockingDeque<>();
	private int localPort;

	public NetworkHandler(Matcher matcher) {
		this.matcher = matcher;

		connections = new ConnectionStore();
		hostname = new SimpleStringProperty(Util.getComputerName());
		hostAddresses = NetworkUtil.getHostAddresses();

		localPort = -1;
	}

	void runOnThread(Runnable task) {
		tasks.add(task);
	}

	public void startPeerScanning() {
		try {
			if (peerDetector == null) {
				peerDetector = new LanPeerDetector(this, address -> announcer != null
						&& address.getPort() == announcer.getPort()
						&& hostAddresses.contains(address.getAddress().getHostAddress()));
			}

			peerDetector.start();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void stopPeerScanning() {
		if (peerDetector != null) {
			peerDetector.interrupt();
			peerDetector = null;
		}
	}

	public void openToLan() {
		try {
			serverSocket = new ServerSocket(0);
			localPort = serverSocket.getLocalPort();
			announcer = new LanPresenceAnnouncer(localPort, hostname);
			announcer.start();

			taskExecutorThread = new Thread(() -> {
				while (true) {
					try {
						tasks.take().run();
					} catch (InterruptedException e) {
						break;
					}
				}
			}, "Network task executor");
			taskExecutorThread.setDaemon(true);
			taskExecutorThread.start();

			connectionListenerThread = new Thread(() -> {
				try {
					while (!serverSocket.isClosed()) {
						acceptClient();
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}, "Network connection listener");
			connectionListenerThread.setDaemon(true);
			connectionListenerThread.start();

			Matcher.LOGGER.info("Opened to LAN on port {}", localPort);
		} catch (IOException e) {
			localPort = -1;
			Matcher.LOGGER.error("Failed to open to LAN on port {}", localPort, e);
		}
	}

	private void acceptClient() throws IOException {
		assert serverSocket != null;
		Socket client = serverSocket.accept();
		LanPeer udpPeer = connections.udpPeersByAddress.get(client.getRemoteSocketAddress());

		ConnectedLanPeer connectedPeer = new ConnectedLanPeer(this, client, System.currentTimeMillis(),
				udpPeer == null ? null : udpPeer.getUdpPacket());
		connections.tcpPeersByAddress.put(client.getRemoteSocketAddress(), connectedPeer);
		Matcher.LOGGER.info("Accepted connection from {}", client.getRemoteSocketAddress());

		connectionThreadPool.submit(() -> {
			try {
				String message = new BufferedReader(new InputStreamReader(client.getInputStream())).lines().collect(Collectors.joining());
				PacketType packetType = mapper.getType(message);

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	Socket connectToPeer(LanPeer peer) throws IOException {
		Socket socket = new Socket(peer.getAddress(), peer.getAnnouncement().data().port());
		ConnectedLanPeer tcpPeer = new ConnectedLanPeer(this, socket, System.currentTimeMillis(), peer.getUdpPacket());
		connections.tcpPeersByAddress.put(socket.getRemoteSocketAddress(), tcpPeer);
		return socket;
	}

	public @Nullable String getHostname() {
		return hostname.get();
	}

	public void setHostname(@Nullable String hostname) {
		this.hostname.set(hostname);
	}

	public ConnectionStore getConnections() {
		return connections;
	}
}
