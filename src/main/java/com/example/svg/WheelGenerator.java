package com.example.svg;

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
import java.util.Locale;

/**
 * Generates the SVG-based roulette wheel from a list of participants.
 *
 * <h2>Layout</h2>
 * <ul>
 *   <li>The wheel is centred at {@code (0, 0)} so the caller can simply place
 *       the returned {@link Group} inside a {@code StackPane}.</li>
 *   <li>One {@link SVGPath} arc per participant, filled from {@link #PALETTE}.</li>
 *   <li>Each slice carries a {@link Text} label rotated to its mid-angle,
 *       positioned half-way along the radius.</li>
 *   <li>A fixed {@link SVGPath} pointer triangle sits above the wheel at the
 *       12-o'clock position to mark the selected slice.</li>
 * </ul>
 *
 * <h2>Edge cases handled</h2>
 * <ul>
 *   <li><b>1 participant</b> — drawn as a filled circle (arc degenerates).</li>
 *   <li><b>Very long names</b> — truncated with an ellipsis at
 *       {@link #MAX_LABEL_CHARS} characters.</li>
 *   <li><b>Many participants (≥ 20)</b> — font size shrinks to keep labels
 *       readable; above {@link #MAX_PARTICIPANTS} the method throws
 *       {@link IllegalArgumentException}.</li>
 * </ul>
 */
public class WheelGenerator {

    /** Default wheel radius in pixels. */
    public static final double DEFAULT_RADIUS = 280.0;

    /** Hard upper bound — above this, slices are too thin to be useful. */
    public static final int MAX_PARTICIPANTS = 40;

    /** Labels are truncated beyond this many characters. */
    private static final int MAX_LABEL_CHARS = 14;

    /** Label is positioned this fraction of the way from centre to rim. */
    private static final double LABEL_RADIAL_FRACTION = 0.62;

    // ── Curated 12-colour palette (dark-mode friendly, high contrast) ──────────
    private static final Color[] PALETTE = {
        Color.web("#6366f1"),  // indigo
        Color.web("#ec4899"),  // pink
        Color.web("#f59e0b"),  // amber
        Color.web("#10b981"),  // emerald
        Color.web("#3b82f6"),  // blue
        Color.web("#ef4444"),  // red
        Color.web("#8b5cf6"),  // violet
        Color.web("#14b8a6"),  // teal
        Color.web("#f97316"),  // orange
        Color.web("#06b6d4"),  // cyan
        Color.web("#a3e635"),  // lime
        Color.web("#e879f9"),  // fuchsia
    };

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
            double sliceAngle = 360.0 / n;
            double fontSize   = adaptiveFontSize(n);

