package matcher.network.packet;

import java.util.HashMap;
import java.util.Map;

import matcher.network.packet.c2s.InviteResponseC2S;
import matcher.network.packet.c2s.MatchedClassesC2S;
import matcher.network.packet.p2p.Ping;
import matcher.network.packet.p2p.Pong;
import matcher.network.packet.p2p.PresenceAnnouncement;
import matcher.network.packet.p2p.Test;
import matcher.network.packet.s2c.InviteS2C;
import matcher.network.packet.s2c.KickS2C;
import matcher.network.packet.s2c.MatchClassesS2C;

public enum PacketType {
	TEST(Test.class),
	PRESENCE_ANNOUNCEMENT(PresenceAnnouncement.class),
	PING(Ping.class),
	PONG(Pong.class),
	INVITE_S2C(InviteS2C.class),
	INVITE_RESPONSE_C2S(InviteResponseC2S.class),
	// LOGIN_C2S(),
	// LOGIN_RESULT_S2C(),
	// CONNECTED_PEERS_S2C(),
	// DISCONNECT_C2S(),
	KICK_S2C(KickS2C.class),
	MATCH_CLASSES_S2C(MatchClassesS2C.class),
	MATCHED_CLASSES_C2S(MatchedClassesC2S.class);

	private static final Map<String, PacketType> byJsonName = new HashMap<>();
	private final Class<? extends Packet<?>> packetClass;

	static {
		for (PacketType packetType : values()) {
			byJsonName.put(packetType.name(), packetType);
		}
	}

	PacketType(Class<? extends Packet<?>> packetClass) {
		this.packetClass = packetClass;
	}

	public static PacketType fromJsonName(String jsonName) {
		PacketType packetType = byJsonName.get(jsonName);

		if (packetType == null) {
			throw new IllegalArgumentException("Unknown packet type: " + jsonName);
		}

		return packetType;
	}

	public String jsonName() {
		return name();
	}

	@Override
	public String toString() {
		return name();
	}

	public Class<? extends Packet<?>> packetClass() {
		return packetClass;
	}
}
