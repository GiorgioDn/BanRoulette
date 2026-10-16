package com.example.db;

import com.example.model.Participant;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NameDao} using an in-memory H2 database.
 *
 * <p>Each test runs against a freshly initialised schema so tests are fully
 * independent. The in-memory URL {@code DB_CLOSE_DELAY=-1} keeps the database
 * alive for the lifetime of the JVM (needed because multiple test classes share
 * the same JVM process and the connection is reopened between tests).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NameDaoTest {

    private static DatabaseManager db;
    private NameDao nameDao;
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
        nameDao   = new NameDao(db.getConnection());
        bannedDao = new BannedDao(db.getConnection());
        // Start each test with clean tables
        nameDao.deleteAll();
        db.getConnection().createStatement().execute("DELETE FROM banned_names");
    }

    // ── insert ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void insertReturnsGeneratedId() throws SQLException {
        long id = nameDao.insert("Alice");
        assertTrue(id > 0, "Generated id should be > 0");
    }

    @Test
    @Order(2)
    void insertedNameAppearsInFindAll() throws SQLException {
        nameDao.insert("Bob");
        List<Participant> all = nameDao.findAll();
        assertEquals(1, all.size());
        assertEquals("Bob", all.get(0).getName());
    }

    @Test
    @Order(3)
    void duplicatesAreAllowed() throws SQLException {
        nameDao.insert("Charlie");
        nameDao.insert("Charlie");
        assertEquals(2, nameDao.findAll().size(),
                "Duplicate names should be allowed (no UNIQUE constraint)");
    }

    // ── findAll ────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    void findAllOnEmptyTableReturnsEmptyList() throws SQLException {
        assertTrue(nameDao.findAll().isEmpty());
    }

    @Test
    @Order(5)
    void findAllReturnsOldestFirst() throws SQLException {
        nameDao.insert("First");
        nameDao.insert("Second");
        nameDao.insert("Third");
        List<Participant> all = nameDao.findAll();
        assertEquals(3, all.size());
        assertEquals("First",  all.get(0).getName());
        assertEquals("Second", all.get(1).getName());
        assertEquals("Third",  all.get(2).getName());
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    void deleteRemovesOnlyTargetRow() throws SQLException {
        long idA = nameDao.insert("Alpha");
        nameDao.insert("Beta");
        nameDao.delete(idA);
        List<Participant> remaining = nameDao.findAll();
        assertEquals(1, remaining.size());
        assertEquals("Beta", remaining.get(0).getName());
    }

    // ── deleteAll ──────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    void deleteAllClearsTable() throws SQLException {
        nameDao.insert("X");
        nameDao.insert("Y");
        nameDao.deleteAll();
        assertTrue(nameDao.findAll().isEmpty());
    }

    // ── ban (transactional move) ───────────────────────────────────────────────

    @Test
    @Order(8)
    void banMovesParticipantToBannedTable() throws SQLException {
        long id   = nameDao.insert("Diana");
        String returned = nameDao.ban(id, bannedDao);

        assertEquals("Diana", returned, "ban() should return the participant's name");
        assertTrue(nameDao.findAll().isEmpty(),
                "Participant should be removed from names");
        assertEquals(1, bannedDao.count(),
                "Participant should appear in banned_names");
        assertEquals("Diana", bannedDao.findAll().get(0).getName());
    }

    @Test
    @Order(9)
    void banNonExistentIdThrows() {
        assertThrows(SQLException.class, () -> nameDao.ban(99_999L, bannedDao),
                "Banning a non-existent id should throw SQLException");
    }
}
