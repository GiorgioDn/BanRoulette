package com.example.anim;

import com.example.core.calculator.SpinCalculator;
import com.example.core.selector.WinnerSelector;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * Drives the roulette-wheel spin animation.
 *
 * <p>Mathematical angle calculations and winner selection are delegated to
 * {@link SpinCalculator} and {@link WinnerSelector} in {@code :core}.
 */
public class SpinAnimation {

    public static final int    DEFAULT_FULL_SPINS    = SpinCalculator.DEFAULT_FULL_SPINS;
    public static final double DEFAULT_DURATION_SECS = SpinCalculator.DEFAULT_DURATION_SECS;
    public static final double MAX_OFFSET_FRACTION   = SpinCalculator.MAX_OFFSET_FRACTION;

    private static final WinnerSelector WINNER_SELECTOR = new WinnerSelector();

    // ── Mathematical calculations (delegated to :core SpinCalculator) ──────────

    public static double computeSliceCenter(int index, int totalParticipants) {
        return SpinCalculator.computeSliceCenter(index, totalParticipants);
    }

    public static double computeRandomOffset(int totalParticipants, double offsetFraction) {
        return SpinCalculator.computeRandomOffset(totalParticipants, offsetFraction);
    }

    public static double normalizeAngle(double angle) {
        return SpinCalculator.normalizeAngle(angle);
    }

    public static double computeTargetAngle(double currentAngle, int index, int totalParticipants,
                                            int fullSpins, double offset) {
        return SpinCalculator.computeTargetAngle(currentAngle, index, totalParticipants, fullSpins, offset);
    }

    public static int getSliceIndexAtPointer(double rotationDeg, int totalParticipants) {
        return SpinCalculator.getSliceIndexAtPointer(rotationDeg, totalParticipants);
    }

    public static int pickRandomWinner(int totalParticipants) {
        return WINNER_SELECTOR.selectWinnerIndex(totalParticipants);
    }

    public static int pickRandomWinner(int totalParticipants, RandomGenerator random) {
        return new WinnerSelector(random).selectWinnerIndex(totalParticipants);
    }

    // ── Animation builders ─────────────────────────────────────────────────────

    /**
     * Builds and returns a configured {@link RotateTransition} that smoothly spins
     * {@code wheelNode} from its current rotation to {@code targetAngle}.
     *
     * <p>On completion:
     * <ol>
     *   <li>Normalizes the wheel node's rotation to {@code [0, 360)} so repeated
     *       spins do not accumulate huge degree values.</li>
     *   <li>Invokes {@code onFinished}.</li>
     * </ol>
     *
     * @param wheelNode   the JavaFX node representing the rotating wheel group
     * @param targetAngle the computed target angle in degrees
     * @param onFinished  callback executed upon animation completion
     * @return the configured, ready-to-play {@link RotateTransition}
     */
    public RotateTransition buildSpin(Node wheelNode, double targetAngle, Runnable onFinished) {
        return buildSpin(wheelNode, targetAngle, Duration.seconds(DEFAULT_DURATION_SECS), onFinished);
    }

    /**
     * Full configuration overload for {@link #buildSpin(Node, double, Runnable)}.
     */
    public RotateTransition buildSpin(Node wheelNode, double targetAngle, Duration duration, Runnable onFinished) {
        RotateTransition rt = new RotateTransition(duration, wheelNode);
        rt.setFromAngle(wheelNode.getRotate());
        rt.setToAngle(targetAngle);
        rt.setInterpolator(Interpolator.EASE_OUT);
        rt.setOnFinished(e -> {
            wheelNode.setRotate(normalizeAngle(targetAngle));
            if (onFinished != null) {
                onFinished.run();
            }
        });
        return rt;
    }

    /**
     * Builds a subtle, snappy pointer wobble transition for visual polish during a spin.
     */
    public static RotateTransition buildPointerWobble(Node pointerNode) {
        RotateTransition rt = new RotateTransition(Duration.millis(90), pointerNode);
        rt.setByAngle(7.0);
        rt.setAutoReverse(true);
        rt.setCycleCount(RotateTransition.INDEFINITE);
        return rt;
    }
}
