package io.github.ashx87.radialshulkerbox;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public final class ShulkerBoxMenuOpener {
	private static final int CONTAINER_SIZE = 27;

	private ShulkerBoxMenuOpener() {
	}

	public static void openFromInventorySlot(final ServerPlayer player, final int slot) {
		if (player.isSpectator()) {
			return;
		}

		Inventory inventory = player.getInventory();
		if (slot < 0 || slot >= inventory.getContainerSize()) {
			return;
		}

		ItemStack stack = inventory.getItem(slot);
		if (stack.isEmpty() || !stack.is(ItemTags.SHULKER_BOXES)) {
			return;
		}

		ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		NonNullList<ItemStack> initialItems = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
		contents.copyInto(initialItems);

		SimpleContainer container = new SimpleContainer(initialItems.toArray(new ItemStack[0])) {
			@Override
			public void setChanged() {
				super.setChanged();
				// Persist every change straight onto the box ItemStack. Deferring the
				// write until the menu closes lets the player empty the open menu into
				// their inventory, move or drop the box, and close — the slot-based
				// write-back then misses the box, whose stale CONTAINER component still
				// holds the extracted items, duplicating them.
				writeContents(stack, this);
			}

			@Override
			public boolean stillValid(final Player user) {
				return user == player && !player.isSpectator() && isStackInInventory(player, stack);
			}
		};

		player.openMenu(new SimpleMenuProvider(
			(containerId, menuInventory, menuPlayer) -> new ShulkerBoxMenu(containerId, menuInventory, container),
			Component.translatable("container.shulkerBox")
		));
	}

	private static void writeContents(final ItemStack stack, final SimpleContainer container) {
		List<ItemStack> items = new ArrayList<>(CONTAINER_SIZE);
		for (int i = 0; i < CONTAINER_SIZE; i++) {
			items.add(container.getItem(i));
		}
		stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
	}

	// Identity scan: the menu must track this exact ItemStack instance, not whatever
	// happens to sit in the original slot. The moment the box leaves the inventory
	// (dropped, stashed in a chest, picked up onto the cursor) the menu closes.
	private static boolean isStackInInventory(final ServerPlayer player, final ItemStack stack) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i) == stack) {
				return true;
			}
		}
		return false;
	}
}
