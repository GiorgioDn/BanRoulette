package com.example.anim;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * Drives the roulette-wheel spin animation.
 *
 * <p>The winner index is selected <em>before</em> the animation starts;
 * the final rotation angle is computed so that the winning slice ends up
 * under the pointer at 12 o'clock (0°). The animation never decides the
 * winner — it only visualises a pre-determined result.
 *
 * <h2>Angle Maths</h2>
 * <ul>
 *   <li>The pointer is fixed at 12 o'clock (0°).</li>
 *   <li>Slice {@code i} occupies clockwise angles
 *       {@code [i * (360/n), (i+1) * (360/n))}.</li>
 *   <li>Its center is {@code sliceCenter = (i + 0.5) * (360/n)}.</li>
 *   <li>Rotating the wheel clockwise by {@code (360 - sliceCenter)} brings the
 *       center of slice {@code i} directly under the 12 o'clock pointer.</li>
 *   <li>A subtle pseudo-random offset within ±40% of the slice angle keeps
 *       the stop from always landing dead-center while staying safely inside
 *       the slice boundaries.</li>
 *   <li>After the spin completes, the wheel rotation is normalized
 *       ({@code angle % 360}) to prevent unbounded angle accumulation.</li>
 * </ul>
 */
public class SpinAnimation {

    public static final int    DEFAULT_FULL_SPINS    = 6;
    public static final double DEFAULT_DURATION_SECS = 5.0;
    public static final double MAX_OFFSET_FRACTION   = 0.40;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ── Mathematical calculations (pure logic, UI-independent) ───────────────

    /**
     * Computes the angular center of slice {@code index} in degrees (0°..360°),
     * measured clockwise from 12 o'clock.
     */
    public static double computeSliceCenter(int index, int totalParticipants) {
        if (totalParticipants < 2) {
            throw new IllegalArgumentException("Total participants must be at least 2");
        }
        if (index < 0 || index >= totalParticipants) {
            throw new IndexOutOfBoundsException("Winner index " + index + " out of bounds for " + totalParticipants);
        }
        double sliceAngle = 360.0 / totalParticipants;
        return (index + 0.5) * sliceAngle;
    }

    /**
     * Computes a random offset within ±{@value #MAX_OFFSET_FRACTION} of the slice angle.
     *
     * @param totalParticipants total number of participants
     * @param offsetFraction    normalized factor in the range [-1.0, 1.0]
     * @return angular offset in degrees
     */
    public static double computeRandomOffset(int totalParticipants, double offsetFraction) {
        if (totalParticipants < 2) {
            throw new IllegalArgumentException("Total participants must be at least 2");
        }
        double clamped = Math.max(-1.0, Math.min(1.0, offsetFraction));
        double sliceAngle = 360.0 / totalParticipants;
        return clamped * MAX_OFFSET_FRACTION * sliceAngle;
    }

    /**
     * Normalizes any angle in degrees into the range [0.0, 360.0).
     */
    public static double normalizeAngle(double angle) {
        double mod = angle % 360.0;
        return (mod < 0) ? mod + 360.0 : mod;
    }

    /**
     * Computes the absolute target angle for a spin starting from {@code currentAngle}.
     *
     * <p>The wheel always rotates forward (clockwise) by at least {@code fullSpins}
     * full 360° revolutions, stopping at the orientation where slice {@code index}
     * is under the 12 o'clock pointer with the given {@code offset}.
     *
     * @param currentAngle      the wheel's current rotation in degrees
     * @param index             the pre-selected winner slice index [0, totalParticipants)
     * @param totalParticipants number of slices (>= 2)
     * @param fullSpins         number of full revolutions (typically 5 to 7)
     * @param offset            offset within slice in degrees (from {@link #computeRandomOffset})
     * @return the target rotation angle in degrees for the transition
     */
    public static double computeTargetAngle(double currentAngle, int index, int totalParticipants,
                                           int fullSpins, double offset) {
        double sliceCenter = computeSliceCenter(index, totalParticipants);
        // Target orientation in [0, 360): (360 - sliceCenter + offset) mod 360
        double targetOrientation = normalizeAngle(360.0 - sliceCenter + offset);
        double currentNorm = normalizeAngle(currentAngle);

        double forwardDelta = targetOrientation - currentNorm;
        if (forwardDelta < 0) {
            forwardDelta += 360.0;
        }

        return currentAngle + (fullSpins * 360.0) + forwardDelta;
    }

    /**
     * Determines which slice index is currently under the 12 o'clock pointer
     * given the wheel's rotation angle.
     *
     * <p>At rotation 0°, slice 0 is centered at 0.5 * (360/n) and spans [0, 360/n).
     * When rotated clockwise by {@code rotationDeg}, the point on the wheel under
     * the pointer (at 0°) is at angle {@code (360 - (rotationDeg % 360)) mod 360}.
     *
     * @param rotationDeg       clockwise wheel rotation in degrees
     * @param totalParticipants total number of participants / slices
     * @return the slice index [0, totalParticipants) currently selected
     */
    public static int getSliceIndexAtPointer(double rotationDeg, int totalParticipants) {
        if (totalParticipants < 2) {
            throw new IllegalArgumentException("Total participants must be at least 2");
        }
        double sliceAngle = 360.0 / totalParticipants;
        double wheelAngleAtPointer = normalizeAngle(360.0 - (rotationDeg % 360.0));
        int index = (int) Math.floor(wheelAngleAtPointer / sliceAngle);
        return index % totalParticipants;
    }

    /**
     * Selects a random winner index fairly using {@link SecureRandom}.
     */
    public static int pickRandomWinner(int totalParticipants) {
        return pickRandomWinner(totalParticipants, SECURE_RANDOM);
    }

    /**
     * Selects a random winner index fairly using the provided {@link RandomGenerator}.
     */
    public static int pickRandomWinner(int totalParticipants, RandomGenerator random) {
        if (totalParticipants < 2) {
            throw new IllegalArgumentException("Need at least 2 participants to pick a winner");
        }
        return random.nextInt(totalParticipants);
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
