package io.github.ashx87.radialshulkerbox.client;

import io.github.ashx87.radialshulkerbox.RadialShulkerBox;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

public class RadialShulkerBoxClient implements ClientModInitializer {
	private static KeyMapping openRadialMenuKey;

	@Override
	public void onInitializeClient() {
		KeyMapping.Category category = KeyMapping.Category.register(RadialShulkerBox.id("main"));
		openRadialMenuKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.radial-shulker-box.open", GLFW.GLFW_KEY_G, category)
		);
	}
}