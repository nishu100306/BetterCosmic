package dev.nishu.bettercosmic.prisons.api;

import dev.nishu.bettercosmic.prisons.BetterPrisons;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

/**
 * A message on the Cosmic API plugin channel.
 *
 * <p><b>Channel.</b> Client mods use a per-mod channel {@code cosmicapi:<modId>} (one channel per mod
 * so mods never collide on a client); the reserved {@code cosmicapi:main} is for official mods and may
 * be deprecated. This mod therefore registers {@code cosmicapi:bettercosmic} — the path is the Fabric
 * mod id ({@link BetterPrisons#FABRIC_MOD_ID}), the single source of truth.
 *
 * <p>The wire payload is the raw UTF-8 JSON body (Bukkit plugin channels carry a {@code byte[]}
 * payload), so this reads/writes all remaining bytes as a string.
 */
public record CosmicApiPayload(String json) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<CosmicApiPayload> TYPE =
			new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("cosmicapi", BetterPrisons.FABRIC_MOD_ID));

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
