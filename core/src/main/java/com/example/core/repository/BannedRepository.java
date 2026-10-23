package com.example.core.repository;

import com.example.model.BannedName;
import com.example.model.Participant;

import java.util.List;

/**
 * Repository interface for managing banned participants history.
 *
 * <p>Implemented by H2 on Desktop and Room (SQLite) on Android.
 * All ban operations must be atomic (executed within a single database transaction).
 */
public interface BannedRepository {

    /** Returns all banned names ordered by ban count descending, then recency. */
    List<BannedName> findAll();

    /**
     * Atomically records a ban for a participant: increments count if already banned,
     * or inserts with count 1 if first ban. Also ensures the name exists in the catalog.
     */
    BannedName ban(Participant participant);

    /**
     * Atomically records a ban for a name string.
     */
    BannedName ban(String name);

    /** Removes a name from the banned table (resets its ban status). */
    void restore(long id);

    /** Finds a banned record by name (case-insensitive) or null if never banned. */
    BannedName findByName(String name);

    /** Returns the ban count for a name, or 0 if not banned. */
    int getBanCount(String name);

    /** Returns the total count of distinct banned names. */
    int count();

    /** Returns the total sum of bans accumulated across all names. */
    int totalBans();

    /** Clears all banned history records. */
    void clear();
}
