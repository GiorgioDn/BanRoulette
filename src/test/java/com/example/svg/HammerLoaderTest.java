package com.example.svg;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HammerLoader}.
 */
class HammerLoaderTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void loadHammerReturnsNonEmptyGroup() {
        HammerLoader loader = new HammerLoader();
        Group hammer = loader.loadHammer();

        assertNotNull(hammer, "Hammer group must not be null");
        assertEquals("ban-hammer", hammer.getId(), "Root group id should be 'ban-hammer'");
        assertFalse(hammer.getChildren().isEmpty(), "Hammer group must have children");

        // The root contains an inner group offset by (-PIVOT_X, -PIVOT_Y)
        Node inner = hammer.getChildren().get(0);
        assertTrue(inner instanceof Group, "Child of root should be inner Group");
        Group innerGroup = (Group) inner;
        assertFalse(innerGroup.getChildren().isEmpty(), "Inner group must contain shape elements");
        assertTrue(innerGroup.getChildren().size() >= 5, "Hammer should consist of multiple detailed shape parts");
    }

    @Test
    void pivotIsOffsetToHandleEnd() {
        HammerLoader loader = new HammerLoader();
        Group hammer = loader.loadHammer();

        Node inner = hammer.getChildren().get(0);
        assertEquals(-HammerLoader.PIVOT_X, inner.getTranslateX(), 1e-9,
                "Inner group translateX must be -PIVOT_X");
        assertEquals(-HammerLoader.PIVOT_Y, inner.getTranslateY(), 1e-9,
                "Inner group translateY must be -PIVOT_Y");
    }
}
