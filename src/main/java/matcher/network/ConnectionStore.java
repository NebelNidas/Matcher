package matcher.network;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ConnectionStore {
	public final Map<SocketAddress, LanPeer> udpPeersByAddress = new HashMap<>();
	public final Map<SocketAddress, ConnectedLanPeer> tcpPeersByAddress = new HashMap<>();
}
