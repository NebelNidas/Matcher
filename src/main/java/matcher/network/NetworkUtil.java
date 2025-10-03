package matcher.network;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import matcher.Matcher;
import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.p2p.Test;
import matcher.network.peer.TcpPeerConnection;

public final class NetworkUtil {
	private NetworkUtil() {
	}

	public static Map<NetworkInterface, List<InetAddress>> getLocalAddresses(
			boolean includeLoopback,
			boolean includeVirtual,
			boolean includeInactive,
			boolean includeLinkLocal,
			boolean includeMulticast) {
		Enumeration<NetworkInterface> interfaces;

		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			return Map.of();
		}

		Map<NetworkInterface, List<InetAddress>> ret = new HashMap<>();

		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = interfaces.nextElement();
			Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

			try {
				if ((!includeLoopback && networkInterface.isLoopback())
						|| (!includeVirtual && networkInterface.isVirtual())
						|| (!includeInactive && !networkInterface.isUp())) {
					continue;
				}
			} catch (SocketException e) {
				continue;
			}

			while (inetAddresses.hasMoreElements()) {
				InetAddress address = inetAddresses.nextElement();

				if ((!includeLoopback && address.isLoopbackAddress())
						|| (!includeLinkLocal && address.isLinkLocalAddress())
						|| (!includeMulticast && address.isMulticastAddress())) {
					continue;
				}

				ret.computeIfAbsent(networkInterface, k -> new ArrayList<>()).add(address);
			}
		}

		return ret;
	}

	public static List<InetAddress> extractMulticastCapableAddresses(NetworkHandler networkHandler, NetworkInterface networkInterface, List<InetAddress> addressesToTry) {
		List<InetAddress> ret = new ArrayList<>();
		byte[] packetContent = new byte[100];
		DatagramPacket packet = new DatagramPacket(packetContent, packetContent.length, NetworkConstants.MULTICAST_ADDRESS, NetworkConstants.MULTICAST_PORT);

		for (InetAddress address : addressesToTry) {
			if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
				continue;
			}

			try (DatagramSocket socket = new DatagramSocket(0, address)) {
				try {
					socket.send(packet);
					ret.add(address);
				} catch (IOException e) {
					// ignore
				}
			} catch (SocketException e) {
				// ignore
			}
		}

		return ret;
	}

	/**
	 * Returns a (hopefully) unique identifier based on the MAC addresses of all network interfaces,
	 * various system properties and the current time. The identifier is hashed using SHA-1 to ensure privacy.
	 * The returned value is identical across multiple invocations, but not across JVM restarts.
	 */
	public static String getInstanceIdentifier() {
		if (deviceIdentifier == null) {
			deviceIdentifierLock.lock();

			try {
				if (deviceIdentifier == null) {
					deviceIdentifier = generateDeviceIdentifier();
				}
			} finally {
				deviceIdentifierLock.unlock();
			}
		}

		return deviceIdentifier;
	}

	private static String generateDeviceIdentifier() {
		StringBuilder sb = new StringBuilder();

		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				NetworkInterface networkInterface = interfaces.nextElement();

				try {
					byte[] mac = networkInterface.getHardwareAddress();

					if (mac != null) {
						for (byte b : mac) {
							sb.append(String.format("%02X", b));
						}
					}
				} catch (SocketException e) {
					Matcher.LOGGER.debug("Failed to get MAC address for network interface {}", networkInterface.getDisplayName(), e);
				}
			}
		} catch (SocketException e) {
			Matcher.LOGGER.error("Failed to get network interfaces", e);
		}

		if (sb.isEmpty()) {
			sb.append("unknown-device");
		}

		sb.append(Runtime.getRuntime().availableProcessors());
		sb.append(System.getProperty("os.name"));
		sb.append(System.getProperty("os.version"));
		sb.append(System.getProperty("os.arch"));
		sb.append(System.getProperty("user.name"));
		sb.append(System.getProperty("user.home"));
		sb.append(System.getProperty("user.dir"));
		sb.append(System.getProperty("java.version"));
		sb.append(System.getProperty("java.vendor"));
		sb.append(System.getProperty("java.vm.name"));
		sb.append(System.getProperty("java.vm.version"));
		sb.append(System.getProperty("java.class.path"));
		sb.append(System.nanoTime());

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
			StringBuilder hashString = new StringBuilder();

			for (byte b : hashBytes) {
				hashString.append(String.format("%02x", b));
			}

			return hashString.substring(0, 15);
		} catch (NoSuchAlgorithmException e) { // impossible
			throw new IllegalStateException();
		}
	}

	public static String hostnamePlusPort(InetSocketAddress address) {
		return address.getHostName() + ":" + address.getPort();
	}

	public static String remoteAddressPlusPort(Socket connection) {
		return connection.getInetAddress() + ":" + connection.getPort();
	}

	public static String localAddressPlusPort(Socket connection) {
		return connection.getLocalAddress() + ":" + connection.getLocalPort();
	}

	public static void logFailedToSerialize(Packet<?> packet, Exception exception) {
		Matcher.LOGGER.error("Failed to serialize {} packet \"{}\"", packet.type().name(), packet, exception);
	}

	public static void logFailedToParse(String json, PacketType packetType, TcpPeerConnection client, Exception exception) {
		Matcher.LOGGER.error("Failed to parse {} packet from {} \"{}\"", packetType.name(), remoteAddressPlusPort(client.getSocket()), json, exception);
	}

	private static final Lock deviceIdentifierLock = new ReentrantLock();
	private static volatile String deviceIdentifier = null;
}
