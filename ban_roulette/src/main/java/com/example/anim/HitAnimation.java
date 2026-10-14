package com.example.anim;

import javafx.animation.SequentialTransition;
import javafx.scene.Node;

/**
 * Drives the ban-hammer hit animation sequence.
 *
 * <p>The full sequence (Step 6) is:
 * <ol>
 *   <li><b>Wind-up</b> — rotate hammer back ~30° (200 ms, EASE_OUT)</li>
 *   <li><b>Strike</b>  — rotate forward quickly to the target slice (120 ms, EASE_IN)</li>
 *   <li><b>Impact</b>  — short shake on the wheel (TranslateTransition ±5 px)</li>
 *   <li><b>Retreat</b> — hammer fades out or returns to rest</li>
 * </ol>
 *
 * <p><b>Implemented in Step 6.</b>
 */
public class HitAnimation {

    /**
     * Builds a {@link SequentialTransition} that plays the full wind-up →
     * strike → impact → retreat sequence.
     *
     * @param hammerNode the hammer Group to animate
     * @param wheelNode  the wheel Group (used for the impact shake)
     * @param onFinished callback invoked after the full sequence completes;
     *                   used to show the result dialog (Step 6)
     * @return the ready-to-play sequential transition
     */
    public SequentialTransition buildHit(Node hammerNode, Node wheelNode, Runnable onFinished) {
        // TODO: implement in Step 6
        SequentialTransition st = new SequentialTransition(hammerNode);
        st.setOnFinished(e -> onFinished.run());
        return st;
    }
}
