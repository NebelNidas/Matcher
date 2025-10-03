package matcher.network;

import matcher.network.peer.Peer;
import matcher.network.peer.TcpPeerConnection;
import matcher.network.peer.UdpPeerConnection;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionStore {
	public final Map<NetworkInterface, Map<InetAddress, Map<InetSocketAddress, UdpPeerConnection>>> incomingUdpConnsByRemoteAddrByNetItfAddrByNetItf = new ConcurrentHashMap<>();
	public final Map<NetworkInterface, Map<InetAddress, Map<InetSocketAddress, TcpPeerConnection>>> tcpConnsByRemoteAddrByNetItfAddrByNetItf = new ConcurrentHashMap<>();
	public final Map<String, Peer> peersById = new ConcurrentHashMap<>();
	public final List<Peer> pendingPeers = Collections.synchronizedList(new ArrayList<>());
}
