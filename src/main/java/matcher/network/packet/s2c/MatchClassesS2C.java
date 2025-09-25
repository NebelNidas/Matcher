package matcher.network.packet.s2c;

import java.util.List;

import matcher.classifier.ClassifierLevel;
import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.s2c.MatchClassesS2C.Data;

public record MatchClassesS2C(PacketType type, Data data) implements Packet<Data> {
	public MatchClassesS2C(Data data) {
		this(PacketType.MATCH_CLASSES_S2C, data);
	}

	public record Data(List<String> classIds, ClassifierLevel classifierLevel) { }
}
