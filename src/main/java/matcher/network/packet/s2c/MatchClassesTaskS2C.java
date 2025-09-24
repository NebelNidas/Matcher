package matcher.network.packet.s2c;

import matcher.classifier.ClassifierLevel;
import matcher.network.packet.Packet;
import matcher.network.packet.PacketType;
import matcher.network.packet.s2c.MatchClassesTaskS2C.Data;
import matcher.type.ClassInstance;

import java.util.Set;

public record MatchClassesTaskS2C(Data data) implements Packet<Data> {
	@Override
	public PacketType type() {
		return PacketType.MATCH_CLASSES_TASK_S2C;
	}

	public record Data(Set<ClassInstance> classes, ClassifierLevel classifierLevel) { }
}
