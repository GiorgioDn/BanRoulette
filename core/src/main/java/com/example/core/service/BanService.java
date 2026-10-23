package com.example.core.service;

import com.example.core.repository.BannedRepository;
import com.example.core.repository.NameRepository;
import com.example.model.BannedName;
import com.example.model.Participant;
import com.example.model.SavedName;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Domain service orchestrating ban rules and participant lifecycle across
 * repository interfaces.
 *
 * <p>Free of threading and UI frameworks so each platform (JavaFX Task on Desktop,
 * Coroutines on Android) can invoke operations on background threads as appropriate.
 */
public class BanService {

    private final NameRepository nameRepository;
    private final BannedRepository bannedRepository;

    public BanService(NameRepository nameRepository, BannedRepository bannedRepository) {
        this.nameRepository = Objects.requireNonNull(nameRepository, "nameRepository must not be null");
        this.bannedRepository = Objects.requireNonNull(bannedRepository, "bannedRepository must not be null");
    }

    public NameRepository getNameRepository() {
        return nameRepository;
    }

    public BannedRepository getBannedRepository() {
        return bannedRepository;
    }

    /**
     * Atomically records a ban for the given participant.
     */
    public BannedName ban(Participant participant) {
        Objects.requireNonNull(participant, "participant must not be null");
        return bannedRepository.ban(participant);
    }

    /**
     * Atomically records a ban for the given name string.
     */
    public BannedName ban(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return bannedRepository.ban(name);
    }

    /**
     * Restores (removes) a participant from the banned history.
     */
    public void restore(long id) {
        bannedRepository.restore(id);
    }

    /**
     * Returns the full history of banned names.
     */
    public List<BannedName> getBannedHistory() {
        return bannedRepository.findAll();
    }

    /**
     * Returns the ban count for a specific name.
     */
    public int getBanCount(String name) {
        return bannedRepository.getBanCount(name);
    }

    /**
     * Saves all participant names spun on the wheel to the catalog.
     */
    public void recordSpinUsage(Collection<String> participantNames) {
        nameRepository.recordUsage(participantNames);
    }

    /**
     * Retrieves participants from the most recent session within 2 minutes.
     */
    public List<String> getLastUsedNames() {
        return nameRepository.findLastUsedNames();
    }

    /**
     * Returns all saved names in the persistent catalog.
     */
    public List<SavedName> getSavedNames() {
        return nameRepository.findAllSaved();
    }

    /**
     * Removes a name from the persistent catalog by ID.
     */
    public void removeSavedName(long id) {
        nameRepository.remove(id);
    }

    /**
     * Removes a name from the persistent catalog by name.
     */
    public void removeSavedNameByName(String name) {
        nameRepository.removeByName(name);
    }

    /**
     * Clears all saved catalog entries.
     */
    public void clearAllSavedNames() {
        nameRepository.clear();
    }

    /**
     * Clears all banned history records.
     */
    public void clearAllBanned() {
        bannedRepository.clear();
    }
}
