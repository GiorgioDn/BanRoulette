package com.example.core.selector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class WinnerSelectorTest {

    @Test
    @DisplayName("Deterministic selection with injected Random")
    void deterministicSelection() {
        Random seededRandom = new Random(12345);
        WinnerSelector selector = new WinnerSelector(seededRandom);

        // Pre-calculated outcomes for seed 12345 with n=5
        Random reference = new Random(12345);
        for (int i = 0; i < 20; i++) {
            int expected = reference.nextInt(5);
            int actual = selector.selectWinnerIndex(5);
            assertEquals(expected, actual);
        }
    }

    @Test
    @DisplayName("selectWinner picks item from list")
    void selectWinnerFromList() {
        List<String> names = List.of("Alice", "Bob", "Charlie", "Diana");
        WinnerSelector selector = new WinnerSelector(new Random(99));

        String winner = selector.selectWinner(names);
        assertTrue(names.contains(winner));
    }

    @Test
    @DisplayName("selectWinnerIndex throws when participants < 2")
    void throwsOnTooFewParticipants() {
        WinnerSelector selector = new WinnerSelector();
        assertThrows(IllegalArgumentException.class, () -> selector.selectWinnerIndex(1));
        assertThrows(IllegalArgumentException.class, () -> selector.selectWinnerIndex(0));
        assertThrows(IllegalArgumentException.class, () -> selector.selectWinner(List.of("Solo")));
        assertThrows(IllegalArgumentException.class, () -> selector.selectWinner(null));
    }

    @Test
    @DisplayName("Fairness distribution check across 10,000 picks")
    void distributionCheck() {
        WinnerSelector selector = new WinnerSelector(new Random(42));
        int n = 4;
        int trials = 10_000;
        int[] counts = new int[n];

        for (int i = 0; i < trials; i++) {
            counts[selector.selectWinnerIndex(n)]++;
        }

        // Each bucket should get roughly 2,500 picks (±15%)
        for (int i = 0; i < n; i++) {
            assertTrue(counts[i] > 2000 && counts[i] < 3000,
                    "Count for bucket " + i + " was " + counts[i]);
        }
    }
}
