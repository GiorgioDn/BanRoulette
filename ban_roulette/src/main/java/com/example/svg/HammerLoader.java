package com.example.svg;

import javafx.scene.Group;

/**
 * Loads and parses the ban-hammer SVG ({@code /svg/hammer.svg}) into
 * JavaFX {@code SVGPath} nodes.
 *
 * <p>Complex SVG features (gradients, filters, text, clip-paths) are not
 * supported by JavaFX. The hammer art is intentionally kept flat so that
 * every {@code <path>} element can be copied 1-to-1 into an
 * {@link javafx.scene.shape.SVGPath}.
 *
 * <p><b>Implemented in Step 6.</b>
 */
public class HammerLoader {

    /**
     * Reads {@code /svg/hammer.svg} from the classpath and converts its
     * {@code <path>} elements into {@link javafx.scene.shape.SVGPath} nodes.
     *
     * @return a {@link Group} representing the hammer, ready for animation
     */
    public Group loadHammer() {
        // TODO: implement in Step 6
        //  - open /svg/hammer.svg as InputStream
        //  - parse <path d="..." fill="..."/> elements (simple regex or tiny SAX)
        //  - wrap in a Group with the pivot set to the handle end
        return new Group();
    }
}
