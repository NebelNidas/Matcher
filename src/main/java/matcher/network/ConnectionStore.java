package matcher.network;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionStore {
	public final Map<SocketAddress, LanPeer> udpPeersByAddress = new ConcurrentHashMap<>();
	public final Map<SocketAddress, ConnectedLanPeer> tcpPeersByAddress = new ConcurrentHashMap<>();
}
