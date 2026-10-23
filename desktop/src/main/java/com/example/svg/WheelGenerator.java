package com.example.svg;

import com.example.core.geometry.WheelGeometry;
import com.example.core.geometry.WheelPalette;
import com.example.model.Participant;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

import java.util.List;

/**
 * Generates the SVG-based roulette wheel from a list of participants.
 *
 * <p>All underlying SVG path geometries, slice angles, label placements,
 * and color palettes are shared via {@link WheelGeometry} and {@link WheelPalette}
 * in {@code :core} so that Desktop and Mobile render identical geometry.
 */
public class WheelGenerator {

    /** Default wheel radius in pixels. */
    public static final double DEFAULT_RADIUS = WheelGeometry.DEFAULT_RADIUS;

    /** Hard upper bound — above this, slices are too thin to be useful. */
    public static final int MAX_PARTICIPANTS = WheelGeometry.MAX_PARTICIPANTS;

    /** Labels are truncated beyond this many characters. */
    private static final int MAX_LABEL_CHARS = WheelGeometry.MAX_LABEL_CHARS;

    /** Label is positioned this fraction of the way from centre to rim. */
    private static final double LABEL_RADIAL_FRACTION = WheelGeometry.LABEL_RADIAL_FRACTION;

    private final WheelPalette palette = WheelPalette.getInstance();

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Builds and returns a {@link Group} containing all wheel slices, labels,
     * and the fixed pointer triangle — centred at (0, 0).
     *
     * @param participants the names to place on the wheel
     * @param radius       the wheel radius in pixels
     * @return the complete, ready-to-add wheel Group
     * @throws IllegalArgumentException if {@code participants.size() > MAX_PARTICIPANTS}
     *                                  or if {@code participants} is empty
     */
    public Group generateWheel(List<? extends Participant> participants, double radius) {
        int n = participants.size();
        if (n == 0) throw new IllegalArgumentException("Need at least 1 participant");
        if (n > MAX_PARTICIPANTS)
            throw new IllegalArgumentException("Too many participants (max " + MAX_PARTICIPANTS + ")");

        Group wheel = new Group();

        if (n == 1) {
            String name = participants.get(0).getName();
            wheel.getChildren().add(buildFullCircle(name, radius));
            wheel.getChildren().add(buildSingleParticipantLabel(name));
        } else {
            double fontSize = WheelGeometry.adaptiveFontSize(n);

            for (int i = 0; i < n; i++) {
                Color fill  = Color.web(palette.getSliceColor(i));
                String label = WheelGeometry.truncate(participants.get(i).getName());

                wheel.getChildren().add(buildSlice(i, n, radius, fill));
                wheel.getChildren().add(buildLabel(label, i, n, radius, fontSize, fill));
            }
        }

        // Thin divider lines between slices for polish
        if (n > 1) {
            for (int i = 0; i < n; i++) {
                wheel.getChildren().add(buildDivider(i, n, radius));
            }
        }

        // Sleek central hub capping the slice vertices
        wheel.getChildren().add(buildCenterHub(radius));

        // Fixed pointer at 12-o'clock (not part of the rotating group)
        wheel.getChildren().add(buildPointer(radius));

        return wheel;
    }

    /**
     * Exposes the pointer separately so the caller can keep it fixed while
     * the wheel rotates. Call {@link #buildPointerOnly(double)} to get just
     * the pointer node to overlay on top of the wheel in the StackPane.
     */
    public SVGPath buildPointerOnly(double radius) {
        return buildPointer(radius);
    }

    // ── Slice builders ─────────────────────────────────────────────────────────

    private SVGPath buildSlice(int index, int totalCount, double r, Color fill) {
        String d = WheelGeometry.slicePath(index, totalCount, r);
        SVGPath path = new SVGPath();
        path.setContent(d);
        path.setFill(fill);
        path.setStroke(Color.TRANSPARENT);
        return path;
    }

