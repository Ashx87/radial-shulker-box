package io.github.ashx87.radialshulkerbox.client;

/** Pure angle math for the radial menu. No Minecraft imports so it is unit-testable. */
public final class RadialMenuMath {
	private RadialMenuMath() {
	}

	/**
	 * Maps a cursor offset from the ring centre to a segment index, or -1 when inside
	 * the dead zone or when there are no segments. Segments are drawn centered on
	 * angle (2π/count · i − π/2), i.e. segment 0 points straight up; the raw angle is
	 * shifted by half a segment so each hover range is centered on its segment's
	 * visual center instead of straddling two segments.
	 */
	public static int hoveredSegment(final double dx, final double dy, final int count, final double deadZoneRadius) {
		if (count <= 0) {
			return -1;
		}
		double distance = Math.sqrt(dx * dx + dy * dy);
		if (distance <= deadZoneRadius) {
			return -1;
		}
		double segArc = Math.PI * 2.0 / count;
		double angle = Math.atan2(dy, dx) + Math.PI / 2.0 + segArc / 2.0;
		angle %= Math.PI * 2.0;
		if (angle < 0) {
			angle += Math.PI * 2.0;
		}
		return (int) (angle / segArc) % count;
	}
}
