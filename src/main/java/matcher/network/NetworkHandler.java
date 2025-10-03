package matcher.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.SimpleStringProperty;

import matcher.network.packet.p2p.PresenceAnnouncement;
import matcher.network.peer.Peer;
import matcher.network.peer.TcpPeerConnection;
import matcher.network.peer.UdpPeerConnection;
import matcher.network.threads.PeerAcceptor;
import matcher.network.threads.PeerDetector;
import matcher.network.threads.PresenceAnnouncer;

import org.jetbrains.annotations.Nullable;

import matcher.Matcher;
import matcher.Util;
import matcher.network.packet.PacketMapper;
import matcher.network.packet.PacketType;
import matcher.network.packet.handler.MatchClassesS2CHandler;
import matcher.network.packet.handler.MatchedClassesC2SHandler;
import matcher.network.packet.handler.PingHandler;

public class NetworkHandler {
	public static final ExecutorService CONNECTION_THREAD_POOL = Executors.newCachedThreadPool();
	public static final String INSTANCE_ID = NetworkUtil.getInstanceIdentifier();
	private final Matcher matcher;
	private final PacketMapper mapper;
	private final ConnectionStore connections;
	private final SimpleStringProperty hostname;
	private final Map<NetworkInterface, List<InetAddress>> hostAddressesByNetworkItf;
	private final Map<NetworkInterface, Map<InetAddress, PeerDetector>> peerDetectorsByAddressByNetworkItf;
	private final Map<NetworkInterface, Map<InetAddress, PresenceAnnouncer>> announcersByAddressByNetworkItf;
	private final Map<NetworkInterface, Map<InetAddress, PeerAcceptor>> acceptorsByAddressByNetworkItf;
	private final BlockingQueue<Runnable> tasks;
	private final Thread taskExecutorThread;
	private final AtomicBoolean shuttingDown;
	private final AtomicBoolean shutDown;
	private final PingHandler pingHandler;
	private final MatchClassesS2CHandler matchClassesS2CHandler;
	private final MatchedClassesC2SHandler matchedClassesC2SHandler;
	private @Nullable Thread connectionListenerThread;

	public NetworkHandler(Matcher matcher) {
		this.matcher = matcher;
		this.mapper = new PacketMapper(matcher, this);

		connections = new ConnectionStore();
		hostname = new SimpleStringProperty(Util.getComputerName());
		hostAddressesByNetworkItf = NetworkUtil.getLocalAddresses(false, false, true, false, false);
		peerDetectorsByAddressByNetworkItf = new ConcurrentHashMap<>();
		announcersByAddressByNetworkItf = new ConcurrentHashMap<>();
		acceptorsByAddressByNetworkItf = new ConcurrentHashMap<>();
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
	}

	public boolean isOnThread() {
		return Thread.currentThread() == taskExecutorThread;
	}