    /** Single-participant special case: a plain filled circle. */
    private SVGPath buildFullCircle(String name, double r) {
        String d = WheelGeometry.fullCirclePath(r);
        SVGPath circle = new SVGPath();
        circle.setContent(d);
        circle.setFill(Color.web(palette.getSliceColor(0)));
        circle.setStroke(Color.TRANSPARENT);
        return circle;
    }

    /** Thin radial line dividing two slices (purely decorative). */
    private SVGPath buildDivider(int index, int totalCount, double r) {
        String d = WheelGeometry.dividerPath(index, totalCount, r);
        SVGPath line = new SVGPath();
        line.setContent(d);
        line.setStroke(Color.color(0, 0, 0, 0.25));
        line.setFill(Color.TRANSPARENT);
        return line;
    }

    // ── Label builder ──────────────────────────────────────────────────────────

    /**
     * Builds a rotated {@link Text} label placed at the mid-angle of the slice,
     * {@link #LABEL_RADIAL_FRACTION} of the way from centre to rim.
     */
    private Text buildLabel(String label, int index, int totalCount,
                            double r, double fontSize, Color sliceFill) {

        WheelGeometry.LabelPosition pos = WheelGeometry.labelPosition(index, totalCount, r);

        Text text = new Text(label);
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));
        text.setFill(contrastColor(sliceFill));

        // Centre the text on its anchor point
        text.setX(pos.x() - text.getLayoutBounds().getWidth()  / 2.0);
        text.setY(pos.y() + text.getLayoutBounds().getHeight() / 4.0);

        // Rotate so the text reads outward along the radius from centre to rim
        Rotate rotate = new Rotate(pos.rotationDegrees(), pos.x(), pos.y());
        text.getTransforms().add(rotate);

        return text;
    }

    /** Label for the single-participant fallback wheel. */
    private Text buildSingleParticipantLabel(String name) {
        Text text = new Text(WheelGeometry.truncate(name));
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18.0));
        text.setFill(contrastColor(Color.web(palette.getSliceColor(0))));
        text.setX(-text.getLayoutBounds().getWidth()  / 2.0);
        text.setY(text.getLayoutBounds().getHeight() / 4.0);
        return text;
    }

    /**
     * Polished hub capping the center of the wheel where slice points converge.
     */
    private Group buildCenterHub(double r) {
        Group hub = new Group();
        WheelGeometry.CenterHub hubDims = WheelGeometry.centerHub(r);

        Circle outer = new Circle(0, 0, hubDims.outerRadius());
        outer.setFill(Color.web(palette.getHubOuterColor()));
        outer.setStroke(Color.web("#334155"));
        outer.setStrokeWidth(2.5);

        Circle inner = new Circle(0, 0, hubDims.innerRadius());
        inner.setFill(Color.web(palette.getHubInnerColor()));
        inner.setStroke(Color.web("#818cf8"));
        inner.setStrokeWidth(1.5);

        hub.getChildren().addAll(outer, inner);
        return hub;
    }

    // ── Pointer ────────────────────────────────────────────────────────────────

    /**
     * Builds a downward-pointing triangle at the top of the wheel (12-o'clock).
     * The pointer is meant to be kept stationary while the wheel rotates.
     */
    private SVGPath buildPointer(double r) {
        String d = WheelGeometry.pointerPath(r);

        SVGPath pointer = new SVGPath();
        pointer.setContent(d);
        pointer.setFill(Color.web(palette.getPointerColor()));
        pointer.setStroke(Color.web("#0b1120"));
        pointer.setStrokeWidth(2);
        pointer.setId("wheel-pointer");
        return pointer;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Returns white or a dark colour depending on the perceived luminance of
     * {@code bg}, so the label is always readable.
     */
    private Color contrastColor(Color bg) {
        String hex = String.format("#%02x%02x%02x",
                (int) Math.round(bg.getRed() * 255),
                (int) Math.round(bg.getGreen() * 255),
                (int) Math.round(bg.getBlue() * 255));
        return Color.web(WheelPalette.getContrastTextColor(hex));
    }
}
