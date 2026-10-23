package com.example.core.geometry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class WheelGeometryTest {

    @ParameterizedTest(name = "slicePath generates valid SVG path data for {0} participants")
    @ValueSource(ints = {2, 3, 7, 20, 40})
    void slicePathGeneratesValidSvgPaths(int n) {
        for (int i = 0; i < n; i++) {
            String path = WheelGeometry.slicePath(i, n, WheelGeometry.DEFAULT_RADIUS);
            assertNotNull(path);
            assertTrue(path.startsWith("M 0,0 L "), "Path must begin with center move and line to rim: " + path);
            assertTrue(path.contains(" A 280"), "Path must contain arc command with radius: " + path);
            assertTrue(path.endsWith(" Z"), "Path must close back to center: " + path);
        }
    }

    @Test
    @DisplayName("fullCirclePath generates valid circle for single participant")
    void fullCirclePath() {
        String path = WheelGeometry.fullCirclePath(WheelGeometry.DEFAULT_RADIUS);
        assertNotNull(path);
        assertTrue(path.startsWith("M 280"));
        assertTrue(path.contains("A 280"));
        assertTrue(path.endsWith("Z"));
    }

    @ParameterizedTest(name = "dividerPath generates valid line for {0} participants")
    @ValueSource(ints = {2, 7, 20})
    void dividerPathGeneratesLines(int n) {
        for (int i = 0; i < n; i++) {
            String divider = WheelGeometry.dividerPath(i, n, WheelGeometry.DEFAULT_RADIUS);
            assertNotNull(divider);
            assertTrue(divider.startsWith("M 0,0 L "));
        }
    }

    @Test
    @DisplayName("pointerPath generates stationary top triangle")
    void pointerPathGeneratesTopTriangle() {
        String pointer = WheelGeometry.pointerPath(WheelGeometry.DEFAULT_RADIUS);
        assertNotNull(pointer);
        assertTrue(pointer.startsWith("M 0,-268")); // -(280 - 12)
        assertTrue(pointer.endsWith("Z"));
    }

    @ParameterizedTest(name = "labelPosition calculates valid coordinates and rotation for {0} participants")
    @ValueSource(ints = {2, 3, 7, 20})
    void labelPositionValidCoordinates(int n) {
        for (int i = 0; i < n; i++) {
            WheelGeometry.LabelPosition pos = WheelGeometry.labelPosition(i, n, WheelGeometry.DEFAULT_RADIUS);
            assertNotNull(pos);

            // Radius distance from center should equal DEFAULT_RADIUS * LABEL_RADIAL_FRACTION
            double expectedDistance = WheelGeometry.DEFAULT_RADIUS * WheelGeometry.LABEL_RADIAL_FRACTION;
            double actualDistance = Math.hypot(pos.x(), pos.y());
            assertEquals(expectedDistance, actualDistance, 1e-3,
                    "Label distance from center must match radial fraction");
        }
    }

    @Test
    @DisplayName("adaptiveFontSize decreases gracefully with participant count")
    void adaptiveFontSizeDecreases() {
        double fs4 = WheelGeometry.adaptiveFontSize(4);
        double fs12 = WheelGeometry.adaptiveFontSize(12);
        double fs20 = WheelGeometry.adaptiveFontSize(20);
        double fs35 = WheelGeometry.adaptiveFontSize(35);

        assertTrue(fs4 >= fs12);
        assertTrue(fs12 >= fs20);
        assertTrue(fs20 >= fs35);
    }

    @Test
    @DisplayName("truncate caps names at maxChars with ellipsis")
    void truncate() {
        assertEquals("Alice", WheelGeometry.truncate("Alice", 10));
        assertEquals("Supercalifrag…", WheelGeometry.truncate("Supercalifragilistic", 14));
        assertEquals("", WheelGeometry.truncate(null));
    }

    @Test
    @DisplayName("centerHub computes proportional dimensions")
    void centerHubDimensions() {
        WheelGeometry.CenterHub hub = WheelGeometry.centerHub(280.0);
        assertEquals(33.6, hub.outerRadius(), 1e-2);
        assertEquals(15.12, hub.innerRadius(), 1e-2);
    }
}
