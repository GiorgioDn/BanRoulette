package com.example.anim;

import com.example.model.Participant;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.Group;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HitAnimation} and the Step 6 spin -> hit -> dialog chain.
 */
class HitAnimationTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    @DisplayName("buildHit creates SequentialTransition with wind-up, strike, impact shake, and retreat")
    void buildHitCreatesSequentialTransitionWithAllPhases() {
        HitAnimation hitAnim = new HitAnimation();
        Group hammer = new Group();
        Group wheel = new Group();

        AtomicBoolean callbackRan = new AtomicBoolean(false);
        SequentialTransition seq = hitAnim.buildHit(hammer, wheel, () -> callbackRan.set(true));

        assertNotNull(seq);
        assertEquals(4, seq.getChildren().size(), "SequentialTransition must contain 4 distinct phases");

        // Phase 1: Wind-up
        assertTrue(seq.getChildren().get(0) instanceof RotateTransition, "Phase 1 must be wind-up RotateTransition");
        RotateTransition windUp = (RotateTransition) seq.getChildren().get(0);
        assertEquals(HitAnimation.WIND_UP_ANGLE, windUp.getToAngle(), 1e-9);
        assertEquals(Interpolator.EASE_OUT, windUp.getInterpolator());

        // Phase 2: Strike
        assertTrue(seq.getChildren().get(1) instanceof RotateTransition, "Phase 2 must be strike RotateTransition");
        RotateTransition strike = (RotateTransition) seq.getChildren().get(1);
        assertEquals(HitAnimation.STRIKE_ANGLE, strike.getToAngle(), 1e-9);
        assertEquals(Interpolator.EASE_IN, strike.getInterpolator());

        // Phase 3: Impact (contains wheel shake)
        assertTrue(seq.getChildren().get(2) instanceof ParallelTransition, "Phase 3 must be impact ParallelTransition");
        ParallelTransition impact = (ParallelTransition) seq.getChildren().get(2);
        boolean hasShake = impact.getChildren().stream()
                .anyMatch(t -> t instanceof TranslateTransition);
        assertTrue(hasShake, "Impact must include a TranslateTransition on the wheel");

        // Phase 4: Retreat
        assertTrue(seq.getChildren().get(3) instanceof ParallelTransition, "Phase 4 must be retreat ParallelTransition");

        // Simulate transition completion
        wheel.setTranslateX(5.0);
        wheel.setTranslateY(-3.0);
        seq.getOnFinished().handle(null);

        assertTrue(callbackRan.get(), "onFinished callback should be executed on completion");
        assertEquals(0.0, wheel.getTranslateX(), 1e-9, "Wheel translation must reset to 0 on finish");
        assertEquals(0.0, wheel.getTranslateY(), 1e-9, "Wheel translation must reset to 0 on finish");
    }

    // ── Full chain simulation with 2 and 20 names (Acceptance Criterion) ──────

    @ParameterizedTest(name = "Step 6 full chain works for {0} participants")
    @ValueSource(ints = {2, 20})
    void fullChainSimulationWorksWith2And20Participants(int n) {
        List<Participant> participants = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            participants.add(new Participant("Player " + (i + 1)));
        }

        // 1. Choose winner first
        int winnerIndex = n - 1; // pick last participant
        Participant winner = participants.get(winnerIndex);

        // 2. Compute target rotation angle
        double offset = SpinAnimation.computeRandomOffset(n, 0.2);
        double targetAngle = SpinAnimation.computeTargetAngle(0.0, winnerIndex, n, 6, offset);

        // 3. Verify wheel lands on winner slice under pointer
        int landedSlice = SpinAnimation.getSliceIndexAtPointer(targetAngle, n);
        assertEquals(winnerIndex, landedSlice, "Wheel must stop with winner under pointer");

        // 4. Hammer hit animation executes
        HitAnimation hitAnim = new HitAnimation();
        Group hammer = new Group();
        Group wheel = new Group();

        AtomicReference<String> dialogMessage = new AtomicReference<>();
        SequentialTransition seq = hitAnim.buildHit(hammer, wheel, () -> {
            // Emulate dialog display
            dialogMessage.set(winner.getName() + " has been banned!");
        });

        seq.getOnFinished().handle(null);

        // 5. Verify dialog announces exact winning participant
        assertEquals(winner.getName() + " has been banned!", dialogMessage.get(),
                "Dialog must announce the exact banned winner");
    }
}
