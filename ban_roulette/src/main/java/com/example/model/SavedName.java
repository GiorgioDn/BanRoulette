package com.example.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a participant name stored in the database's saved names catalog.
 *
 * <p>Persisted in the {@code saved_names} table. This table acts as a persistent
 * catalog of all names entered into the application, allowing them to be quickly
 * browsed and clicked to be added back into the active roulette wheel at any time.
 */
public class SavedName {

    private long id;
    private String name;
    private LocalDateTime createdAt;

    /** In-memory constructor. */
    public SavedName(String name) {
        this(0, name, LocalDateTime.now());
    }

    /** Constructor with id and name. */
    public SavedName(long id, String name) {
        this(id, name, LocalDateTime.now());
    }

    /** Full constructor for DB-backed rows. */
    public SavedName(long id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    public long getId()                             { return id; }
    public void setId(long id)                      { this.id = id; }

    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavedName other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
