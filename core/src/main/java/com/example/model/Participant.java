package com.example.model;

import java.time.LocalDateTime;

/**
 * Represents a participant eligible to be spun on the wheel.
 */
public class Participant {

    private long id;
    private String name;
    private LocalDateTime createdAt;

    /** In-memory constructor (defaults to id=0, createdAt=now). */
    public Participant(String name) {
        this(0, name, LocalDateTime.now());
    }

    /** Constructor with id and name. */
    public Participant(long id, String name) {
        this(id, name, LocalDateTime.now());
    }

    /** Full constructor for DB-backed rows. */
    public Participant(long id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public long getId()                    { return id; }
    public void setId(long id)             { this.id = id; }

    public String getName()                { return name; }
    public void setName(String name)       { this.name = name; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    /** Used by cell factories and lists to display the participant's name. */
    @Override
    public String toString() { return name; }
}
