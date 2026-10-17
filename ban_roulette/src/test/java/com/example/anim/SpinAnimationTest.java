package com.example.anim;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SpinAnimation}.
 *
 * <p>Validates:
 * <ul>
 *   <li><b>Angle maths acceptance criterion (Step 5):</b> over 1,000 simulated spins,
 *       the slice under the pointer always equals the pre-selected winner index.</li>
 *   <li>Boundary behavior at exact ±40% slice offset limits.</li>
 *   <li>Forward rotation guarantees across multiple spins.</li>
 *   <li>Angle normalization (preventing unbounded accumulation).</li>
 *   <li>Pre-selected winner generator validity and bounds.</li>
 *   <li>JavaFX transition instantiation and callbacks.</li>
 * </ul>
 */
class SpinAnimationTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already started — fine.
        }
    }

    // ── Acceptance Criterion: 1,000+ simulated spins ──────────────────────────

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

            // Starting angle (could be positive, negative, or multiple rotations)
            double currentAngle = (random.nextDouble() * 2880.0) - 1440.0;

            // Full rotations
            int fullSpins = 4 + random.nextInt(6); // 4 to 9

            // Random offset factor within [-1.0, 1.0] (±40% of slice angle)
            double offsetFactor = (random.nextDouble() * 2.0) - 1.0;
            double offset = SpinAnimation.computeRandomOffset(n, offsetFactor);

            // Compute target angle
            double targetAngle = SpinAnimation.computeTargetAngle(currentAngle, preselectedIndex, n, fullSpins, offset);

            // 1. Verify winner slice under pointer with targetAngle
            int sliceAtPointer = SpinAnimation.getSliceIndexAtPointer(targetAngle, n);
            assertEquals(preselectedIndex, sliceAtPointer,
                    String.format("Iteration %d failed: expected winner %d but pointer landed on %d (n=%d, spins=%d, offset=%.2f, target=%.2f)",
                            i, preselectedIndex, sliceAtPointer, n, fullSpins, offset, targetAngle));

            // 2. Verify winner slice under pointer after angle normalization
            double normalized = SpinAnimation.normalizeAngle(targetAngle);
            assertTrue(normalized >= 0.0 && normalized < 360.0,
                    "Normalized angle must be in [0, 360): " + normalized);

            int sliceAtNormalizedPointer = SpinAnimation.getSliceIndexAtPointer(normalized, n);
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

    // ── Targeted tests for key participant counts: 2, 3, 7, 20 ────────────────

    @ParameterizedTest(name = "Every slice for {0} participants lands on target across boundary offsets")
    @ValueSource(ints = {2, 3, 7, 20})
    void everySliceForParticipantCountLandsUnderPointer(int n) {
        double[] testOffsetFractions = {-1.0, -0.75, -0.4, 0.0, 0.4, 0.75, 1.0};

        for (int slice = 0; slice < n; slice++) {
            for (double factor : testOffsetFractions) {
                double offset = SpinAnimation.computeRandomOffset(n, factor);
                double targetAngle = SpinAnimation.computeTargetAngle(0.0, slice, n, 6, offset);

                int result = SpinAnimation.getSliceIndexAtPointer(targetAngle, n);
                assertEquals(slice, result,
                        String.format("Slice %d/%d with offset factor %.2f landed on %d", slice, n, factor, result));

                int normalizedResult = SpinAnimation.getSliceIndexAtPointer(
                        SpinAnimation.normalizeAngle(targetAngle), n);
                assertEquals(slice, normalizedResult,
                        String.format("Slice %d/%d normalized with offset factor %.2f landed on %d", slice, n, factor, normalizedResult));
            }
        }
    }

    // ── Boundary offset limits ─────────────────────────────────────────────────

    @Test
    void boundaryOffsetsStayWithinWinnerSlice() {
        int n = 8;
        int winner = 0; // slice 0 is especially sensitive near 0° / 360° boundary

        // Exactly at max negative limit (-40% of slice)
        double minOffset = SpinAnimation.computeRandomOffset(n, -1.0);
        double angleMin = SpinAnimation.computeTargetAngle(0.0, winner, n, 5, minOffset);
        assertEquals(winner, SpinAnimation.getSliceIndexAtPointer(angleMin, n));

        // Exactly at max positive limit (+40% of slice)
        double maxOffset = SpinAnimation.computeRandomOffset(n, 1.0);
        double angleMax = SpinAnimation.computeTargetAngle(0.0, winner, n, 5, maxOffset);
        assertEquals(winner, SpinAnimation.getSliceIndexAtPointer(angleMax, n));

        // Also test the last slice (n - 1) near the 360° wrap
        int lastSlice = n - 1;
        double angleLastMin = SpinAnimation.computeTargetAngle(0.0, lastSlice, n, 5, minOffset);
        assertEquals(lastSlice, SpinAnimation.getSliceIndexAtPointer(angleLastMin, n));

        double angleLastMax = SpinAnimation.computeTargetAngle(0.0, lastSlice, n, 5, maxOffset);
        assertEquals(lastSlice, SpinAnimation.getSliceIndexAtPointer(angleLastMax, n));
    }

    @Test
    void offsetClampsValuesExceedingUnitRange() {
        int n = 10;
        double sliceAngle = 360.0 / n;
        double maxAllowed = 0.40 * sliceAngle;

        double offsetOver = SpinAnimation.computeRandomOffset(n, 2.5);
        assertEquals(maxAllowed, offsetOver, 1e-9);

        double offsetUnder = SpinAnimation.computeRandomOffset(n, -3.0);
        assertEquals(-maxAllowed, offsetUnder, 1e-9);
    }

    // ── Normalization helper ───────────────────────────────────────────────────

    @Test
    void normalizeAngleKeepsAnglesInExpectedRange() {
        assertEquals(0.0, SpinAnimation.normalizeAngle(0.0), 1e-9);
        assertEquals(0.0, SpinAnimation.normalizeAngle(360.0), 1e-9);
        assertEquals(0.0, SpinAnimation.normalizeAngle(720.0), 1e-9);
        assertEquals(90.0, SpinAnimation.normalizeAngle(90.0), 1e-9);
        assertEquals(90.0, SpinAnimation.normalizeAngle(450.0), 1e-9);
        assertEquals(270.0, SpinAnimation.normalizeAngle(-90.0), 1e-9);
        assertEquals(350.0, SpinAnimation.normalizeAngle(-10.0), 1e-9);
    }

    // ── Winner selection ───────────────────────────────────────────────────────

    @Test
    void pickRandomWinnerReturnsValidIndices() {
        int n = 12;
        for (int i = 0; i < 500; i++) {
            int winner = SpinAnimation.pickRandomWinner(n);
            assertTrue(winner >= 0 && winner < n,
                    "Winner index " + winner + " must be in [0, " + n + ")");
        }
    }

    // ── Error handling & guards ────────────────────────────────────────────────

    @Test
    void invalidArgumentsThrowExceptions() {
        assertThrows(IllegalArgumentException.class,
                () -> SpinAnimation.computeSliceCenter(0, 1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> SpinAnimation.computeSliceCenter(-1, 5));
        assertThrows(IndexOutOfBoundsException.class,
                () -> SpinAnimation.computeSliceCenter(5, 5));

        assertThrows(IllegalArgumentException.class,
                () -> SpinAnimation.computeRandomOffset(1, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> SpinAnimation.getSliceIndexAtPointer(180.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> SpinAnimation.pickRandomWinner(1));
    }

    // ── JavaFX RotateTransition Builder ────────────────────────────────────────

    @Test
    void buildSpinCreatesValidTransitionWithEaseOut() {
        Group wheel = new Group();
        wheel.setRotate(45.0);
        SpinAnimation anim = new SpinAnimation();

        AtomicBoolean finishedCalled = new AtomicBoolean(false);
        double target = 2205.0;

        RotateTransition rt = anim.buildSpin(wheel, target, Duration.millis(50), () -> finishedCalled.set(true));

        assertNotNull(rt);
        assertEquals(wheel, rt.getNode());
        assertEquals(Interpolator.EASE_OUT, rt.getInterpolator());
        assertEquals(45.0, rt.getFromAngle(), 1e-9);
        assertEquals(target, rt.getToAngle(), 1e-9);

        // Simulate finish action
        rt.getOnFinished().handle(null);
        assertTrue(finishedCalled.get(), "onFinished callback should be executed");
        assertEquals(SpinAnimation.normalizeAngle(target), wheel.getRotate(), 1e-9,
                "Wheel rotation must be normalized on completion");
    }

    @Test
    void buildPointerWobbleCreatesValidTransition() {
        Group pointer = new Group();
        RotateTransition wobble = SpinAnimation.buildPointerWobble(pointer);

        assertNotNull(wobble);
        assertEquals(pointer, wobble.getNode());
        assertEquals(RotateTransition.INDEFINITE, wobble.getCycleCount());
        assertTrue(wobble.isAutoReverse());
    }
}
