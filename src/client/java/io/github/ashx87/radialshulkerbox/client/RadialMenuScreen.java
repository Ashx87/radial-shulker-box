package io.github.ashx87.radialshulkerbox.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RadialMenuScreen extends Screen {
	private static final int RING_RADIUS = 70;
	private static final int DEAD_ZONE_RADIUS = 20;
	private static final int ICON_HALF_SIZE = 8;

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
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float partialTick) {
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		int count = this.entries.size();

		this.hoveredIndex = -1;
		if (count > 0) {
			double dx = mouseX - centerX;
			double dy = mouseY - centerY;
			double distance = Math.sqrt(dx * dx + dy * dy);
			if (distance > DEAD_ZONE_RADIUS) {
				double angle = Math.atan2(dy, dx) + Math.PI / 2.0;
				if (angle < 0) {
					angle += Math.PI * 2.0;
				}
				this.hoveredIndex = (int) (angle / (Math.PI * 2.0) * count) % count;
			}
		}

		for (int i = 0; i < count; i++) {
			ShulkerBoxScanner.Entry entry = this.entries.get(i);
			double sectorAngle = (Math.PI * 2.0 / count) * i - Math.PI / 2.0;
			int iconX = centerX + (int) (Math.cos(sectorAngle) * RING_RADIUS) - ICON_HALF_SIZE;
			int iconY = centerY + (int) (Math.sin(sectorAngle) * RING_RADIUS) - ICON_HALF_SIZE;

			boolean hovered = i == this.hoveredIndex;
			int highlightColor = hovered ? 0x80FFFFFF : 0x40000000;
			graphics.fill(iconX - 4, iconY - 4, iconX + ICON_HALF_SIZE * 2 + 4, iconY + ICON_HALF_SIZE * 2 + 4, highlightColor);
			graphics.item(entry.stack(), iconX, iconY);

			Component name = entry.stack().getHoverName();
			int textWidth = this.font.width(name);
			int nameX = centerX + (int) (Math.cos(sectorAngle) * RING_RADIUS) - textWidth / 2;
			int nameY = iconY + ICON_HALF_SIZE * 2 + 6;
			graphics.text(this.font, name, nameX, nameY, 0xFFFFFFFF, true);
		}
	}
}
