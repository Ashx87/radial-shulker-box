package io.github.ashx87.radialshulkerbox.client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RadialMenuMathTest {
	private static final double DEAD_ZONE = 100.0;

	@ParameterizedTest(name = "count={2}, cursor=({0},{1}) -> segment {3}")
	@CsvSource({
		// Four segments: 0 = up, 1 = right, 2 = down, 3 = left.
		"0, -200, 4, 0",
		"200, 0, 4, 1",
		"0, 200, 4, 2",
		"-200, 0, 4, 3",
		// Diagonals sit at the shifted boundary's centre-side: up-right belongs to
		// whichever segment centre is nearer; exactly 45° falls into segment 1
		// because the half-segment shift maps [0°..90°) of raw angle to it.
		"141, -141, 4, 1",
		// A single segment owns the full circle.
		"0, -200, 1, 0",
		"57, 193, 1, 0",
		// Two segments: up half vs down half.
		"0, -200, 2, 0",
		"0, 200, 2, 1",
		// Just outside the dead zone still counts.
		"0, -101, 4, 0"
	})
	void hoveredSegmentMapsCursorToSegment(final double dx, final double dy, final int count, final int expected) {
		assertEquals(expected, RadialMenuMath.hoveredSegment(dx, dy, count, DEAD_ZONE));
	}

	@ParameterizedTest(name = "cursor=({0},{1}) inside dead zone")
	@CsvSource({
		"0, 0",
		"0, -100",
		"70, 70",
		"-99, 0"
	})
	void insideDeadZoneReturnsMinusOne(final double dx, final double dy) {
		assertEquals(-1, RadialMenuMath.hoveredSegment(dx, dy, 8, DEAD_ZONE));
	}

	@ParameterizedTest(name = "count={0} has no segments")
	@CsvSource({
		"0",
		"-1"
	})
	void nonPositiveCountReturnsMinusOne(final int count) {
		assertEquals(-1, RadialMenuMath.hoveredSegment(0, -200, count, DEAD_ZONE));
	}
}
