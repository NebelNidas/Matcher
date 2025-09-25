package matcher.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matcher.Matcher;
import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;

public final class NetworkUtil {
	private NetworkUtil() {
	}

	public static Map<NetworkInterface, List<InetAddress>> getAllLocalAddresses() {
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

			while (inetAddresses.hasMoreElements()) {
				InetAddress address = inetAddresses.nextElement();
				ret.computeIfAbsent(networkInterface, k -> new ArrayList<>()).add(address);
			}
		}

		return ret;
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
