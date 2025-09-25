package matcher.network.packet.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import matcher.Matcher;
import matcher.jobs.AutoMatchClassesRemoteJob;
import matcher.network.ConnectedLanPeer;
import matcher.network.NetworkHandler;
import matcher.network.packet.PacketHandler;
import matcher.network.packet.PacketMapper;
import matcher.network.packet.PacketType;
import matcher.network.packet.c2s.MatchedClassesC2S;

public class MatchedClassesC2SHandler implements PacketHandler<MatchedClassesC2S> {
	private final NetworkHandler networkHandler;
	private final Map<ConnectedLanPeer, AutoMatchClassesRemoteJob> jobsByPeer = new ConcurrentHashMap<>();

	public MatchedClassesC2SHandler(NetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public PacketType getPacketType() {
		return PacketType.MATCHED_CLASSES_C2S;
	}

	public Map<ConnectedLanPeer, AutoMatchClassesRemoteJob> getJobsByPeer() {
		return jobsByPeer;
	}

	@Override
	public void handlePacket(String json, ConnectedLanPeer client) {
		PacketMapper mapper = networkHandler.getPacketMapper();
		MatchedClassesC2S packet;

		try {
			packet = mapper.fromString(json, MatchedClassesC2S.class);
		} catch (Exception e) {
			Matcher.LOGGER.error("Failed to parse MatchedClassesC2S packet from {}", client.getAddress(), e);
			return;
		}

		jobsByPeer.get(client).addMatches(packet.data().matches());
	}
}
