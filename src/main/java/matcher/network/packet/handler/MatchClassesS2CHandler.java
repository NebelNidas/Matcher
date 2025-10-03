package matcher.network.packet.handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import matcher.jobs.AutoMatchClassesLocalJob;
import matcher.jobs.AutoMatchClassesRemoteJob;
import matcher.network.peer.Peer;
import matcher.network.peer.TcpPeerConnection;
import matcher.network.NetworkHandler;
import matcher.network.NetworkUtil;
import matcher.network.packet.PacketHandler;
import matcher.network.packet.PacketMapper;
import matcher.network.packet.PacketType;
import matcher.network.packet.c2s.MatchedClassesC2S;
import matcher.network.packet.s2c.MatchClassesS2C;
import matcher.type.ClassInstance;

public class MatchClassesS2CHandler implements PacketHandler<MatchClassesS2C> {
	private final NetworkHandler networkHandler;
	private AutoMatchClassesRemoteJob job;

	public MatchClassesS2CHandler(NetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public PacketType getPacketType() {
		return PacketType.MATCH_CLASSES_S2C;
	}

	@Override
	public void handlePacket(String json, Peer peer, TcpPeerConnection connection) {
		PacketMapper mapper = networkHandler.getPacketMapper();
		MatchClassesS2C packet;

		try {
			packet = mapper.fromString(json, MatchClassesS2C.class);
		} catch (Exception e) {
			NetworkUtil.logFailedToParse(json, getPacketType(), connection, e);
			return;
		}

		List<ClassInstance> classes = packet.data().classIds().stream()
				.map(id -> networkHandler.getMatcher().getEnv().getClsByIdA(id))
				.toList();
		var job = new AutoMatchClassesLocalJob(networkHandler.getMatcher(), packet.data().classifierLevel(), classes);
		job.addFinishListener((result, error) -> {
			Map<String, String> matches = result.orElseThrow().entrySet().stream()
					.collect(Collectors.toMap(
							e -> e.getKey().getId(),
							e -> e.getValue().getId()));
			peer.send(new MatchedClassesC2S(new MatchedClassesC2S.Data(matches)));
		});
		job.runAsync();
	}
}
