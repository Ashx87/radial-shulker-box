package io.github.ashx87.radialshulkerbox;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
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

		SimpleContainer container = new SimpleContainer(CONTAINER_SIZE) {
			@Override
			public void stopOpen(final ContainerUser containerUser) {
				super.stopOpen(containerUser);
				writeBack(inventory, slot, this);
			}
		};

		for (int i = 0; i < CONTAINER_SIZE; i++) {
			container.setItem(i, initialItems.get(i));
		}

		player.openMenu(new SimpleMenuProvider(
			(containerId, menuInventory, menuPlayer) -> new ShulkerBoxMenu(containerId, menuInventory, container),
			Component.translatable("container.shulkerBox")
		));
	}

	private static void writeBack(final Inventory inventory, final int slot, final SimpleContainer container) {
		ItemStack current = inventory.getItem(slot);
		if (current.isEmpty() || !current.is(ItemTags.SHULKER_BOXES)) {
			return;
		}

		List<ItemStack> items = new ArrayList<>(CONTAINER_SIZE);
		for (int i = 0; i < CONTAINER_SIZE; i++) {
			items.add(container.getItem(i));
		}

		current.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
		inventory.setItem(slot, current);
	}
}
