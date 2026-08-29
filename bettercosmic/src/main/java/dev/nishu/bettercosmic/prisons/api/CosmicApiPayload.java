package dev.nishu.bettercosmic.prisons.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

/**
 * A message on the Cosmic API plugin channel ({@code cosmicapi:main}).
 *
 * <p>The wire payload is the raw UTF-8 JSON body (Bukkit plugin channels carry a {@code byte[]}
 * payload), so this reads/writes all remaining bytes as a string. Prison-only: Cosmic Prisons is the
 * sole server that exposes this API. Ported from BetterPrisons (Yarn → Mojang).
 */
public record CosmicApiPayload(String json) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CosmicApiPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("cosmicapi", "main"));

	public static final StreamCodec<FriendlyByteBuf, CosmicApiPayload> CODEC = StreamCodec.of(
			(buf, value) -> buf.writeBytes(value.json.getBytes(StandardCharsets.UTF_8)),
			buf -> {
				byte[] bytes = new byte[buf.readableBytes()];
				buf.readBytes(bytes);
				return new CosmicApiPayload(new String(bytes, StandardCharsets.UTF_8));
			});

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
