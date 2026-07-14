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
	private static boolean suppressReopen = false;

	/**
	 * Called when the radial menu is dismissed via ESC. Suppresses reopening until the
	 * key is genuinely released — otherwise the very next tick sees the key still held
	 * with no screen open and immediately reopens the menu, making ESC a no-op.
	 */
	static void onMenuDismissed() {
		suppressReopen = true;
		// setScreen(null) inside Screen#onClose zeroed isDown via KeyMapping.releaseAll()
		// even though the key is physically held (same quirk as the open path below).
		// Restore it so suppressReopen only clears on a real key-release event, not on
		// the artificial gap before the next GLFW key-repeat.
		openRadialMenuKey.setDown(true);
	}

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(RadialShulkerBox.id("main"));
		openRadialMenuKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.radial-shulker-box.open", GLFW.GLFW_KEY_G, category)
		);

		ClientTickEvents.END_CLIENT_TICK.register(RadialShulkerBoxClient::onEndTick);
	}

	private static void onEndTick(final Minecraft client) {
		if (!openRadialMenuKey.isDown()) {
			suppressReopen = false;
		}

		if (client.screen instanceof RadialMenuScreen radialMenu) {
			if (!openRadialMenuKey.isDown()) {
				int hovered = radialMenu.hoveredIndex();
				List<ShulkerBoxScanner.Entry> entries = radialMenu.entries();
				client.setScreen(null);
				if (hovered >= 0 && hovered < entries.size()) {
					ClientPlayNetworking.send(new OpenShulkerBoxPayload(entries.get(hovered).slot()));
				}
			}
		} else if (client.screen == null && openRadialMenuKey.isDown() && !suppressReopen
				&& client.player != null && !client.player.isSpectator()) {
			List<ShulkerBoxScanner.Entry> entries = ShulkerBoxScanner.scan(client.player);
			if (!entries.isEmpty()) {
				client.setScreen(new RadialMenuScreen(entries));
				// Minecraft#setScreen() unconditionally calls KeyMapping.releaseAll(), which zeroes
				// openRadialMenuKey's isDown state even though G is still physically held. Without this,
				// the very next tick sees isDown() == false and closes the screen again, causing a
				// rapid open/close loop (visible as flicker) until the next GLFW key-repeat event
				// flips isDown back to true and reopens it. Restore the true state immediately.
				openRadialMenuKey.setDown(true);
			}
		}
	}
}