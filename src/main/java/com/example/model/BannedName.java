package com.example.model;

import java.time.LocalDateTime;

/**
 * Represents a name that has been banned from the wheel.
 * Persisted in the {@code banned_names} table (Step 3).
 */
public class BannedName {

    private long id;
    private String name;
    private int banCount;
    private LocalDateTime bannedAt;

    /** In-memory constructor (used before DB is wired up). Defaults to 1 ban. */
    public BannedName(String name) {
        this(0, name, 1, LocalDateTime.now());
    }

    /** Constructor without count (defaults to 1 ban). */
    public BannedName(long id, String name, LocalDateTime bannedAt) {
        this(id, name, 1, bannedAt);
    }

    /** Full constructor for DB-backed rows with explicit ban count. */
    public BannedName(long id, String name, int banCount, LocalDateTime bannedAt) {
        this.id = id;
        this.name = name;
        this.banCount = Math.max(1, banCount);
        this.bannedAt = bannedAt;
    }

    public long getId()                              { return id; }
    public void setId(long id)                       { this.id = id; }

    public String getName()                          { return name; }
    public void setName(String name)                 { this.name = name; }

    public int getBanCount()                         { return banCount; }
    public void setBanCount(int banCount)            { this.banCount = Math.max(1, banCount); }
    public void incrementCount()                     { this.banCount++; }

    public LocalDateTime getBannedAt()               { return bannedAt; }
    public void setBannedAt(LocalDateTime bannedAt)  { this.bannedAt = bannedAt; }

    @Override
    public String toString() {
        return name + " (" + banCount + "×)";
    }
}
