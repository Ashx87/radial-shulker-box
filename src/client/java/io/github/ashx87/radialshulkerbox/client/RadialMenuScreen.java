package io.github.ashx87.radialshulkerbox.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import org.joml.Matrix3x2fStack;

import java.util.List;

public class RadialMenuScreen extends Screen {
	private static final int INNER_RADIUS = 100;
	private static final int OUTER_RADIUS = 135;
	private static final int ICON_RADIUS = 117;
	private static final int ICON_HALF_SIZE = 8;
	private static final int DEAD_ZONE_RADIUS = INNER_RADIUS;

	private static final int GRID_COLS = 9;
	private static final int GRID_ROWS = 3;
	private static final int GRID_SLOTS = GRID_COLS * GRID_ROWS;
	private static final int SLOT_SIZE = 18;
	private static final int SLOT_ICON = 16;

	private static final int COLOR_RING_BASE = 0x77000000;
	private static final int COLOR_RING_HOVER = 0xAA5C9FE0;
	private static final int COLOR_PANEL_BG = 0xC0000000;
	private static final int COLOR_PANEL_BORDER = 0xFF6A6A6A;
	private static final int COLOR_SLOT_BG = 0x60373737;
	private static final int COLOR_TEXT = 0xFFFFFFFF;

	private final List<ShulkerBoxScanner.Entry> entries;
	private int hoveredIndex = -1;

	public RadialMenuScreen(final List<ShulkerBoxScanner.Entry> entries) {
		super(Component.empty());
		this.entries = entries;
	}

	public List<ShulkerBoxScanner.Entry> entries() {
		return this.entries;
	}

	public int hoveredIndex() {
		return this.hoveredIndex;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	@Override
	public void onClose() {
		// Only ESC (Screen#keyPressed) routes through here; the normal key-release
		// close path in RadialShulkerBoxClient calls setScreen(null) directly.
		super.onClose();
		RadialShulkerBoxClient.onMenuDismissed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		int count = this.entries.size();
		if (count == 0) {
			return;
		}

		this.hoveredIndex = computeHovered(mouseX - centerX, mouseY - centerY, count);

		drawRing(graphics, centerX, centerY, count);
		drawIcons(graphics, centerX, centerY, count);
		drawCenterPanel(graphics, centerX, centerY);
	}

	private int computeHovered(final double dx, final double dy, final int count) {
		double distance = Math.sqrt(dx * dx + dy * dy);
		if (distance <= DEAD_ZONE_RADIUS) {
			return -1;
		}
		// Segments are drawn centered on angle (segArc*i - PI/2). Shift the raw angle by
		// half a segment so each hover range is centered on its segment's visual center
		// instead of straddling two segments (which selected the neighbour by half a slice).
		double segArc = Math.PI * 2.0 / count;
		double angle = Math.atan2(dy, dx) + Math.PI / 2.0 + segArc / 2.0;
		angle %= Math.PI * 2.0;
		if (angle < 0) {
			angle += Math.PI * 2.0;
		}
		return (int) (angle / segArc) % count;
	}

	private void drawRing(final GuiGraphicsExtractor graphics, final int centerX, final int centerY, final int count) {
		Matrix3x2fStack pose = graphics.pose();
		double segArc = Math.PI * 2.0 / count;
		double gapRad = Math.min(0.06, segArc * 0.10);
		double halfSpan = segArc / 2.0 - gapRad;
		int subBars = Math.max(4, (int) Math.ceil((halfSpan * 2.0) / 0.03));
		double stepRad = (halfSpan * 2.0) / subBars;
		int halfWidth = (int) Math.max(2.0, Math.ceil(stepRad * OUTER_RADIUS * 0.6));

		for (int i = 0; i < count; i++) {
			int color = i == this.hoveredIndex ? COLOR_RING_HOVER : COLOR_RING_BASE;
			double centerAngle = segArc * i - Math.PI / 2.0;
			for (int j = 0; j < subBars; j++) {
				double a = centerAngle - halfSpan + stepRad * (j + 0.5);
				pose.pushMatrix();
				pose.translate(centerX, centerY);
				pose.rotate((float) (a - Math.PI / 2.0));
				graphics.fill(-halfWidth, INNER_RADIUS, halfWidth, OUTER_RADIUS, color);
				pose.popMatrix();
			}
		}
	}

	private void drawIcons(final GuiGraphicsExtractor graphics, final int centerX, final int centerY, final int count) {
		double segArc = Math.PI * 2.0 / count;
		for (int i = 0; i < count; i++) {
			double a = segArc * i - Math.PI / 2.0;
			int iconX = centerX + (int) (Math.cos(a) * ICON_RADIUS) - ICON_HALF_SIZE;
			int iconY = centerY + (int) (Math.sin(a) * ICON_RADIUS) - ICON_HALF_SIZE;
			graphics.item(this.entries.get(i).stack(), iconX, iconY);
		}
	}

	private void drawCenterPanel(final GuiGraphicsExtractor graphics, final int centerX, final int centerY) {
		if (this.hoveredIndex < 0 || this.hoveredIndex >= this.entries.size()) {
			drawHintPanel(graphics, centerX, centerY);
			return;
		}

		ItemStack box = this.entries.get(this.hoveredIndex).stack();
		Component name = box.getHoverName();
		NonNullList<ItemStack> contents = readContents(box);

		int pad = 8;
		int nameHeight = 12;
		int nameGap = 4;
		int gridWidth = GRID_COLS * SLOT_SIZE;
		int gridHeight = GRID_ROWS * SLOT_SIZE;
		int panelWidth = gridWidth + pad * 2;
		int panelHeight = pad * 2 + nameHeight + nameGap + gridHeight;
		int panelX = centerX - panelWidth / 2;
		int panelY = centerY - panelHeight / 2;

		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, COLOR_PANEL_BG);
		graphics.outline(panelX, panelY, panelWidth, panelHeight, COLOR_PANEL_BORDER);
		graphics.centeredText(this.font, name, centerX, panelY + pad, COLOR_TEXT);

		int gridX = centerX - gridWidth / 2;
		int gridY = panelY + pad + nameHeight + nameGap;
		for (int index = 0; index < GRID_SLOTS; index++) {
			int slotX = gridX + (index % GRID_COLS) * SLOT_SIZE;
			int slotY = gridY + (index / GRID_COLS) * SLOT_SIZE;
			graphics.fill(slotX, slotY, slotX + SLOT_ICON, slotY + SLOT_ICON, COLOR_SLOT_BG);
			ItemStack item = contents.get(index);
			if (!item.isEmpty()) {
				graphics.item(item, slotX, slotY);
				graphics.itemDecorations(this.font, item, slotX, slotY);
			}
		}
	}

	private void drawHintPanel(final GuiGraphicsExtractor graphics, final int centerX, final int centerY) {
		Component hint = Component.translatable("screen.radial-shulker-box.hint");
		int width = this.font.width(hint) + 16;
		int height = 20;
		graphics.fill(centerX - width / 2, centerY - height / 2, centerX + width / 2, centerY + height / 2, COLOR_PANEL_BG);
		graphics.centeredText(this.font, hint, centerX, centerY - this.font.lineHeight / 2, COLOR_TEXT);
	}

	private static NonNullList<ItemStack> readContents(final ItemStack box) {
		NonNullList<ItemStack> items = NonNullList.withSize(GRID_SLOTS, ItemStack.EMPTY);
		ItemContainerContents contents = box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
		contents.copyInto(items);
		return items;
	}
}