	public void assertOnThread() {
		if (!isOnThread()) {
			throw new IllegalStateException("Not on NetworkHandler thread");
		}
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
		Matcher.LOGGER.info("Starting peer scanning");

		try {
			for (Map.Entry<NetworkInterface, List<InetAddress>> entry : hostAddressesByNetworkItf.entrySet()) {
				NetworkInterface networkInterface = entry.getKey();
				List<InetAddress> addresses = entry.getValue();

				for (InetAddress address : addresses) {
					PeerDetector detector = new PeerDetector(this, networkInterface, address);
					peerDetectorsByAddressByNetworkItf
							.computeIfAbsent(networkInterface, k -> new ConcurrentHashMap<>())
							.put(address, detector);
					detector.start();
				}

			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void stopPeerScanning() {
		Matcher.LOGGER.info("Stopping peer scanning");

		for (Map.Entry<NetworkInterface, List<InetAddress>> entry : hostAddressesByNetworkItf.entrySet()) {
			NetworkInterface networkInterface = entry.getKey();
			List<InetAddress> addresses = entry.getValue();

			for (InetAddress address : addresses) {
				PeerDetector detector = peerDetectorsByAddressByNetworkItf
						.getOrDefault(networkInterface, Map.of())
						.get(address);
				if (detector != null) {
					detector.interrupt();
					peerDetectorsByAddressByNetworkItf.get(networkInterface).remove(address);
				}
			}
		}
	}

	public void openToLan() {
		Matcher.LOGGER.info("""
				Opening to LAN. Detected {} network interfaces with addresses: {}
				The network constants are as follows:
				\t- Protocol version: {}
				\t- Multicast address: {}
				\t- Multicast port: {}
				\t- Milliseconds between presence announcements: {}

				To be announced:
				\t- Name: {}
				\t- Instance ID: {}
				""",
				hostAddressesByNetworkItf.size(),
				Util.prettyPrint(hostAddressesByNetworkItf),
				NetworkConstants.PROTOCOL_VERSION,
				NetworkConstants.MULTICAST_ADDRESS,
				NetworkConstants.MULTICAST_PORT,
				NetworkConstants.MULTICAST_INTERVAL_MS,
				hostname.get(),
				INSTANCE_ID);
		Matcher.LOGGER.info("Starting presence announcers");

		for (Map.Entry<NetworkInterface, List<InetAddress>> entry : hostAddressesByNetworkItf.entrySet()) {
			NetworkInterface networkInterface = entry.getKey();
			List<InetAddress> addresses = entry.getValue();

			for (InetAddress address : addresses) {
				PresenceAnnouncer announcer = new PresenceAnnouncer(this, networkInterface, address, hostname);
				announcersByAddressByNetworkItf
						.computeIfAbsent(networkInterface, k -> new ConcurrentHashMap<>())
						.put(address, announcer);
				announcer.start();
			}
		}

		Matcher.LOGGER.info("Starting peer acceptors");

		for (Map.Entry<NetworkInterface, List<InetAddress>> entry : hostAddressesByNetworkItf.entrySet()) {
			NetworkInterface networkInterface = entry.getKey();
			List<InetAddress> addresses = entry.getValue();

			for (InetAddress address : addresses) {
				PeerAcceptor acceptor = new PeerAcceptor(this, networkInterface, address);
				acceptorsByAddressByNetworkItf
						.computeIfAbsent(networkInterface, k -> new ConcurrentHashMap<>())
						.put(address, acceptor);
				acceptor.start();
			}
		}
	}

	public void connectToPeer(UdpPeerConnection udpConnection) throws IOException {
		assertOnThread();
		Map<InetSocketAddress, TcpPeerConnection> tcpConnsByRemoteAddr = connections.tcpConnsByRemoteAddrByNetItfAddrByNetItf
				.computeIfAbsent(udpConnection.getNetworkInterface(), k -> new ConcurrentHashMap<>())
				.computeIfAbsent(udpConnection.getNetworkInterfaceAddress(), k -> new ConcurrentHashMap<>());
		InetSocketAddress remoteAddr = udpConnection.getRemoteSocketAddress();
		PresenceAnnouncement.Data announcementData = udpConnection.getAnnouncementData();

		if (shuttingDown.get() || shutDown.get()
				|| tcpConnsByRemoteAddr.containsKey(remoteAddr)
				|| connections.peersById.containsKey(announcementData.instanceIdentifier())) {
			return;
		}
		Socket socket = new Socket(
				udpConnection.getRemoteSocketAddress().getAddress(),
				announcementData.port(),
				udpConnection.getNetworkInterfaceAddress(),
				0);

		Matcher.LOGGER.info("Connecting to peer {} (id: {}) at address {}:{} via local address {}:{} through network interface {}",
				announcementData.name(),
				announcementData.instanceIdentifier(),
				remoteAddr.getAddress(),
				announcementData.port(),
				udpConnection.getNetworkInterfaceAddress(),
				socket.getLocalPort(),
				udpConnection.getNetworkInterface().getDisplayName());

		TcpPeerConnection tcpConnection = new TcpPeerConnection(
				this,
				udpConnection.getNetworkInterface(),
				udpConnection.getNetworkInterfaceAddress(),
				socket,
				true,
				System.currentTimeMillis());
		tcpConnsByRemoteAddr.put(remoteAddr, tcpConnection);
		Peer peer = new Peer(this, udpConnection, tcpConnection);
		connections.peersById.put(announcementData.instanceIdentifier(), peer);

		CONNECTION_THREAD_POOL.submit(() -> {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				while (!socket.isClosed() && !shuttingDown.get() && !shutDown.get()) {
					String message = reader.readLine();

					if (message == null) {
						// connection closed by peer
						tcpConnection.terminateConnection();
						break;
					}

					runOnThread(() -> onMessage(message, peer, tcpConnection));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public void onMessage(String message, Peer peer, TcpPeerConnection connection) {
		PacketType packetType = mapper.getType(message);

		switch (packetType) {
		case PING:
			pingHandler.handlePacket(message, peer, connection);
			break;
		case MATCH_CLASSES_S2C:
			matchClassesS2CHandler.handlePacket(message, peer, connection);
			break;
		case MATCHED_CLASSES_C2S:
			matchedClassesC2SHandler.handlePacket(message, peer, connection);
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

			for (Map<InetAddress, PresenceAnnouncer> entry : announcersByAddressByNetworkItf.values()) {
				for (PresenceAnnouncer announcer : entry.values()) {
					announcer.interrupt();
				}
			}

			announcersByAddressByNetworkItf.clear();

			if (connectionListenerThread != null) {
				connectionListenerThread.interrupt();
				connectionListenerThread = null;
			}

			for (Peer peer : connections.peersById.values()) {
				peer.kick("Server shutting down");
			}

			for (Peer peer : connections.pendingPeers) {
				peer.kick("Server shutting down");
			}

			CONNECTION_THREAD_POOL.shutdownNow();

			for (Map<InetAddress, PeerAcceptor> acceptorsByAddress : acceptorsByAddressByNetworkItf.values()) {
				for (PeerAcceptor acceptor : acceptorsByAddress.values()) {
					acceptor.interrupt();
				}
			}

			acceptorsByAddressByNetworkItf.clear();

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

	public Map<NetworkInterface, Map<InetAddress, PeerDetector>> getPeerDetectorsByAddressByNetworkItf() {
		return peerDetectorsByAddressByNetworkItf;
	}

	public Map<NetworkInterface, Map<InetAddress, PresenceAnnouncer>> getAnnouncersByAddressByNetworkItf() {
		return announcersByAddressByNetworkItf;
	}

	public Map<NetworkInterface, Map<InetAddress, PeerAcceptor>> getAcceptorsByAddressByNetworkItf() {
		return acceptorsByAddressByNetworkItf;
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