            for (int i = 0; i < n; i++) {
                double startDeg = i * sliceAngle;
                Color  fill     = PALETTE[i % PALETTE.length];
                String label    = truncate(participants.get(i).getName());

                wheel.getChildren().add(buildSlice(startDeg, sliceAngle, radius, fill));
                wheel.getChildren().add(buildLabel(label, startDeg, sliceAngle, radius, fontSize, fill));
            }
        }

        // Thin divider lines between slices for polish
        if (n > 1) {
            double sliceAngle = 360.0 / n;
            for (int i = 0; i < n; i++) {
                wheel.getChildren().add(buildDivider(i * sliceAngle, radius));
            }
        }

        // Sleek central hub capping the slice vertices
        wheel.getChildren().add(buildCenterHub(radius));

        // Fixed pointer at 12-o'clock (not part of the rotating group)
        // The caller wraps the wheel in a StackPane and adds the pointer separately;
        // for convenience we return it as the last child tagged with fx:id "pointer"
        wheel.getChildren().add(buildPointer(radius));

        return wheel;
    }

    /**
     * Exposes the pointer separately so the caller can keep it fixed while
     * the wheel rotates.  Call {@link #buildPointerOnly(double)} to get just
     * the pointer node to overlay on top of the wheel in the StackPane.
     */
    public SVGPath buildPointerOnly(double radius) {
        return buildPointer(radius);
    }

    // ── Slice builders ─────────────────────────────────────────────────────────

    /**
     * Builds one pie-slice as an SVGPath arc command.
     *
     * <pre>
     *   M 0,0  — move to centre
     *   L x1,y1  — line to start of arc on the rim
     *   A r,r 0 largeArcFlag,1 x2,y2  — arc to end of rim
     *   Z  — close back to centre
     * </pre>
     *
     * Angles are measured clockwise from the top (12-o'clock = 0°).
     */
    private SVGPath buildSlice(double startDeg, double sliceDeg, double r, Color fill) {
        double startRad = Math.toRadians(startDeg - 90); // -90 so 0° == top
        double endRad   = Math.toRadians(startDeg + sliceDeg - 90);

        double x1 = r * Math.cos(startRad);
        double y1 = r * Math.sin(startRad);
        double x2 = r * Math.cos(endRad);
        double y2 = r * Math.sin(endRad);

        int largeArc = (sliceDeg > 180) ? 1 : 0;

        String d = String.format(Locale.US,
                "M 0,0 L %.4f,%.4f A %.4f,%.4f 0 %d,1 %.4f,%.4f Z",
                x1, y1, r, r, largeArc, x2, y2);

        SVGPath path = new SVGPath();
        path.setContent(d);
        path.setFill(fill);
        path.setStroke(Color.TRANSPARENT);
        return path;
    }

    /** Single-participant special case: a plain filled circle. */
    private SVGPath buildFullCircle(String name, double r) {
        // Two 180° arcs to avoid arc-degeneration when start == end
        String d = String.format(Locale.US,
                "M %.4f,0 A %.4f,%.4f 0 1,1 %.4f,0 A %.4f,%.4f 0 1,1 %.4f,0 Z",
                r, r, r, -r, r, r, r);
        SVGPath circle = new SVGPath();
        circle.setContent(d);
        circle.setFill(PALETTE[0]);
        circle.setStroke(Color.TRANSPARENT);
        return circle;
    }

    /** Thin radial line dividing two slices (purely decorative). */
    private SVGPath buildDivider(double startDeg, double r) {
        double rad = Math.toRadians(startDeg - 90);
        double x   = r * Math.cos(rad);
        double y   = r * Math.sin(rad);
        String d   = String.format(Locale.US, "M 0,0 L %.4f,%.4f", x, y);
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
     *
     * <p>The label's fill is white when the slice is dark, and a dark shade when
     * the slice is light, for automatic contrast.
     */
    private Text buildLabel(String label, double startDeg, double sliceDeg,
                            double r, double fontSize, Color sliceFill) {

        double midAngleDeg = startDeg + sliceDeg / 2.0;
        double midAngleRad = Math.toRadians(midAngleDeg - 90);

        double lx = r * LABEL_RADIAL_FRACTION * Math.cos(midAngleRad);
        double ly = r * LABEL_RADIAL_FRACTION * Math.sin(midAngleRad);

        Text text = new Text(label);
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));
        text.setFill(contrastColor(sliceFill));

        // Centre the text on its anchor point
        text.setX(lx - text.getLayoutBounds().getWidth()  / 2.0);
        text.setY(ly + text.getLayoutBounds().getHeight() / 4.0);

        // Rotate so the text reads outward along the radius from centre to rim
        Rotate rotate = new Rotate(midAngleDeg - 90, lx, ly);
        text.getTransforms().add(rotate);

        return text;
    }

    /** Label for the single-participant fallback wheel. */
    private Text buildSingleParticipantLabel(String name) {
        Text text = new Text(truncate(name));
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18.0));
        text.setFill(contrastColor(PALETTE[0]));
        text.setX(-text.getLayoutBounds().getWidth()  / 2.0);
        text.setY(text.getLayoutBounds().getHeight() / 4.0);
        return text;
    }

    /**
     * Polished hub capping the center of the wheel where slice points converge.
     */
    private Group buildCenterHub(double r) {
        Group hub = new Group();
        double hubR = Math.max(22.0, r * 0.12);

        Circle outer = new Circle(0, 0, hubR);
        outer.setFill(Color.web("#0f172a"));
        outer.setStroke(Color.web("#334155"));
        outer.setStrokeWidth(2.5);

        Circle inner = new Circle(0, 0, hubR * 0.45);
        inner.setFill(Color.web("#6366f1"));
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
        double tipY    = -(r - 12);
        double baseY   = -(r + 16);
        double halfW   = 12.0;

        String d = String.format(Locale.US,
                "M 0,%.4f L %.4f,%.4f L %.4f,%.4f Z",
                tipY, -halfW, baseY, halfW, baseY);

        SVGPath pointer = new SVGPath();
        pointer.setContent(d);
        pointer.setFill(Color.web("#f43f5e"));        // rose-500
        pointer.setStroke(Color.web("#0b1120"));
        pointer.setStrokeWidth(2);
        pointer.setId("wheel-pointer");
        return pointer;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Returns a font size that stays readable as the number of slices grows. */
    private double adaptiveFontSize(int n) {
        if (n <= 8)  return 13.0;
        if (n <= 15) return 11.0;
        if (n <= 25) return 9.5;
        return 8.0;
    }

    /** Truncates a name to {@link #MAX_LABEL_CHARS} with an ellipsis. */
    private String truncate(String name) {
        if (name.length() <= MAX_LABEL_CHARS) return name;
        return name.substring(0, MAX_LABEL_CHARS - 1) + "…";
    }

    /**
     * Returns white or a dark colour depending on the perceived luminance of
     * {@code bg}, so the label is always readable.
     */
    private Color contrastColor(Color bg) {
        // Perceived luminance formula (WCAG)
        double lum = 0.2126 * linearise(bg.getRed())
                   + 0.7152 * linearise(bg.getGreen())
                   + 0.0722 * linearise(bg.getBlue());
        return lum > 0.35 ? Color.web("#111827") : Color.WHITE;
    }

    private double linearise(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
