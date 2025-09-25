package matcher.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import matcher.Matcher;
import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;

public final class NetworkUtil {
	private NetworkUtil() {
	}

	public static Set<String> getHostAddresses() {
		Set<String> addresses = new HashSet<>();

		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {
				Enumeration<InetAddress> inetAddresses = interfaces.nextElement().getInetAddresses();

				while (inetAddresses.hasMoreElements()) {
					addresses.add(inetAddresses.nextElement().getHostAddress());
				}
			}
		} catch (SocketException e) {
			return Set.of();
		}

		return addresses;
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

	public static void logFailedToParse(String json, PacketType packetType, ConnectedLanPeer client, Exception exception) {
		Matcher.LOGGER.error("Failed to parse {} packet from {} \"{}\"", packetType.name(), remoteAddressPlusPort(client.getSocket()), json, exception);
	}
}
