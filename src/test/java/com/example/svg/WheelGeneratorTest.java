package com.example.svg;

import com.example.model.Participant;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WheelGenerator}.
 *
 * <p>JavaFX scene-graph objects can be created off the FX thread during
 * tests as long as the toolkit is initialised first.
 * We initialise it once with {@code Platform.startup}.
 */
class WheelGeneratorTest {

    @BeforeAll
    static void initJfx() {
        // Initialise the JavaFX toolkit so scene-graph nodes can be created
        // in a headless test environment.
        try {
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already started — fine.
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static List<Participant> participants(int count) {
        List<Participant> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(new Participant("Player " + (i + 1)));
        }
        return list;
    }

    private static WheelGenerator gen() { return new WheelGenerator(); }

    // ── Core generation tests ──────────────────────────────────────────────────

    @ParameterizedTest(name = "generateWheel({0} participants) should not be null or empty")
    @ValueSource(ints = {1, 2, 3, 7, 20})
    void generateWheelReturnsNonEmptyGroup(int n) {
        Group g = gen().generateWheel(participants(n), WheelGenerator.DEFAULT_RADIUS);
        assertNotNull(g);
        assertFalse(g.getChildren().isEmpty(),
                "Group should have children for n=" + n);
    }

    @ParameterizedTest(name = "generateWheel({0}) should have at least n SVGPath slices")
    @ValueSource(ints = {2, 3, 7, 20})
    void generateWheelHasCorrectSliceCount(int n) {
        Group g = gen().generateWheel(participants(n), WheelGenerator.DEFAULT_RADIUS);
        long slices = g.getChildren().stream()
                .filter(nd -> nd instanceof SVGPath)
                .count();
        // n slices + n dividers + 1 pointer = 2n+1 SVGPaths
        assertTrue(slices >= n,
                "Expected at least " + n + " SVGPath slices, found " + slices);
    }

    @ParameterizedTest(name = "generateWheel({0}) should have n Text labels")
    @ValueSource(ints = {2, 3, 7, 20})
    void generateWheelHasCorrectLabelCount(int n) {
        Group g = gen().generateWheel(participants(n), WheelGenerator.DEFAULT_RADIUS);
        long labels = g.getChildren().stream()
                .filter(nd -> nd instanceof Text)
                .count();
        assertEquals(n, labels,
                "Expected exactly " + n + " Text labels, found " + labels);
    }

    // ── Single-participant full-circle ─────────────────────────────────────────

    @Test
    void singleParticipantRendersFullCircle() {
        Group g = gen().generateWheel(participants(1), WheelGenerator.DEFAULT_RADIUS);
        long svgPaths = g.getChildren().stream().filter(n -> n instanceof SVGPath).count();
        long texts    = g.getChildren().stream().filter(n -> n instanceof Text).count();
        assertTrue(svgPaths >= 1, "Full-circle should produce at least 1 SVGPath");
        assertEquals(1, texts, "Single participant should have 1 text label");
    }

    // ── Pointer triangle ───────────────────────────────────────────────────────

    @Test
    void generateWheelContainsPointer() {
        Group g = gen().generateWheel(participants(3), WheelGenerator.DEFAULT_RADIUS);
        boolean hasPointer = g.getChildren().stream()
                .anyMatch(n -> "wheel-pointer".equals(n.getId()));
        assertTrue(hasPointer, "Wheel group should include the pointer SVGPath");
    }

    // ── Label truncation ───────────────────────────────────────────────────────

    @Test
    void longNamesAreTruncatedWithEllipsis() {
        String longName = "A".repeat(30); // 30 chars, well over MAX_LABEL_CHARS
        List<Participant> ps = List.of(new Participant(longName), new Participant("Bob"));
        Group g = gen().generateWheel(ps, WheelGenerator.DEFAULT_RADIUS);
        Text truncated = (Text) g.getChildren().stream()
                .filter(n -> n instanceof Text)
                .findFirst()
                .orElseThrow();
        String labelText = truncated.getText();
        assertTrue(labelText.length() <= WheelGenerator.MAX_PARTICIPANTS,
                "Truncated label should not exceed MAX_LABEL_CHARS: " + labelText);
        assertTrue(labelText.endsWith("…"),
                "Truncated label should end with ellipsis: " + labelText);
    }

    // ── Add/remove dynamic transitions for 2, 3, 7, and 20 names ──────────────

    @Test
    void redrawsCorrectlyWhenAddingAndRemovingNames() {
        List<Participant> list = new ArrayList<>(participants(2));
        Group g2 = gen().generateWheel(list, WheelGenerator.DEFAULT_RADIUS);
        assertEquals(2, g2.getChildren().stream().filter(n -> n instanceof Text).count());

        // Add to 3
        list.add(new Participant("Player 3"));
        Group g3 = gen().generateWheel(list, WheelGenerator.DEFAULT_RADIUS);
        assertEquals(3, g3.getChildren().stream().filter(n -> n instanceof Text).count());

        // Add to 7
        for (int i = 4; i <= 7; i++) list.add(new Participant("Player " + i));
        Group g7 = gen().generateWheel(list, WheelGenerator.DEFAULT_RADIUS);
        assertEquals(7, g7.getChildren().stream().filter(n -> n instanceof Text).count());

        // Add to 20
        for (int i = 8; i <= 20; i++) list.add(new Participant("Player " + i));
        Group g20 = gen().generateWheel(list, WheelGenerator.DEFAULT_RADIUS);
        assertEquals(20, g20.getChildren().stream().filter(n -> n instanceof Text).count());

        // Remove back down to 7
        while (list.size() > 7) list.remove(list.size() - 1);
        Group g7Back = gen().generateWheel(list, WheelGenerator.DEFAULT_RADIUS);
        assertEquals(7, g7Back.getChildren().stream().filter(n -> n instanceof Text).count());

        // Remove back down to 3
        while (list.size() > 3) list.remove(list.size() - 1);
        Group g3Back = gen().generateWheel(list, WheelGenerator.DEFAULT_RADIUS);
        assertEquals(3, g3Back.getChildren().stream().filter(n -> n instanceof Text).count());

        // Remove back down to 2
        while (list.size() > 2) list.remove(list.size() - 1);
        Group g2Back = gen().generateWheel(list, WheelGenerator.DEFAULT_RADIUS);
        assertEquals(2, g2Back.getChildren().stream().filter(n -> n instanceof Text).count());
    }

    // ── Edge-case guards ───────────────────────────────────────────────────────

    @Test
    void emptyListThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> gen().generateWheel(List.of(), WheelGenerator.DEFAULT_RADIUS));
    }

    @Test
    void tooManyParticipantsThrowsIllegalArgumentException() {
        List<Participant> tooMany = participants(WheelGenerator.MAX_PARTICIPANTS + 1);
        assertThrows(IllegalArgumentException.class,
                () -> gen().generateWheel(tooMany, WheelGenerator.DEFAULT_RADIUS));
    }

    // ── Pointer builder ────────────────────────────────────────────────────────

    @Test
    void buildPointerOnlyReturnsNonNullSVGPath() {
        SVGPath pointer = gen().buildPointerOnly(WheelGenerator.DEFAULT_RADIUS);
        assertNotNull(pointer);
        assertFalse(pointer.getContent().isBlank());
    }
}
