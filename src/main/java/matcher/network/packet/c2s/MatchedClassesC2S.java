package matcher.network.packet.c2s;

import java.util.Map;

import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.c2s.MatchedClassesC2S.Data;

public record MatchedClassesC2S(PacketType type, Data data) implements Packet<Data> {
	public MatchedClassesC2S(Data data) {
		this(PacketType.MATCHED_CLASSES_C2S, data);
	}

	/**
	 * @param matches clsId -> clsId
	 * */
	public record Data(Map<String, String> matches) { }
}
