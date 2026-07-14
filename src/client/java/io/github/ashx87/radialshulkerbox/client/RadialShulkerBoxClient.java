package io.github.ashx87.radialshulkerbox.client;

import io.github.ashx87.radialshulkerbox.RadialShulkerBox;
import io.github.ashx87.radialshulkerbox.network.OpenShulkerBoxPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

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

	static boolean matchesOpenKey(final KeyEvent event) {
		return openRadialMenuKey.matches(event);
	}

	static boolean matchesOpenMouse(final MouseButtonEvent event) {
		return openRadialMenuKey.matchesMouse(event);
	}

	/**
	 * Closes the radial menu and opens the hovered shulker box, if any. Called from
	 * the screen's release-event handlers — the authoritative path, since KeyMapping
	 * down-state is NOT updated for mouse buttons while a screen is open (see
	 * MouseHandler: KeyMapping.set only runs when screen == null, unlike keyboard
	 * releases which always go through) — and from the tick fallback below.
	 */
	static void closeAndSelect(final Minecraft client, final RadialMenuScreen radialMenu) {
		int hovered = radialMenu.hoveredIndex();
		List<ShulkerBoxScanner.Entry> entries = radialMenu.entries();
		client.setScreen(null);
		// Reaching here means the hotkey was just released, but neither vanilla path
		// records that: setScreen(null) does not call KeyMapping.releaseAll (only
		// opening a screen does), and MouseHandler returns early once the screen
		// consumes the release, skipping its KeyMapping.set(false). Without this the
		// still-true down state reopens the menu on the very next tick.
		openRadialMenuKey.setDown(false);
		if (hovered >= 0 && hovered < entries.size()) {
			ClientPlayNetworking.send(new OpenShulkerBoxPayload(entries.get(hovered).slot()));
		}
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
			// Fallback for releases the screen never sees (e.g. focus loss zeroing the
			// key state); the usual close path is the screen's release-event handlers.
			if (!openRadialMenuKey.isDown()) {
				closeAndSelect(client, radialMenu);
			}
		} else if (client.screen == null && openRadialMenuKey.isDown() && !suppressReopen
				&& client.player != null && !client.player.isSpectator()) {
			List<ShulkerBoxScanner.Entry> entries = ShulkerBoxScanner.scan(client.player);
			if (entries.isEmpty()) {
				client.player.sendOverlayMessage(
					Component.translatable("message.radial-shulker-box.no_boxes"));
				// Reuse the suppress flag so the action bar message fires once per key
				// press instead of every tick while the key stays held.
				suppressReopen = true;
			} else {
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