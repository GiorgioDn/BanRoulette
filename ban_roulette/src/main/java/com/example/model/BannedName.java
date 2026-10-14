package com.example.model;

import java.time.LocalDateTime;

/**
 * Represents a name that has been banned from the wheel.
 * Persisted in the {@code banned_names} table (Step 3).
 */
public class BannedName {

    private long id;
    private String name;
    private LocalDateTime bannedAt;

    /** In-memory constructor (used before DB is wired up). */
    public BannedName(String name) {
        this.name = name;
    }

    /** Full constructor for DB-backed rows. */
    public BannedName(long id, String name, LocalDateTime bannedAt) {
        this.id = id;
        this.name = name;
        this.bannedAt = bannedAt;
    }

    public long getId()                    { return id; }
    public void setId(long id)             { this.id = id; }

    public String getName()                { return name; }
    public void setName(String name)       { this.name = name; }

    public LocalDateTime getBannedAt()               { return bannedAt; }
    public void setBannedAt(LocalDateTime bannedAt)  { this.bannedAt = bannedAt; }

    @Override
    public String toString() { return name; }
}
