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
        db.getConnection().createStatement().execute("DELETE FROM banned_names");
    }

    // ── insert ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void insertReturnsGeneratedId() throws SQLException {
        long id = bannedDao.insert("Eve");
        assertTrue(id > 0);
    }

    @Test
    @Order(2)
    void insertedNameAppearsInFindAll() throws SQLException {
        bannedDao.insert("Frank");
        List<BannedName> all = bannedDao.findAll();
        assertEquals(1, all.size());
        assertEquals("Frank", all.get(0).getName());
    }

    // ── findAll ────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    void findAllOnEmptyTableReturnsEmptyList() throws SQLException {
        assertTrue(bannedDao.findAll().isEmpty());
    }

    @Test
    @Order(4)
    void findAllReturnsMostRecentFirst() throws SQLException {
        bannedDao.insert("OldBan");
        // Tiny sleep so timestamps differ (H2 TIMESTAMP precision is milliseconds)
        try { Thread.sleep(5); } catch (InterruptedException ignored) { }
        bannedDao.insert("NewBan");
        List<BannedName> all = bannedDao.findAll();
        assertEquals(2, all.size());
        assertEquals("NewBan", all.get(0).getName(), "Most recently banned should be first");
        assertEquals("OldBan", all.get(1).getName());
    }

    // ── count ──────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    void countReflectsInserts() throws SQLException {
        assertEquals(0, bannedDao.count());
        bannedDao.insert("Grace");
        assertEquals(1, bannedDao.count());
        bannedDao.insert("Heidi");
        assertEquals(2, bannedDao.count());
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    void deleteRemovesBannedName() throws SQLException {
        long id = bannedDao.insert("Ivan");
        bannedDao.delete(id);
        assertTrue(bannedDao.findAll().isEmpty());
    }

    @Test
    @Order(7)
    void deleteMissingIdIsNoOp() {
        assertDoesNotThrow(() -> bannedDao.delete(99_999L),
                "Deleting a non-existent id should be a silent no-op");
    }

    // ── restore (transactional move back to names) ────────────────────────────

    @Test
    @Order(8)
    void restoreMovesBannedNameToNamesTable() throws SQLException {
        NameDao nameDao = new NameDao(db.getConnection());
        nameDao.deleteAll();

        long id = bannedDao.insert("Judy");
        assertEquals(1, bannedDao.count());

        com.example.model.Participant restored = bannedDao.restore(id, nameDao);
        assertNotNull(restored);
        assertEquals("Judy", restored.getName());
        assertTrue(restored.getId() > 0);

        // Should be removed from banned_names
        assertEquals(0, bannedDao.count());
        assertTrue(bannedDao.findAll().isEmpty());

        // Should be present in names
        List<com.example.model.Participant> participants = nameDao.findAll();
        assertEquals(1, participants.size());
        assertEquals("Judy", participants.get(0).getName());
    }

    @Test
    @Order(9)
    void restoreNonExistentIdThrows() {
        NameDao nameDao = new NameDao(db.getConnection());
        assertThrows(SQLException.class, () -> bannedDao.restore(99_999L, nameDao),
                "Restoring a non-existent banned id should throw SQLException");
    }

    // ── deleteAll ──────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    void deleteAllClearsAllBannedNames() throws SQLException {
        bannedDao.insert("Ban1");
        bannedDao.insert("Ban2");
        assertEquals(2, bannedDao.count());

        bannedDao.deleteAll();
        assertEquals(0, bannedDao.count());
        assertTrue(bannedDao.findAll().isEmpty());
    }
}
