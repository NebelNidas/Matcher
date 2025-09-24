package matcher.network.packet;

import java.io.IOException;
import java.io.Reader;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import matcher.network.packet.serdes.SemverDeserializer;
import matcher.network.packet.serdes.SemverSerializer;

public class PacketMapper {
	private static final PacketMapper INSTANCE = new PacketMapper();
	private final ObjectMapper mapper = JsonMapper.builder()
			.addModule(new SemverSerializer.Module())
			.addModule(new SemverDeserializer.Module())
			.build();

	public static PacketMapper getInstance() {
		return INSTANCE;
	}

	public String toString(Packet<?> packet) {
		try {
			return mapper.writeValueAsString(packet);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize %s packet".formatted(packet.getClass().getSimpleName()), e);
		}
	}

	public byte[] toBytes(Packet<?> packet) {
		return toString(packet).getBytes(StandardCharsets.UTF_8);
	}

	public JsonNode parse(DatagramPacket packet) {
		return parse(Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength()));
	}

	public JsonNode parse(Reader reader) {
		try {
			return mapper.readTree(reader);
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse JSON from reader", e);
		}
	}

	public JsonNode parse(byte[] bytes) {
		try {
			return mapper.readTree(bytes);
		} catch (IOException e) {
			throw new RuntimeException("Failed to parse JSON from byte[]: " + new String(bytes, StandardCharsets.UTF_8), e);
		}
	}

	public JsonNode parse(String json) {
		try {
			return mapper.readTree(json);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to parse JSON from string: " + json, e);
		}
	}

	public PacketType getType(String json) {
		return getType(parse(json));
	}

	private PacketType getType(JsonNode node) {
		JsonNode packetTypeJsonNode = node.get("type");

		if (packetTypeJsonNode == null) {
			throw new RuntimeException("Invalid packet: \"" + node.toPrettyString() + "\"");
		}

		return PacketType.fromJsonName(packetTypeJsonNode.asText());
	}

	public <T extends Packet<?>> T fromUdpPacket(DatagramPacket udpPacket, Class<T> packetClass) {
		return fromJsonNode(parse(udpPacket), packetClass);
	}

	public <T extends Packet<?>> T fromBytes(byte[] bytes, Class<T> packetClass) {
		return fromJsonNode(parse(bytes), packetClass);
	}

	public <T extends Packet<?>> T fromReader(Reader reader, Class<T> packetClass) {
		return fromJsonNode(parse(reader), packetClass);
	}

	public <T extends Packet<?>> T fromString(String json, Class<T> packetClass) {
		return fromJsonNode(parse(json), packetClass);
	}

	private <T extends Packet<?>> T fromJsonNode(JsonNode node, Class<T> packetClass) {
		PacketType type = getType(node);

		assert packetClass == type.packetClass();

		try {
			return mapper.treeToValue(node, packetClass);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to parse %s packet:\n%s".formatted(type.jsonName(), node.toPrettyString()), e);
		}
	}
}
