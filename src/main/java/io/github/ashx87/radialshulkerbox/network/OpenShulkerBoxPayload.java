package io.github.ashx87.radialshulkerbox.network;

import io.github.ashx87.radialshulkerbox.RadialShulkerBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenShulkerBoxPayload(int slot) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OpenShulkerBoxPayload> TYPE =
		new CustomPacketPayload.Type<>(RadialShulkerBox.id("open_shulker_box"));

	public static final StreamCodec<FriendlyByteBuf, OpenShulkerBoxPayload> STREAM_CODEC =
		CustomPacketPayload.codec(OpenShulkerBoxPayload::write, OpenShulkerBoxPayload::new);

	private OpenShulkerBoxPayload(final FriendlyByteBuf input) {
		this(input.readVarInt());
	}

	private void write(final FriendlyByteBuf output) {
		output.writeVarInt(this.slot);
	}

	@Override
	public CustomPacketPayload.Type<OpenShulkerBoxPayload> type() {
		return TYPE;
	}
}
