package com.example.db;

import com.example.model.BannedName;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BannedDao} using an in-memory H2 database.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BannedDaoTest {

    private static DatabaseManager db;
    private BannedDao bannedDao;

    @BeforeAll
    static void setUpDb() throws SQLException {
        db = new DatabaseManager();
        db.connect(DatabaseManager.IN_MEMORY_URL);
    }

    @AfterAll
    static void tearDownDb() {
        db.disconnect();
    }

    @BeforeEach
    void setUp() throws SQLException {
        bannedDao = new BannedDao(db.getConnection());
        // Clean table before each test
        bannedDao.deleteAll();
    }

    // ── insert & recordBan ─────────────────────────────────────────────────────

    @Test
    @Order(1)
    void insertReturnsGeneratedId() throws SQLException {
        long id = bannedDao.insert("Eve");
        assertTrue(id > 0);
    }

    @Test
    @Order(2)
    void insertedNameAppearsInFindAllWithCountOne() throws SQLException {
        bannedDao.insert("Frank");
        List<BannedName> all = bannedDao.findAll();
        assertEquals(1, all.size());
        assertEquals("Frank", all.get(0).getName());
        assertEquals(1, all.get(0).getBanCount());
    }

    @Test
    @Order(3)
    void recordBanIncrementsCountForExistingName() throws SQLException {
        bannedDao.recordBan("George");
        assertEquals(1, bannedDao.getBanCount("George"));

        bannedDao.recordBan("George");
        assertEquals(2, bannedDao.getBanCount("George"));

        bannedDao.recordBan("George");
        assertEquals(3, bannedDao.getBanCount("George"));

        // Still only 1 distinct row in the table
        assertEquals(1, bannedDao.count());
    }

    // ── findAll ────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    void findAllOnEmptyTableReturnsEmptyList() throws SQLException {
        assertTrue(bannedDao.findAll().isEmpty());
    }

    @Test
    @Order(5)
    void findAllOrdersByBanCountDescending() throws SQLException {
        bannedDao.recordBan("LowBan");
        bannedDao.recordBan("HighBan");
        bannedDao.recordBan("HighBan");
        bannedDao.recordBan("HighBan");

        List<BannedName> all = bannedDao.findAll();
        assertEquals(2, all.size());
        assertEquals("HighBan", all.get(0).getName());
        assertEquals(3, all.get(0).getBanCount());
        assertEquals("LowBan", all.get(1).getName());
        assertEquals(1, all.get(1).getBanCount());
    }

    // ── count & totalBans ──────────────────────────────────────────────────────

    @Test
    @Order(6)
    void countReflectsDistinctNames() throws SQLException {
        assertEquals(0, bannedDao.count());
        bannedDao.recordBan("Grace");
        assertEquals(1, bannedDao.count());
        bannedDao.recordBan("Grace");
        assertEquals(1, bannedDao.count()); // same name, still 1 distinct
        bannedDao.recordBan("Heidi");
        assertEquals(2, bannedDao.count());
    }

    @Test
    @Order(7)
    void totalBansSumsAllCounts() throws SQLException {
        assertEquals(0, bannedDao.totalBans());
        bannedDao.recordBan("A"); // 1
        bannedDao.recordBan("A"); // 2
        bannedDao.recordBan("B"); // 1
        bannedDao.recordBan("C"); // 1
        assertEquals(4, bannedDao.totalBans());
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    @Order(8)
    void deleteRemovesBannedName() throws SQLException {
        long id = bannedDao.insert("Ivan");
        bannedDao.delete(id);
        assertTrue(bannedDao.findAll().isEmpty());
        assertEquals(0, bannedDao.getBanCount("Ivan"));
    }

    @Test
    @Order(9)
    void deleteMissingIdIsNoOp() {
        assertDoesNotThrow(() -> bannedDao.delete(99_999L),
                "Deleting a non-existent id should be a silent no-op");
    }

    // ── restore (reset ban and ensure presence in names) ───────────────────────

    @Test
    @Order(10)
    void restoreResetsBanRecord() throws SQLException {
        NameDao nameDao = new NameDao(db.getConnection());
        nameDao.deleteAll();

        long id = bannedDao.insert("Judy");
        assertEquals(1, bannedDao.count());

        com.example.model.Participant restored = bannedDao.restore(id, nameDao);
        assertNotNull(restored);
        assertEquals("Judy", restored.getName());

        // Should be removed from banned_names (count reset to 0)
        assertEquals(0, bannedDao.count());
        assertTrue(bannedDao.findAll().isEmpty());

        // Should be present in names
        List<com.example.model.Participant> participants = nameDao.findAll();
        assertEquals(1, participants.size());
        assertEquals("Judy", participants.get(0).getName());
    }

    // ── deleteAll ──────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    void deleteAllClearsAllBannedNames() throws SQLException {
        bannedDao.insert("Ban1");
        bannedDao.insert("Ban2");
        assertEquals(2, bannedDao.count());

        bannedDao.deleteAll();
        assertEquals(0, bannedDao.count());
        assertTrue(bannedDao.findAll().isEmpty());
    }
}
