package com.example.svg;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads and parses the ban-hammer SVG ({@code /svg/hammer.svg}) into
 * JavaFX shape nodes.
 *
 * <p>The hammer art is intentionally flat (rectangles, circles, lines, and paths)
 * so it converts reliably into standard JavaFX shapes.
 *
 * <p>The pivot point is set to the handle end {@code (60, 200)} by offsetting
 * the inner shape group, allowing rotational transitions to naturally swing
 * the hammer around its grip.
 */
public class HammerLoader {

    private static final Logger LOG = Logger.getLogger(HammerLoader.class.getName());

    public static final double PIVOT_X = 60.0;
    public static final double PIVOT_Y = 200.0;

    /**
     * Reads {@code /svg/hammer.svg} from the classpath and converts its
     * elements into JavaFX shapes wrapped in a pivot-aligned {@link Group}.
     *
     * @return a {@link Group} representing the hammer, ready for animation
     */
    public Group loadHammer() {
        Group inner = new Group();

        try (InputStream is = getClass().getResourceAsStream("/svg/hammer.svg")) {
            if (is != null) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                // Disable external entities for safety
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                DocumentBuilder builder = dbf.newDocumentBuilder();
                Document doc = builder.parse(is);

                Element root = doc.getDocumentElement();
                NodeList children = root.getChildNodes();

                for (int i = 0; i < children.getLength(); i++) {
                    org.w3c.dom.Node node = children.item(i);
                    if (node instanceof Element el) {
                        Node fxNode = parseSvgElement(el);
                        if (fxNode != null) {
                            inner.getChildren().add(fxNode);
                        }
                    }
                }
            } else {
                LOG.warning("Could not find /svg/hammer.svg on classpath; using programmatic fallback");
                buildFallbackHammer(inner);
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to parse /svg/hammer.svg; using programmatic fallback", ex);
            inner.getChildren().clear();
            buildFallbackHammer(inner);
        }

        if (inner.getChildren().isEmpty()) {
            buildFallbackHammer(inner);
        }

        // Offset the inner group by (-PIVOT_X, -PIVOT_Y) so the handle's bottom tip is at (0, 0)
        inner.setTranslateX(-PIVOT_X);
        inner.setTranslateY(-PIVOT_Y);

        Group root = new Group(inner);
        root.setId("ban-hammer");
        return root;
    }

    private Node parseSvgElement(Element el) {
        String tag = el.getTagName().toLowerCase();
        return switch (tag) {
            case "rect" -> parseRect(el);
            case "circle" -> parseCircle(el);
            case "line" -> parseLine(el);
            case "path" -> parsePath(el);
            default -> null;
        };
    }

    private Rectangle parseRect(Element el) {
        double x = parseDouble(el.getAttribute("x"), 0);
        double y = parseDouble(el.getAttribute("y"), 0);
        double w = parseDouble(el.getAttribute("width"), 0);
        double h = parseDouble(el.getAttribute("height"), 0);
        double rx = parseDouble(el.getAttribute("rx"), 0);
        double ry = parseDouble(el.getAttribute("ry"), 0);

        Rectangle rect = new Rectangle(x, y, w, h);
        if (rx > 0) rect.setArcWidth(rx * 2);
        if (ry > 0) rect.setArcHeight(ry * 2);

        String fill = el.getAttribute("fill");
        if (!fill.isEmpty() && !"none".equalsIgnoreCase(fill)) {
            rect.setFill(Color.web(fill));
        } else {
            rect.setFill(Color.TRANSPARENT);
        }

        applyStroke(el, rect);
        return rect;
    }

