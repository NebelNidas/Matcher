package matcher.network.threads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import matcher.Matcher;
import matcher.Util;
import matcher.network.peer.Peer;
import matcher.network.peer.TcpPeerConnection;
import matcher.network.NetworkHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accepts TCP connections from other Matcher instances in a given
 * network interface's network.
 */
public class PeerAcceptor extends Thread {
	private static final AtomicInteger THREAD_ID = new AtomicInteger();
	private final NetworkHandler networkHandler;
	private final NetworkInterface networkInterface;
	private final InetAddress address;
	private ServerSocket socket;
	private int port;
	private boolean running;
	private int consecutiveFailedAccepts;
	private Exception lastError;

	public PeerAcceptor(
			NetworkHandler networkHandler,
			NetworkInterface networkInterface,
			InetAddress address) {
		super("PeerAcceptor #" + THREAD_ID.incrementAndGet());
		this.networkHandler = networkHandler;
		this.networkInterface = networkInterface;
		this.address = address;

		setDaemon(true);
	}

	@Override
	public void run() {
		running = true;

		while (!isInterrupted()) {
			if (socket == null || socket.isClosed()) {
				try {
					if (!networkInterface.isUp()) {
						handleError(null, () -> Matcher.LOGGER.warn("Network interface {} is down, cannot accept connections at address {}",
								networkInterface.getDisplayName(),
								address));
					}

					socket = new ServerSocket(0, 50, address);
					port = socket.getLocalPort();
					PresenceAnnouncer announcer = networkHandler.getAnnouncersByAddressByNetworkItf()
							.getOrDefault(networkInterface, Map.of())
							.get(address);

					announcer.setTcpPortToAnnounce(port);
					consecutiveFailedAccepts = 0;
					Matcher.LOGGER.info("Accepting connections at address {}:{} through network interface {}",
							address,
							port,
							networkInterface.getDisplayName());
				} catch (IOException e) {
					handleError(e, () -> {
						Matcher.LOGGER.warn("Error while binding PeerAcceptor socket at address {} through network interface {}",
								address,
								networkInterface.getDisplayName(),
								e);
					});
				}
			}

			try {
				acceptClient();
				consecutiveFailedAccepts = 0;
			} catch (IOException e) {
				handleError(e, () -> Matcher.LOGGER.warn("Error while waiting for connections at address {}:{} through network interface {}",
						address,
						port,
						networkInterface.getDisplayName(),
						e));
			}
		}

		Matcher.LOGGER.debug("PeerAcceptor at address {}:{} through network interface {} stopped",
				address,
				port,
				networkInterface.getDisplayName());

		if (socket != null && !socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
				Matcher.LOGGER.debug("Error while closing PeerAcceptor socket at address {}:{} through network interface {}",
						address,
						port,
						networkInterface.getDisplayName(),
						e);
			}
		}

		port = 0;
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

	private void acceptClient() throws IOException {
		Socket client = socket.accept();
		consecutiveFailedAccepts = 0;
		TcpPeerConnection tcpConnection = new TcpPeerConnection(
				networkHandler,
				networkInterface,
				address,
				client,
				false,
				System.currentTimeMillis());
		TcpPeerConnection oldTcpConn = networkHandler.getConnections().tcpConnsByRemoteAddrByNetItfAddrByNetItf
				.computeIfAbsent(networkInterface, k -> new ConcurrentHashMap<>())
				.computeIfAbsent(address, k -> new ConcurrentHashMap<>())
				.put((InetSocketAddress) client.getRemoteSocketAddress(), tcpConnection);

		if (oldTcpConn != null) {
			Matcher.LOGGER.info("Closing old connection to {} at address {}:{} through network interface {}",
					client.getRemoteSocketAddress(),
					address,
					port,
					networkInterface.getDisplayName());
			oldTcpConn.terminateConnection();
		}

		Peer peer = new Peer(networkHandler, null, tcpConnection);
		networkHandler.getConnections().pendingPeers.add(peer);

		Matcher.LOGGER.info("Accepted connection from {} at address {}:{} through network interface {}",
				client.getRemoteSocketAddress(),
				address,
				port,
				networkInterface.getDisplayName());

		NetworkHandler.CONNECTION_THREAD_POOL.submit(() -> {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));

				while (!client.isClosed() && !networkHandler.isShuttingDown() && !networkHandler.isShutDown()) {
					String message = reader.readLine();

					if (message == null) {
						// connection closed by client
						tcpConnection.terminateConnection();
						break;
					}

					networkHandler.runOnThread(() -> networkHandler.onMessage(message, peer, tcpConnection));
				}
			} catch (IOException e) {
				Matcher.LOGGER.error("Connection to {} lost at address {}:{} through network interface {}",
						client.getRemoteSocketAddress(),
						address,
						port,
						networkInterface.getDisplayName(),
						e);
			}
		});
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

	public boolean isBound() {
		return socket != null && socket.isBound() && !socket.isClosed();
	}

	public boolean isRunning() {
		return running;
	}
}
