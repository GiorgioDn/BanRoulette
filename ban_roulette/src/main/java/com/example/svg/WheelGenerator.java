package com.example.svg;

import com.example.model.Participant;
import javafx.scene.Group;

import java.util.List;

/**
 * Generates the SVG-based roulette wheel from a list of participants.
 *
 * <p>Each slice is one {@code SVGPath} arc, coloured from a curated palette,
 * with a rotated {@code Text} label along the mid-angle of the slice.
 * All slices are collected into one {@link Group} so the entire wheel
 * rotates as a single node.
 *
 * <p><b>Implemented in Step 4.</b>
 */
public class WheelGenerator {

    /** Default wheel radius in pixels. */
    public static final double DEFAULT_RADIUS = 280.0;

    /**
     * Builds and returns a {@link Group} containing one SVGPath slice per
     * participant.
     *
     * @param participants the names to place on the wheel (must have ≥ 2 entries)
     * @param radius       the wheel radius in pixels
     * @return a Group ready to be added to the scene graph
     */
    public Group generateWheel(List<? extends Participant> participants, double radius) {
        // TODO: implement in Step 4
        //  - slice angle  = 360.0 / n
        //  - path data    = "M cx,cy L x1,y1 A r,r 0 largeArc,1 x2,y2 Z"
        //  - fill colours = cycle through PALETTE
        //  - labels       = Text node rotated to slice mid-angle
        return new Group();
    }
}
