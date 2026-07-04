package io.github.ashx87.radialshulkerbox;

import io.github.ashx87.radialshulkerbox.network.OpenShulkerBoxPayload;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadialShulkerBox implements ModInitializer {
	public static final String MOD_ID = "radial-shulker-box";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.serverboundPlay().register(OpenShulkerBoxPayload.TYPE, OpenShulkerBoxPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(OpenShulkerBoxPayload.TYPE, (payload, context) ->
			ShulkerBoxMenuOpener.openFromInventorySlot(context.player(), payload.slot()));
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
