package com.example.anim;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Drives the roulette-wheel spin animation.
 *
 * <p>The winner index is selected <em>before</em> the animation starts;
 * the final rotation angle is computed so that the winning slice ends up
 * under the pointer. The animation never decides the winner — it only
 * visualises a pre-determined result.
 *
 * <p><b>Implemented in Step 5.</b>
 */
public class SpinAnimation {

    private static final int    FULL_SPINS    = 6;
    private static final double DURATION_SECS = 6.0;

    /**
     * Builds and returns a configured {@link RotateTransition} that spins
     * {@code wheelNode} to {@code targetAngle}.
     *
     * @param wheelNode   the JavaFX node representing the wheel group
     * @param targetAngle pre-computed final angle in degrees
     *                    ({@code fullSpins * 360 + (360 − sliceCenter) + randomOffset})
     * @param onFinished  callback invoked when the spin completes; used to
     *                    trigger the hit animation (Step 6)
     * @return the ready-to-play transition
     */
    public RotateTransition buildSpin(Node wheelNode, double targetAngle, Runnable onFinished) {
        // TODO: implement fully in Step 5
        //  - use Interpolator.EASE_OUT (fast start, slow stop)
        //  - normalise angle after each spin with (angle % 360)
        //  - disable UI inputs for the duration
        RotateTransition rt = new RotateTransition(Duration.seconds(DURATION_SECS), wheelNode);
        rt.setByAngle(targetAngle);
        rt.setInterpolator(Interpolator.EASE_OUT);
        rt.setOnFinished(e -> onFinished.run());
        return rt;
    }
}
