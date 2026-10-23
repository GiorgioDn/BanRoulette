package com.example.anim;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Drives the ban-hammer hit animation sequence.
 *
 * <p>The full sequence (Step 6) is:
 * <ol>
 *   <li><b>Wind-up</b> — rotate hammer back ~30° (200 ms, EASE_OUT)</li>
 *   <li><b>Strike</b>  — rotate forward quickly to the target slice (120 ms, EASE_IN)</li>
 *   <li><b>Impact</b>  — short shake on the wheel (TranslateTransition ±5 px)</li>
 *   <li><b>Retreat</b> — hammer fades out while returning to rest</li>
 * </ol>
 */
public class HitAnimation {

    public static final double WIND_UP_ANGLE = -35.0;
    public static final double STRIKE_ANGLE  =  42.0;
    public static final double SHAKE_OFFSET  =   5.0;

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
        // Ensure hammer starts visible at rest angle
        hammerNode.setOpacity(1.0);
        hammerNode.setRotate(0.0);

        // 1. Wind-up: rotate back ~30° (200 ms, EASE_OUT)
        RotateTransition windUp = new RotateTransition(Duration.millis(200), hammerNode);
        windUp.setFromAngle(0.0);
        windUp.setToAngle(WIND_UP_ANGLE);
        windUp.setInterpolator(Interpolator.EASE_OUT);

        // 2. Strike: rotate forward quickly to the target slice (120 ms, EASE_IN)
        RotateTransition strike = new RotateTransition(Duration.millis(120), hammerNode);
        strike.setFromAngle(WIND_UP_ANGLE);
        strike.setToAngle(STRIKE_ANGLE);
        strike.setInterpolator(Interpolator.EASE_IN);

        // 3. Impact: short shake on the wheel (TranslateTransition ±5 px, 6 cycles)
        TranslateTransition shake = new TranslateTransition(Duration.millis(35), wheelNode);
        shake.setByX(SHAKE_OFFSET);
        shake.setByY(-SHAKE_OFFSET * 0.6);
        shake.setAutoReverse(true);
        shake.setCycleCount(6);

        PauseTransition impactHold = new PauseTransition(Duration.millis(120));
        ParallelTransition impact = new ParallelTransition(shake, impactHold);

        // 4. Retreat: hammer fades out and returns to rest (250 ms)
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), hammerNode);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        RotateTransition retreatRotate = new RotateTransition(Duration.millis(250), hammerNode);
        retreatRotate.setToAngle(0.0);

        ParallelTransition retreat = new ParallelTransition(fadeOut, retreatRotate);

        // Assemble full sequential transition
        SequentialTransition seq = new SequentialTransition(windUp, strike, impact, retreat);
        seq.setOnFinished(e -> {
            hammerNode.setOpacity(0.0);
            wheelNode.setTranslateX(0.0);
            wheelNode.setTranslateY(0.0);
            if (onFinished != null) {
                onFinished.run();
            }
        });

        return seq;
    }
}
