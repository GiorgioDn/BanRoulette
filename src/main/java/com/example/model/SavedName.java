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
    private LocalDateTime lastUsedAt;

    /** In-memory constructor. */
    public SavedName(String name) {
        this(0, name, LocalDateTime.now());
    }

    /** In-memory constructor with lastUsedAt. */
    public SavedName(String name, LocalDateTime lastUsedAt) {
        this(0, name, lastUsedAt);
    }

    /** Constructor with id and name. */
    public SavedName(long id, String name) {
        this(id, name, LocalDateTime.now());
    }

    /** Full constructor for DB-backed rows. */
    public SavedName(long id, String name, LocalDateTime lastUsedAt) {
        this.id = id;
        this.name = name;
        this.lastUsedAt = (lastUsedAt != null) ? lastUsedAt : LocalDateTime.now();
    }

    public long getId()                               { return id; }
    public void setId(long id)                        { this.id = id; }

    public String getName()                           { return name; }
    public void setName(String name)                  { this.name = name; }

    public LocalDateTime getLastUsedAt()              { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt){ this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getCreatedAt()               { return lastUsedAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.lastUsedAt = createdAt; }

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
