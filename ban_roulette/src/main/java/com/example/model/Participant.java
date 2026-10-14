package com.example.model;

import java.time.LocalDateTime;

/**
 * Represents a participant eligible to be spun on the wheel.
 * Persisted in the {@code names} table (Step 3).
 */
public class Participant {

    private long id;
    private String name;
    private LocalDateTime createdAt;

    /** In-memory constructor (used before DB is wired up). */
    public Participant(String name) {
        this.name = name;
    }

    /** Full constructor for DB-backed rows. */
    public Participant(long id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public long getId()                    { return id; }
    public void setId(long id)             { this.id = id; }

    public String getName()                { return name; }
    public void setName(String name)       { this.name = name; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    /** Used by the ListView cell factory to display the participant's name. */
    @Override
    public String toString() { return name; }
}
