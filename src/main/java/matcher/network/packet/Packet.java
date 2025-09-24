package matcher.network.packet;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Packet<T> {
	@JsonProperty(required = true, index = 0)
	PacketType type();

	@JsonProperty(required = true, index = 1)
	T data();
}