    private Circle parseCircle(Element el) {
        double cx = parseDouble(el.getAttribute("cx"), 0);
        double cy = parseDouble(el.getAttribute("cy"), 0);
        double r = parseDouble(el.getAttribute("r"), 0);

        Circle circle = new Circle(cx, cy, r);
        String fill = el.getAttribute("fill");
        if (!fill.isEmpty() && !"none".equalsIgnoreCase(fill)) {
            circle.setFill(Color.web(fill));
        } else {
            circle.setFill(Color.TRANSPARENT);
        }

        applyStroke(el, circle);
        return circle;
    }

    private Line parseLine(Element el) {
        double x1 = parseDouble(el.getAttribute("x1"), 0);
        double y1 = parseDouble(el.getAttribute("y1"), 0);
        double x2 = parseDouble(el.getAttribute("x2"), 0);
        double y2 = parseDouble(el.getAttribute("y2"), 0);

        Line line = new Line(x1, y1, x2, y2);
        applyStroke(el, line);

        String linecap = el.getAttribute("stroke-linecap");
        if ("round".equalsIgnoreCase(linecap)) {
            line.setStrokeLineCap(StrokeLineCap.ROUND);
        }
        return line;
    }

    private SVGPath parsePath(Element el) {
        String d = el.getAttribute("d");
        if (d.isEmpty()) return null;

        SVGPath path = new SVGPath();
        path.setContent(d);

        String fill = el.getAttribute("fill");
        if (!fill.isEmpty() && !"none".equalsIgnoreCase(fill)) {
            path.setFill(Color.web(fill));
        } else {
            path.setFill(Color.TRANSPARENT);
        }

        applyStroke(el, path);
        return path;
    }

    private void applyStroke(Element el, javafx.scene.shape.Shape shape) {
        String stroke = el.getAttribute("stroke");
        if (!stroke.isEmpty() && !"none".equalsIgnoreCase(stroke)) {
            shape.setStroke(Color.web(stroke));
            double strokeWidth = parseDouble(el.getAttribute("stroke-width"), 1.0);
            shape.setStrokeWidth(strokeWidth);
        }
    }

    private double parseDouble(String str, double def) {
        if (str == null || str.isBlank()) return def;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Programmatic fallback replicating the geometry of hammer.svg.
     */
    private void buildFallbackHammer(Group inner) {
        // Handle
        Rectangle handle = new Rectangle(50, 90, 20, 105);
        handle.setArcWidth(10);
        handle.setArcHeight(10);
        handle.setFill(Color.web("#6B3A1F"));

        Rectangle grip1 = new Rectangle(50, 140, 20, 16);
        grip1.setArcWidth(6);
        grip1.setArcHeight(6);
        grip1.setFill(Color.web("#4A2810"));

        Rectangle grip2 = new Rectangle(50, 162, 20, 10);
        grip2.setArcWidth(6);
        grip2.setArcHeight(6);
        grip2.setFill(Color.web("#4A2810"));

        // Hammer head
        Rectangle head = new Rectangle(10, 10, 100, 86);
        head.setArcWidth(20);
        head.setArcHeight(20);
        head.setFill(Color.web("#3B3B3B"));

        Rectangle highlight = new Rectangle(10, 10, 100, 18);
        highlight.setArcWidth(20);
        highlight.setArcHeight(20);
        highlight.setFill(Color.web("#555555"));

        Rectangle strikeFace = new Rectangle(14, 78, 92, 16);
        strikeFace.setArcWidth(8);
        strikeFace.setArcHeight(8);
        strikeFace.setFill(Color.web("#222222"));

        // Ban emblem
        Circle emblem = new Circle(60, 52, 22);
        emblem.setFill(Color.TRANSPARENT);
        emblem.setStroke(Color.web("#EF4444"));
        emblem.setStrokeWidth(5);

        Line slash = new Line(43, 35, 77, 69);
        slash.setStroke(Color.web("#EF4444"));
        slash.setStrokeWidth(5);
        slash.setStrokeLineCap(StrokeLineCap.ROUND);

        inner.getChildren().addAll(handle, grip1, grip2, head, highlight, strikeFace, emblem, slash);
    }
}
