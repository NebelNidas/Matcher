package matcher.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.SimpleStringProperty;
import org.jetbrains.annotations.Nullable;

import matcher.Matcher;
import matcher.Util;
import matcher.network.packet.PacketMapper;
import matcher.network.packet.PacketType;
import matcher.network.packet.handler.MatchClassesS2CHandler;
import matcher.network.packet.handler.MatchedClassesC2SHandler;
import matcher.network.packet.handler.PingHandler;

public class NetworkHandler {
	private static final ExecutorService connectionThreadPool = Executors.newCachedThreadPool();
	private final Matcher matcher;
	private final PacketMapper mapper;
	private final ConnectionStore connections;
	private final SimpleStringProperty hostname;
	private final Map<NetworkInterface, List<InetAddress>> hostAddresses;
	private final Map<NetworkInterface, LanPeerDetector> peerDetectorsByNetworkItf;
	private final Map<NetworkInterface, LanPresenceAnnouncer> announcersByNetworkItf;
	private final BlockingQueue<Runnable> tasks;
	private final Thread taskExecutorThread;
	private final AtomicBoolean shuttingDown;
	private final AtomicBoolean shutDown;
	private final PingHandler pingHandler;
	private final MatchClassesS2CHandler matchClassesS2CHandler;
	private final MatchedClassesC2SHandler matchedClassesC2SHandler;
	private @Nullable ServerSocket serverSocket;
	private @Nullable Thread connectionListenerThread;
	private int localPort;

	public NetworkHandler(Matcher matcher) {
		this.matcher = matcher;
		this.mapper = new PacketMapper(matcher, this);

		connections = new ConnectionStore();
		hostname = new SimpleStringProperty(Util.getComputerName());
		hostAddresses = NetworkUtil.getAllLocalAddresses();
		peerDetectorsByNetworkItf = new ConcurrentHashMap<>();
		announcersByNetworkItf = new ConcurrentHashMap<>();
		tasks = new LinkedBlockingDeque<>();
		taskExecutorThread = new Thread(() -> {
			while (true) {
				try {
					tasks.take().run();
				} catch (InterruptedException e) {
					break;
				}
			}
		}, "Network Task Executor");
		taskExecutorThread.setDaemon(true);
		taskExecutorThread.start();
		shuttingDown = new AtomicBoolean(false);
		shutDown = new AtomicBoolean(false);
		pingHandler = new PingHandler(this);
		matchClassesS2CHandler = new MatchClassesS2CHandler(this);
		matchedClassesC2SHandler = new MatchedClassesC2SHandler(this);

		localPort = -1;
	}

	public void runOnThread(Runnable task) {
		if (shuttingDown.get()) {
			throw new IllegalStateException("NetworkHandler is shutting down");
		} else if (shutDown.get()) {
			throw new IllegalStateException("NetworkHandler is shut down");
		}

		runOnThread0(task);
	}

	private void runOnThread0(Runnable task) {
		tasks.add(task);
	}

	public void startPeerScanning() {
		try {
			for (Map.Entry<NetworkInterface, List<InetAddress>> entry : hostAddresses.entrySet()) {
				LanPeerDetector detector = new LanPeerDetector(this, entry.getKey(), entry.getValue(), (netItf, address) -> {
					LanPresenceAnnouncer announcer = announcersByNetworkItf.get(netItf);
					return announcer != null && address.getPort() == announcer.getPort();
				});
				peerDetectorsByNetworkItf.put(entry.getKey(), detector);
				detector.start();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void stopPeerScanning() {
		for (Map.Entry<NetworkInterface, LanPeerDetector> entry : peerDetectorsByNetworkItf.entrySet()) {
			entry.getValue().interrupt();
			peerDetectorsByNetworkItf.remove(entry.getKey());
		}
	}

	public void openToLan() {
		try {
			serverSocket = new ServerSocket(0);
			localPort = serverSocket.getLocalPort();

			for (Map.Entry<NetworkInterface, List<InetAddress>> entry : hostAddresses.entrySet()) {
				LanPresenceAnnouncer announcer = new LanPresenceAnnouncer(this, entry.getKey(), entry.getValue(), localPort, hostname);
				announcersByNetworkItf.put(entry.getKey(), announcer);
				announcer.start();
			}

			connectionListenerThread = new Thread(() -> {
				try {
					while (!serverSocket.isClosed()) {
						acceptClient();
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}, "Network Connection Listener");
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
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));

				while (!client.isClosed() && !shuttingDown.get() && !shutDown.get()) {
					String message = reader.readLine();
					runOnThread(() -> onMessage(connectedPeer, message));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	void connectToPeer(LanPeer peer) throws IOException {
		Socket socket = new Socket(peer.getAddress(), peer.getAnnouncement().data().port());
		ConnectedLanPeer tcpPeer = new ConnectedLanPeer(this, socket, System.currentTimeMillis(), peer.getUdpPacket());
		connections.tcpPeersByAddress.put(socket.getRemoteSocketAddress(), tcpPeer);

		connectionThreadPool.submit(() -> {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				while (!socket.isClosed() && !shuttingDown.get() && !shutDown.get()) {
					String message = reader.readLine();
					runOnThread(() -> onMessage(tcpPeer, message));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void onMessage(ConnectedLanPeer peer, String message) {
		PacketType packetType = mapper.getType(message);

		switch (packetType) {
		case PING:
			pingHandler.handlePacket(message, peer);
			break;
		case MATCH_CLASSES_S2C:
			matchClassesS2CHandler.handlePacket(message, peer);
			break;
		case MATCHED_CLASSES_C2S:
			matchedClassesC2SHandler.handlePacket(message, peer);
			break;
		default:
			Matcher.LOGGER.warn("Unhandled packet type: {}", packetType);
			break;
		}
	}

	public void shutdown() {
		if (shutDown.get() || shuttingDown.getAndSet(true)) {
			return;
		}

		runOnThread0(() -> {
			stopPeerScanning();

			for (LanPresenceAnnouncer announcer : announcersByNetworkItf.values()) {
				announcer.interrupt();
			}

			announcersByNetworkItf.clear();

			if (connectionListenerThread != null) {
				connectionListenerThread.interrupt();
				connectionListenerThread = null;
			}

			for (Map.Entry<SocketAddress, ConnectedLanPeer> entry : new HashMap<>(connections.tcpPeersByAddress).entrySet()) {
				entry.getValue().kick("Server shutting down");
			}

			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					Matcher.LOGGER.error("Failed to close LAN server socket", e);
				}

				serverSocket = null;
				localPort = -1;
			}

			shutDown.set(true);
			shuttingDown.set(false);
			taskExecutorThread.interrupt();
		});
	}

	public Matcher getMatcher() {
		return matcher;
	}

	public PacketMapper getPacketMapper() {
		return mapper;
	}

	public ConnectionStore getConnections() {
		return connections;
	}

	public @Nullable String getHostname() {
		return hostname.get();
	}

	public void setHostname(@Nullable String hostname) {
		this.hostname.set(hostname);
	}

	public Map<NetworkInterface, LanPeerDetector> getPeerDetectorsByNetworkItf() {
		return peerDetectorsByNetworkItf;
	}

	public Map<NetworkInterface, LanPresenceAnnouncer> getAnnouncersByNetworkItf() {
		return announcersByNetworkItf;
	}

	public boolean isShuttingDown() {
		return shuttingDown.get();
	}

	public boolean isShutDown() {
		return shutDown.get();
	}

	public MatchedClassesC2SHandler getMatchedClassesC2SHandler() {
		return matchedClassesC2SHandler;
	}
}
