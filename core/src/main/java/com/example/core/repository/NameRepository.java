package com.example.core.repository;

import com.example.model.Participant;
import com.example.model.SavedName;

import java.util.Collection;
import java.util.List;

/**
 * Repository interface for managing participant and saved names data.
 *
 * <p>Implemented by H2 on Desktop and Room (SQLite) on Android.
 */
public interface NameRepository {

    /** Returns all participants in the repository. */
    List<Participant> findAll();

    /** Adds or saves a participant name. */
    Participant add(String name);

    /** Removes a saved participant row by primary key. */
    void remove(long id);

    /** Removes a saved participant row by name (case-insensitive). */
    void removeByName(String name);

    /** Returns all persistent saved names in the catalog. */
    List<SavedName> findAllSaved();

    /**
     * Records usage of names upon wheel spin, updating timestamps
     * and adding new names if not already present.
     */
    void recordUsage(Collection<String> names);

    /**
     * Retrieves distinct names used within a 2-minute interval of the
     * most recent spin.
     */
    List<String> findLastUsedNames();

    /** Returns total count of saved names. */
    int count();

    /** Clears all saved names. */
    void clear();
}
