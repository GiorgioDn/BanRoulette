package com.example.core.selector;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Fair winner selection for the roulette wheel.
 *
 * <p>Uses {@link SecureRandom} by default to pick the winning index prior to
 * the spin animation. Accepts an injected {@link RandomGenerator} for deterministic
 * and reproducible unit testing.
 */
public class WinnerSelector {

    private final RandomGenerator random;

    public WinnerSelector() {
        this(new SecureRandom());
    }

    public WinnerSelector(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random generator must not be null");
    }

    /**
     * Picks a random winning slice index in the range {@code [0, participantCount)}.
     *
     * @param participantCount total number of participants (must be >= 2)
     * @return the winning slice index
     */
    public int selectWinnerIndex(int participantCount) {
        if (participantCount < 2) {
            throw new IllegalArgumentException("Need at least 2 participants to select a winner");
        }
        return random.nextInt(participantCount);
    }

    /**
     * Picks a winning item from the provided list.
     *
     * @param participants the list of participants (size >= 2)
     * @param <T>          type of participant
     * @return the selected winning participant
     */
    public <T> T selectWinner(List<T> participants) {
        if (participants == null || participants.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 participants to select a winner");
        }
        int index = selectWinnerIndex(participants.size());
        return participants.get(index);
    }
}
