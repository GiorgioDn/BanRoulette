package com.example.core.calculator;

/**
 * Pure mathematical calculations for the roulette wheel spin physics.
 *
 * <p>Contains zero UI or platform framework dependencies so that desktop (JavaFX)
 * and mobile (Android Jetpack Compose) share identical geometry, stopping angles,
 * and slice determinations.
 */
public class SpinCalculator {

    public static final int    DEFAULT_FULL_SPINS    = 6;
    public static final double DEFAULT_DURATION_SECS = 5.0;
    public static final double MAX_OFFSET_FRACTION   = 0.40;

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
}
