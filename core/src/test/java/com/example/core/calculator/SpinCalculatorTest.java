package com.example.core.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpinCalculator}.
 *
 * <p>Validates:
 * <ul>
 *   <li><b>Acceptance criterion (Step 3 / Step 5):</b> over 1,000 simulated spins,
 *       the slice under the pointer always equals the pre-selected winner index.</li>
 *   <li>Boundary behavior at exact ±40% slice offset limits.</li>
 *   <li>Forward rotation guarantees across multiple spins.</li>
 *   <li>Angle normalization into [0.0, 360.0).</li>
 * </ul>
 */
class SpinCalculatorTest {

    @Test
    @DisplayName("Over 2,000 simulated spins, the slice under the pointer always equals the pre-selected index")
    void simulatedSpinsAlwaysEqualPreselectedIndex() {
        Random random = new Random(42); // deterministic seed for reproducibility
        final int iterations = 2000;

        for (int i = 0; i < iterations; i++) {
            // Random participant count between 2 and 40
            int n = 2 + random.nextInt(39);

            // Pre-selected winner index
            int preselectedIndex = random.nextInt(n);

            // Starting angle
            double currentAngle = (random.nextDouble() * 2880.0) - 1440.0;

            // Full revolutions
            int fullSpins = 4 + random.nextInt(6); // 4 to 9

            // Random offset factor within [-1.0, 1.0] (±40% of slice angle)
            double offsetFactor = (random.nextDouble() * 2.0) - 1.0;
            double offset = SpinCalculator.computeRandomOffset(n, offsetFactor);

            // Compute target angle
            double targetAngle = SpinCalculator.computeTargetAngle(currentAngle, preselectedIndex, n, fullSpins, offset);

            // 1. Verify winner slice under pointer with targetAngle
            int sliceAtPointer = SpinCalculator.getSliceIndexAtPointer(targetAngle, n);
            assertEquals(preselectedIndex, sliceAtPointer,
                    String.format("Iteration %d failed: expected winner %d but pointer landed on %d (n=%d, spins=%d, offset=%.2f, target=%.2f)",
                            i, preselectedIndex, sliceAtPointer, n, fullSpins, offset, targetAngle));

            // 2. Verify winner slice under pointer after angle normalization
            double normalized = SpinCalculator.normalizeAngle(targetAngle);
            assertTrue(normalized >= 0.0 && normalized < 360.0,
                    "Normalized angle must be in [0, 360): " + normalized);

            int sliceAtNormalizedPointer = SpinCalculator.getSliceIndexAtPointer(normalized, n);
            assertEquals(preselectedIndex, sliceAtNormalizedPointer,
                    String.format("Iteration %d normalized failed: expected %d but got %d",
                            i, preselectedIndex, sliceAtNormalizedPointer));

            // 3. Verify the transition always spins forward by at least fullSpins * 360
            double forwardDelta = targetAngle - currentAngle;
            assertTrue(forwardDelta >= fullSpins * 360.0 - 1e-9,
                    String.format("Target angle must rotate forward by at least %d spins (delta=%.2f)",
                            fullSpins, forwardDelta));
        }
    }

    @ParameterizedTest(name = "Every slice for {0} participants lands on target across boundary offsets")
    @ValueSource(ints = {2, 3, 7, 20, 40})
    void everySliceForParticipantCountLandsUnderPointer(int n) {
        double[] testOffsetFractions = {-1.0, -0.5, 0.0, 0.5, 1.0};

        for (int winner = 0; winner < n; winner++) {
            for (double offsetFactor : testOffsetFractions) {
                double offset = SpinCalculator.computeRandomOffset(n, offsetFactor);
                double target = SpinCalculator.computeTargetAngle(0.0, winner, n, 6, offset);

                int landedIndex = SpinCalculator.getSliceIndexAtPointer(target, n);
                assertEquals(winner, landedIndex,
                        String.format("Slice %d of %d failed with offsetFraction=%.2f (target=%.2f, landed=%d)",
                                winner, n, offsetFactor, target, landedIndex));
            }
        }
    }

    @Test
    @DisplayName("computeSliceCenter throws on invalid arguments")
    void sliceCenterValidation() {
        assertThrows(IllegalArgumentException.class, () -> SpinCalculator.computeSliceCenter(0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> SpinCalculator.computeSliceCenter(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> SpinCalculator.computeSliceCenter(5, 5));
    }

    @Test
    @DisplayName("normalizeAngle handles negative and high values")
    void angleNormalization() {
        assertEquals(0.0, SpinCalculator.normalizeAngle(0.0), 1e-9);
        assertEquals(0.0, SpinCalculator.normalizeAngle(360.0), 1e-9);
        assertEquals(0.0, SpinCalculator.normalizeAngle(720.0), 1e-9);
        assertEquals(90.0, SpinCalculator.normalizeAngle(450.0), 1e-9);
        assertEquals(270.0, SpinCalculator.normalizeAngle(-90.0), 1e-9);
        assertEquals(0.0, SpinCalculator.normalizeAngle(-360.0), 1e-9);
    }
}
