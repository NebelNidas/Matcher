package matcher.network.peer;

import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

public interface PeerConnection {
	NetworkInterface getNetworkInterface();

	InetAddress getNetworkInterfaceAddress();

	InetSocketAddress getRemoteSocketAddress();

	long getLastSignOfLifeTime();

	@Nullable Peer getPeer();
}
