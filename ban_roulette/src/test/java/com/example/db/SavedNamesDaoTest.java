package com.example.db;

import com.example.model.SavedName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SavedNamesDao}.
 */
class SavedNamesDaoTest {

    private DatabaseManager db;
    private SavedNamesDao dao;

    @BeforeEach
    void setUp() throws SQLException {
        db = new DatabaseManager();
        db.connect(DatabaseManager.IN_MEMORY_URL);
        dao = new SavedNamesDao(db.getConnection());
        dao.deleteAll();
    }

    @AfterEach
    void tearDown() {
        db.disconnect();
    }

    @Test
    void insertAndFindAllAlphabetical() throws SQLException {
        dao.insert("Charlie");
        dao.insert("Alice");
        dao.insert("Bob");

        List<SavedName> list = dao.findAll();
        assertEquals(3, list.size());
        assertEquals("Alice", list.get(0).getName());
        assertEquals("Bob", list.get(1).getName());
        assertEquals("Charlie", list.get(2).getName());
    }

    @Test
    void insertDuplicateCaseInsensitiveIsIdempotent() throws SQLException {
        long id1 = dao.insert("Alice");
        long id2 = dao.insert("alice");
        long id3 = dao.insert("ALICE");

        assertEquals(id1, id2);
        assertEquals(id1, id3);
        assertEquals(1, dao.count());
    }

    @Test
    void deleteById() throws SQLException {
        long id1 = dao.insert("Alice");
        long id2 = dao.insert("Bob");

        assertEquals(2, dao.count());
        dao.delete(id1);

        List<SavedName> list = dao.findAll();
        assertEquals(1, list.size());
        assertEquals("Bob", list.get(0).getName());
    }

    @Test
    void deleteByName() throws SQLException {
        dao.insert("Alice");
        dao.insert("Bob");

        dao.deleteByName("alice");
        assertEquals(1, dao.count());
        assertEquals("Bob", dao.findAll().get(0).getName());
    }

    @Test
    void deleteAll() throws SQLException {
        dao.insert("Alice");
        dao.insert("Bob");
        assertEquals(2, dao.count());

        dao.deleteAll();
        assertEquals(0, dao.count());
        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    void blankNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> dao.insert("   "));
        assertThrows(IllegalArgumentException.class, () -> dao.insert(null));
    }

    @Test
    void recordUsageAddsNewNamesAndUpdatesLastUsedAt() throws SQLException, InterruptedException {
        dao.recordUsage("Alice");
        SavedName sn1 = dao.findByName("Alice");
        assertNotNull(sn1);
        assertNotNull(sn1.getLastUsedAt());

        Thread.sleep(20);
        dao.recordUsage("Alice");
        SavedName sn2 = dao.findByName("Alice");
        assertNotNull(sn2);
        assertEquals(1, dao.count(), "Alice should only be saved once in saved_names");
        assertTrue(sn2.getLastUsedAt().isAfter(sn1.getLastUsedAt()) || sn2.getLastUsedAt().isEqual(sn1.getLastUsedAt()));
    }

    @Test
    void recordUsageWithDuplicateParticipantsSavesOnlyOnce() throws SQLException {
        // Simulates wheel with multiple tickets for the same participant
        dao.recordUsage(List.of("Alice", "Bob", "Alice", "Bob", "Alice"));
        assertEquals(2, dao.count(), "Each distinct name should only be saved once");
        assertNotNull(dao.findByName("Alice"));
        assertNotNull(dao.findByName("Bob"));
    }

    @Test
    void recordBanAlsoSeedsSavedNames() throws SQLException {
        BannedDao bannedDao = new BannedDao(db.getConnection());
        bannedDao.recordBan("Eve");

        List<SavedName> list = dao.findAll();
        List<String> names = list.stream().map(SavedName::getName).toList();
        assertTrue(names.contains("Eve"), "Eve should be in saved_names");
    }

    @Test
    void findLastUsedNamesReturnsWithinTwoMinutesInterval() throws SQLException {
        long now = System.currentTimeMillis();
        // 5 minutes ago (outside the 2-minute interval)
        dao.recordUsage("OldCharlie", new java.sql.Timestamp(now - 300_000));

        // 1 minute ago (within the 2-minute interval of the latest usage)
        dao.recordUsage("RecentDavid", new java.sql.Timestamp(now - 60_000));

        // Latest usage at now
        java.sql.Timestamp latest = new java.sql.Timestamp(now);
        dao.recordUsage("Alice", latest);
        dao.recordUsage("Bob", latest);

        List<String> lastUsed = dao.findLastUsedNames();
        assertEquals(3, lastUsed.size(), "Should return names used within 2 minutes of the latest usage");
        assertTrue(lastUsed.contains("Alice"));
        assertTrue(lastUsed.contains("Bob"));
        assertTrue(lastUsed.contains("RecentDavid"));
        assertFalse(lastUsed.contains("OldCharlie"), "Names older than 2 minutes before latest should not be included");
    }

    @Test
    void findLastUsedNamesOnEmptyTableReturnsEmptyList() throws SQLException {
        assertTrue(dao.findLastUsedNames().isEmpty());
    }
}
