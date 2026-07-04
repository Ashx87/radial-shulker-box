package io.github.ashx87.radialshulkerbox.client;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ShulkerBoxScanner {
	private ShulkerBoxScanner() {
	}

	public record Entry(int slot, ItemStack stack) {
	}

	public static List<Entry> scan(final Player player) {
		Inventory inventory = player.getInventory();
		List<Entry> entries = new ArrayList<>();

		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && stack.is(ItemTags.SHULKER_BOXES)) {
				entries.add(new Entry(slot, stack));
			}
		}

		return entries;
	}
}
