package io.github.ashx87.radialshulkerbox.client;

import io.github.ashx87.radialshulkerbox.RadialShulkerBox;
import io.github.ashx87.radialshulkerbox.network.OpenShulkerBoxPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

import java.util.List;

public class RadialShulkerBoxClient implements ClientModInitializer {
	private static KeyMapping openRadialMenuKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(RadialShulkerBox.id("main"));
		openRadialMenuKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.radial-shulker-box.open", GLFW.GLFW_KEY_G, category)
		);

		ClientTickEvents.END_CLIENT_TICK.register(RadialShulkerBoxClient::onEndTick);
	}

	private static void onEndTick(final Minecraft client) {
		if (client.screen instanceof RadialMenuScreen radialMenu) {
			if (!openRadialMenuKey.isDown()) {
				int hovered = radialMenu.hoveredIndex();
				List<ShulkerBoxScanner.Entry> entries = radialMenu.entries();
				client.setScreen(null);
				if (hovered >= 0 && hovered < entries.size()) {
					ClientPlayNetworking.send(new OpenShulkerBoxPayload(entries.get(hovered).slot()));
				}
			}
		} else if (client.screen == null && openRadialMenuKey.isDown() && client.player != null) {
			List<ShulkerBoxScanner.Entry> entries = ShulkerBoxScanner.scan(client.player);
			if (!entries.isEmpty()) {
				client.setScreen(new RadialMenuScreen(entries));
			}
		}
	}
}