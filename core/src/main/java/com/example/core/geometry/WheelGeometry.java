package com.example.core.geometry;

import java.util.Locale;

/**
 * Shared mathematical model and SVG path-data generator for the roulette wheel.
 *
 * <p>Centred at {@code (0, 0)}. Angles are measured clockwise from 12 o'clock (0°).
 * Both Desktop (JavaFX) and Android (Jetpack Compose Canvas) consume these exact
 * path strings and label coordinates so wheel appearance is visually identical
 * across platforms.
 */
public final class WheelGeometry {

    /** Default wheel radius in pixels. */
    public static final double DEFAULT_RADIUS = 280.0;

    /** Hard upper bound for participants on the wheel. */
    public static final int MAX_PARTICIPANTS = 40;

    /** Maximum characters to display on a slice label before truncation. */
    public static final int MAX_LABEL_CHARS = 14;

    /** Label radial fraction from center (0.0) to rim (1.0). */
    public static final double LABEL_RADIAL_FRACTION = 0.62;

    private WheelGeometry() {}

    /**
     * Generates the SVG path-data string for a single pie slice arc.
     *
     * <pre>
     *   M 0,0
     *   L x1,y1
     *   A r,r 0 largeArc,1 x2,y2
     *   Z
     * </pre>
     *
     * @param index      0-based slice index
     * @param totalCount total number of slices (>= 2)
     * @param radius     radius in pixels
     * @return SVG path data string
     */
    public static String slicePath(int index, int totalCount, double radius) {
        if (totalCount < 2) {
            throw new IllegalArgumentException("totalCount must be at least 2");
        }
        if (index < 0 || index >= totalCount) {
            throw new IndexOutOfBoundsException("Slice index " + index + " out of bounds for " + totalCount);
        }

        double sliceDeg = 360.0 / totalCount;
        double startDeg = index * sliceDeg;

        double startRad = Math.toRadians(startDeg - 90); // -90 so 0° == 12 o'clock
        double endRad   = Math.toRadians(startDeg + sliceDeg - 90);

        double x1 = radius * Math.cos(startRad);
        double y1 = radius * Math.sin(startRad);
        double x2 = radius * Math.cos(endRad);
        double y2 = radius * Math.sin(endRad);

        int largeArc = (sliceDeg > 180.0) ? 1 : 0;

        return String.format(Locale.US,
                "M 0,0 L %.4f,%.4f A %.4f,%.4f 0 %d,1 %.4f,%.4f Z",
                x1, y1, radius, radius, largeArc, x2, y2);
    }

    /**
     * Generates the SVG path-data string for a full circle (used for 1 participant).
     */
    public static String fullCirclePath(double radius) {
        return String.format(Locale.US,
                "M %.4f,0 A %.4f,%.4f 0 1,1 %.4f,0 A %.4f,%.4f 0 1,1 %.4f,0 Z",
                radius, radius, radius, -radius, radius, radius, radius);
    }

    /**
     * Generates the SVG path-data string for a radial divider line between slices.
     */
    public static String dividerPath(int index, int totalCount, double radius) {
        if (totalCount < 2) {
            throw new IllegalArgumentException("totalCount must be at least 2");
        }
        double sliceDeg = 360.0 / totalCount;
        double startDeg = index * sliceDeg;
        double rad = Math.toRadians(startDeg - 90);
        double x   = radius * Math.cos(rad);
        double y   = radius * Math.sin(rad);
        return String.format(Locale.US, "M 0,0 L %.4f,%.4f", x, y);
    }

    /**
     * Generates the SVG path-data string for the stationary 12 o'clock pointer triangle.
     */
    public static String pointerPath(double radius) {
        double tipY  = -(radius - 12.0);
        double baseY = -(radius + 16.0);
        double halfW = 12.0;

        return String.format(Locale.US,
                "M 0,%.4f L %.4f,%.4f L %.4f,%.4f Z",
                tipY, -halfW, baseY, halfW, baseY);
    }

    /**
     * Computes the center position and rotation angle for slice label text.
     *
     * @param index      0-based slice index
     * @param totalCount total number of slices
     * @param radius     wheel radius in pixels
     * @return {@link LabelPosition} with anchor coordinates and rotation angle in degrees
     */
    public static LabelPosition labelPosition(int index, int totalCount, double radius) {
        if (totalCount < 1) {
            throw new IllegalArgumentException("totalCount must be at least 1");
        }
        if (totalCount == 1) {
            return new LabelPosition(0.0, 0.0, 0.0);
        }

        double sliceDeg = 360.0 / totalCount;
        double startDeg = index * sliceDeg;
        double midAngleDeg = startDeg + sliceDeg / 2.0;
        double midAngleRad = Math.toRadians(midAngleDeg - 90);

        double lx = radius * LABEL_RADIAL_FRACTION * Math.cos(midAngleRad);
        double ly = radius * LABEL_RADIAL_FRACTION * Math.sin(midAngleRad);
        double rotationDeg = midAngleDeg - 90.0; // reads radially outward

        return new LabelPosition(lx, ly, rotationDeg);
    }

    /**
     * Returns an adaptive font size that scales down gracefully as slices become narrower.
     */
    public static double adaptiveFontSize(int totalCount) {
        if (totalCount <= 8)  return 13.0;
        if (totalCount <= 15) return 11.0;
        if (totalCount <= 25) return 9.5;
        return 8.0;
    }

    /**
     * Truncates participant name to {@value #MAX_LABEL_CHARS} characters with an ellipsis.
     */
    public static String truncate(String name) {
        return truncate(name, MAX_LABEL_CHARS);
    }

    /**
     * Truncates name to {@code maxChars} with an ellipsis.
     */
    public static String truncate(String name, int maxChars) {
        if (name == null) return "";
        if (name.length() <= maxChars) return name;
        return name.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    /**
     * Computes center hub outer and inner radii proportional to the wheel radius.
     */
    public static CenterHub centerHub(double radius) {
        double outerR = Math.max(22.0, radius * 0.12);
        double innerR = outerR * 0.45;
        return new CenterHub(outerR, innerR);
    }

    /**
     * Represents the anchor coordinates and rotation degrees for slice label text.
     */
    public record LabelPosition(double x, double y, double rotationDegrees) {}

    /**
     * Dimensions for the decorative center hub.
     */
    public record CenterHub(double outerRadius, double innerRadius) {}
}
