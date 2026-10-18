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
    void automaticSeedFromNamesAndBanned() throws SQLException {
        NameDao nameDao = new NameDao(db.getConnection());
        nameDao.insert("David");

        BannedDao bannedDao = new BannedDao(db.getConnection());
        bannedDao.recordBan("Eve");

        // Both David and Eve should have been automatically added to saved_names
        List<SavedName> list = dao.findAll();
        List<String> names = list.stream().map(SavedName::getName).toList();

        assertTrue(names.contains("David"), "David should be in saved_names");
        assertTrue(names.contains("Eve"), "Eve should be in saved_names");
    }
}
