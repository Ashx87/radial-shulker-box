package io.github.ashx87.radialshulkerbox.client;

import com.mojang.blaze3d.platform.NativeImage;

import io.github.ashx87.radialshulkerbox.RadialShulkerBox;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuScreen extends Screen {
	private static final int BASE_INNER_RADIUS = 100;
	private static final int BASE_OUTER_RADIUS = 135;
	private static final int BASE_ICON_RADIUS = 117;
	private static final int ICON_HALF_SIZE = 8;
	// Minimum arc length (gui px) per segment at the icon ring; below this the icons
	// overlap, so the whole ring scales up instead (clamped to the window size).
	private static final int MIN_SEGMENT_ARC = 26;
	private static final int SCREEN_MARGIN = 10;

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
	private int innerRadius = BASE_INNER_RADIUS;
	private int outerRadius = BASE_OUTER_RADIUS;
	private int iconRadius = BASE_ICON_RADIUS;

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
		// close path calls setScreen(null) directly.
		super.onClose();
		RadialShulkerBoxClient.onMenuDismissed();
	}

	// While a screen is open, mouse button releases never reach KeyMapping (unlike
	// keyboard releases), so a mouse-bound hotkey's isDown would stay true forever
	// and the tick loop would never close the menu. The screen therefore handles the
	// release itself for both input types.
	@Override
	public boolean mouseReleased(final MouseButtonEvent event) {
		if (RadialShulkerBoxClient.matchesOpenMouse(event)) {
			RadialShulkerBoxClient.closeAndSelect(this.minecraft, this);
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyReleased(final KeyEvent event) {
		if (RadialShulkerBoxClient.matchesOpenKey(event)) {
			RadialShulkerBoxClient.closeAndSelect(this.minecraft, this);
			return true;
		}
		return super.keyReleased(event);
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

		updateRadii(count);
		this.hoveredIndex = computeHovered(mouseX - centerX, mouseY - centerY, count);

		drawRing(graphics, centerX, centerY, count);
		drawIcons(graphics, centerX, centerY, count);
		drawCenterPanel(graphics, centerX, centerY);
	}

	private void updateRadii(final int count) {
		double neededIconRadius = MIN_SEGMENT_ARC * count / (Math.PI * 2.0);
		double scale = Math.max(1.0, neededIconRadius / BASE_ICON_RADIUS);
		double maxScale = (Math.min(this.width, this.height) / 2.0 - SCREEN_MARGIN) / BASE_OUTER_RADIUS;
		scale = Math.min(scale, Math.max(1.0, maxScale));
		this.innerRadius = (int) Math.round(BASE_INNER_RADIUS * scale);
		this.outerRadius = (int) Math.round(BASE_OUTER_RADIUS * scale);
		this.iconRadius = (int) Math.round(BASE_ICON_RADIUS * scale);
	}

	private int computeHovered(final double dx, final double dy, final int count) {
		// The dead zone is the ring's inner circle.
		return RadialMenuMath.hoveredSegment(dx, dy, count, this.innerRadius);
	}

	/**
	 * One horizontal run of ring pixels in physical (framebuffer) coordinates,
	 * relative to the ring centre. Colors are precomputed for both hover states so
	 * hovering only repaints pixels and never re-rasterizes the geometry.
	 */
	private record RingSpan(int x, int y, int width, int segment, int baseColor, int hoverColor) {
	}

	private static final Identifier RING_TEXTURE_ID = RadialShulkerBox.id("radial_ring");

	private List<RingSpan> ringSpans;
	private DynamicTexture ringTexture;
	private int ringTextureSize = -1;
	private int ringBound;
	private int spanCount = -1;
	private int spanGuiScale = -1;
	private int spanOuterRadius = -1;
	private int paintedHover = -1;

	// The ring is rasterized once at physical-pixel resolution into a texture (every
	// framebuffer pixel painted exactly once, edge alpha scaled by pixel coverage for
	// anti-aliasing) and each frame draws that texture as a single quad. Issuing the
	// ring as thousands of per-frame fill() quads instead stalled the new gui
	// pipeline badly, and the even older rotated-quad approach double-blended into
	// dark stripes. Hover changes only repaint the two affected segments' pixels.
	private void drawRing(final GuiGraphicsExtractor graphics, final int centerX, final int centerY, final int count) {
		int guiScale = Math.max(1, this.minecraft.getWindow().getGuiScale());
		if (this.ringSpans == null || count != this.spanCount || guiScale != this.spanGuiScale
				|| this.outerRadius != this.spanOuterRadius) {
			rebuildRingTexture(count, guiScale);
			this.spanCount = count;
			this.spanGuiScale = guiScale;
			this.spanOuterRadius = this.outerRadius;
		}
		if (this.hoveredIndex != this.paintedHover) {
			repaintHover(this.paintedHover, this.hoveredIndex);
			this.paintedHover = this.hoveredIndex;
			this.ringTexture.upload();
		}

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(centerX, centerY);
		float inverseScale = 1.0f / guiScale;
		pose.scale(inverseScale, inverseScale);
		graphics.blit(RING_TEXTURE_ID, -this.ringBound, -this.ringBound, this.ringBound, this.ringBound,
			0.0f, 1.0f, 0.0f, 1.0f);
		pose.popMatrix();
	}

	@Override
	public void removed() {
		super.removed();
		// Frees the texture's native and gpu memory; rebuilt on next open.
		this.minecraft.getTextureManager().release(RING_TEXTURE_ID);
		this.ringTexture = null;
		this.ringSpans = null;
	}

	private void rebuildRingTexture(final int count, final int guiScale) {
		this.ringSpans = buildRingSpans(count, guiScale);
		int size = this.ringBound * 2;
		if (this.ringTexture == null || this.ringTextureSize != size) {
			this.ringTexture = new DynamicTexture("radial-shulker-box ring", size, size, true);
			// register() closes any texture previously under this id, so re-registering
			// after a resize does not leak.
			this.minecraft.getTextureManager().register(RING_TEXTURE_ID, this.ringTexture);
			this.ringTextureSize = size;
		} else {
			this.ringTexture.getPixels().fillRect(0, 0, size, size, 0);
		}

		NativeImage image = this.ringTexture.getPixels();
		for (RingSpan span : this.ringSpans) {
			paintSpan(image, span, span.baseColor());
		}
		this.paintedHover = -1;
		this.ringTexture.upload();
	}

	private void repaintHover(final int previous, final int current) {
		NativeImage image = this.ringTexture.getPixels();
		for (RingSpan span : this.ringSpans) {
			if (span.segment() == previous) {
				paintSpan(image, span, span.baseColor());
			} else if (span.segment() == current) {
				paintSpan(image, span, span.hoverColor());
			}
		}
	}

	private void paintSpan(final NativeImage image, final RingSpan span, final int color) {
		image.fillRect(span.x() + this.ringBound, span.y() + this.ringBound, span.width(), 1, color);
	}

	private List<RingSpan> buildRingSpans(final int count, final int guiScale) {
		List<RingSpan> spans = new ArrayList<>();
		double inner = (double) this.innerRadius * guiScale;
		double outer = (double) this.outerRadius * guiScale;
		double segArc = Math.PI * 2.0 / count;
		double gapRad = Math.min(0.06, segArc * 0.10);
		// Pad by one pixel so the anti-aliased fringe just outside the outer radius
		// is not clipped by the scan bounds.
		int bound = (int) Math.ceil(outer) + 1;
		double boundSq = (double) bound * bound;
		this.ringBound = bound;

		for (int y = -bound; y < bound; y++) {
			double yc = y + 0.5;
			int half = (int) Math.ceil(Math.sqrt(boundSq - yc * yc));
			int runStart = 0;
			long runKey = -1;
			for (int x = -half; x <= half; x++) {
				long key = x < half ? ringSample(x + 0.5, yc, inner, outer, count, segArc, gapRad) : -1;
				if (key != runKey) {
					if (runKey != -1) {
						int segment = (int) (runKey >> 8);
						int coverage = (int) (runKey & 0xFF);
						spans.add(new RingSpan(runStart, y, x - runStart, segment,
							applyCoverage(COLOR_RING_BASE, coverage), applyCoverage(COLOR_RING_HOVER, coverage)));
					}
					runStart = x;
					runKey = key;
				}
			}
		}
		return spans;
	}

	/**
	 * Samples the ring at this physical-pixel offset from the centre. Returns -1 for
	 * no coverage, else (segment << 8 | coverage) where coverage is 0-255 of how much
	 * of the pixel the ring covers (signed-distance approximation at the arc and gap
	 * edges — this is what anti-aliases the outline).
	 */
	private static long ringSample(final double xc, final double yc, final double inner, final double outer,
			final int count, final double segArc, final double gapRad) {
		double distSq = xc * xc + yc * yc;
		double innerMin = Math.max(0.0, inner - 1.0);
		double outerMax = outer + 1.0;
		if (distSq < innerMin * innerMin || distSq > outerMax * outerMax) {
			return -1;
		}
		double dist = Math.sqrt(distSq);
		// Signed distance (px) to the nearest circular edge; positive = inside the ring.
		double radial = Math.min(outer - dist, dist - inner);
		// Same half-segment shift as RadialMenuMath.hoveredSegment so the painted
		// segment always matches the hover selection.
		double angle = Math.atan2(yc, xc) + Math.PI / 2.0 + segArc / 2.0;
		angle %= Math.PI * 2.0;
		if (angle < 0) {
			angle += Math.PI * 2.0;
		}
		int index = Math.min((int) (angle / segArc), count - 1);
		double frac = angle - index * segArc;
		// Signed distance (px) to the nearest gap edge, converted from radians.
		double angular = Math.min(frac - gapRad, segArc - gapRad - frac) * dist;
		double coverage = clamp01(radial + 0.5) * clamp01(angular + 0.5);
		int coverage255 = (int) (coverage * 255.0 + 0.5);
		if (coverage255 == 0) {
			return -1;
		}
		return ((long) index << 8) | coverage255;
	}

	private static int applyCoverage(final int color, final int coverage255) {
		int alpha = ((color >>> 24) * coverage255 + 127) / 255;
		return (alpha << 24) | (color & 0x00FFFFFF);
	}

	private static double clamp01(final double value) {
		return value < 0.0 ? 0.0 : Math.min(value, 1.0);
	}

	private void drawIcons(final GuiGraphicsExtractor graphics, final int centerX, final int centerY, final int count) {
		double segArc = Math.PI * 2.0 / count;
		for (int i = 0; i < count; i++) {
			double a = segArc * i - Math.PI / 2.0;
			int iconX = centerX + (int) (Math.cos(a) * this.iconRadius) - ICON_HALF_SIZE;
			int iconY = centerY + (int) (Math.sin(a) * this.iconRadius) - ICON_HALF_SIZE;
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
