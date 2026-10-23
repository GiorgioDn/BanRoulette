package com.example.core.service;

import com.example.core.repository.BannedRepository;
import com.example.core.repository.NameRepository;
import com.example.model.BannedName;
import com.example.model.Participant;
import com.example.model.SavedName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BanServiceTest {

    private InMemoryNameRepository nameRepo;
    private InMemoryBannedRepository bannedRepo;
    private BanService banService;

    @BeforeEach
    void setUp() {
        nameRepo = new InMemoryNameRepository();
        bannedRepo = new InMemoryBannedRepository();
        banService = new BanService(nameRepo, bannedRepo);
    }

    @Test
    @DisplayName("ban increments count on repeat bans")
    void repeatBansIncrementCount() {
        Participant p = new Participant("Alice");

        BannedName ban1 = banService.ban(p);
        assertEquals(1, ban1.getBanCount());
        assertEquals("Alice", ban1.getName());

        BannedName ban2 = banService.ban(p);
        assertEquals(2, ban2.getBanCount());
        assertEquals(2, banService.getBanCount("Alice"));
    }

    @Test
    @DisplayName("restore removes participant from banned list")
    void restoreBan() {
        Participant p = new Participant("Bob");
        BannedName record = banService.ban(p);

        assertEquals(1, banService.getBannedHistory().size());
        banService.restore(record.getId());
        assertEquals(0, banService.getBannedHistory().size());
        assertEquals(0, banService.getBanCount("Bob"));
    }

    @Test
    @DisplayName("recordSpinUsage saves participant names to catalog")
    void recordSpinUsage() {
        banService.recordSpinUsage(List.of("Alice", "Bob", "Charlie"));

        List<SavedName> saved = banService.getSavedNames();
        assertEquals(3, saved.size());
        assertTrue(saved.stream().anyMatch(s -> s.getName().equals("Alice")));
        assertTrue(saved.stream().anyMatch(s -> s.getName().equals("Bob")));
        assertTrue(saved.stream().anyMatch(s -> s.getName().equals("Charlie")));
    }

    @Test
    @DisplayName("clear operations wipe respective repositories")
    void clearRepositories() {
        banService.ban("Alice");
        banService.recordSpinUsage(List.of("Alice"));

        banService.clearAllBanned();
        assertEquals(0, banService.getBannedHistory().size());

        banService.clearAllSavedNames();
        assertEquals(0, banService.getSavedNames().size());
    }

    // ── In-memory test doubles ────────────────────────────────────────────────

    static class InMemoryNameRepository implements NameRepository {
        private final Map<Long, SavedName> storage = new LinkedHashMap<>();
        private long idSeq = 1;

        @Override
        public List<Participant> findAll() {
            return storage.values().stream()
                    .map(sn -> new Participant(sn.getId(), sn.getName(), sn.getLastUsedAt()))
                    .toList();
        }

        @Override
        public Participant add(String name) {
            SavedName sn = new SavedName(idSeq++, name, LocalDateTime.now());
            storage.put(sn.getId(), sn);
            return new Participant(sn.getId(), sn.getName(), sn.getLastUsedAt());
        }

        @Override
        public void remove(long id) {
            storage.remove(id);
        }

        @Override
        public void removeByName(String name) {
            storage.entrySet().removeIf(e -> e.getValue().getName().equalsIgnoreCase(name));
        }

        @Override
        public List<SavedName> findAllSaved() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void recordUsage(Collection<String> names) {
            for (String name : names) {
                boolean found = false;
                for (SavedName sn : storage.values()) {
                    if (sn.getName().equalsIgnoreCase(name)) {
                        sn.setLastUsedAt(LocalDateTime.now());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    SavedName sn = new SavedName(idSeq++, name, LocalDateTime.now());
                    storage.put(sn.getId(), sn);
                }
            }
        }

        @Override
        public List<String> findLastUsedNames() {
            return storage.values().stream().map(SavedName::getName).toList();
        }

        @Override
        public int count() {
            return storage.size();
        }

        @Override
        public void clear() {
            storage.clear();
        }
    }

    static class InMemoryBannedRepository implements BannedRepository {
        private final Map<Long, BannedName> storage = new LinkedHashMap<>();
        private long idSeq = 1;

        @Override
        public List<BannedName> findAll() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public BannedName ban(Participant participant) {
            return ban(participant.getName());
        }

        @Override
        public BannedName ban(String name) {
            for (BannedName bn : storage.values()) {
                if (bn.getName().equalsIgnoreCase(name)) {
                    bn.incrementCount();
                    bn.setBannedAt(LocalDateTime.now());
                    return bn;
                }
            }
            BannedName newRecord = new BannedName(idSeq++, name, 1, LocalDateTime.now());
            storage.put(newRecord.getId(), newRecord);
            return newRecord;
        }

        @Override
        public void restore(long id) {
            storage.remove(id);
        }

        @Override
        public BannedName findByName(String name) {
            return storage.values().stream()
                    .filter(bn -> bn.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public int getBanCount(String name) {
            BannedName bn = findByName(name);
            return (bn != null) ? bn.getBanCount() : 0;
        }

        @Override
        public int count() {
            return storage.size();
        }

        @Override
        public int totalBans() {
            return storage.values().stream().mapToInt(BannedName::getBanCount).sum();
        }

        @Override
        public void clear() {
            storage.clear();
        }
    }
}
