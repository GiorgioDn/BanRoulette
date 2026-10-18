package com.example.db;

import org.junit.jupiter.api.*;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@link DatabaseManager} can open an in-memory H2 database,
 * create the schema, and close cleanly — without touching the filesystem.
 *
 * <p>Each test gets a fresh in-memory database that is torn down after the test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseManagerTest {

    private DatabaseManager db;

    @BeforeEach
    void setUp() throws SQLException {
        db = new DatabaseManager();
        db.connect(DatabaseManager.IN_MEMORY_URL);
    }

    @AfterEach
    void tearDown() {
        db.disconnect();
    }

    @Test
    @Order(1)
    void connectionIsOpen() {
        assertNotNull(db.getConnection(), "Connection should not be null after connect()");
    }

    @Test
    @Order(2)
    void schemaCreatedIdempotently() throws SQLException {
        // Calling connect on the same in-memory DB a second time should not throw
        // (tables already exist but CREATE TABLE IF NOT EXISTS is idempotent)
        assertDoesNotThrow(() -> db.connect(DatabaseManager.IN_MEMORY_URL),
                "Second connect() call should be idempotent");
    }

    @Test
    @Order(3)
    void disconnectReleasesConnection() {
        db.disconnect();
        assertNull(db.getConnection(), "Connection should be null after disconnect()");
    }

    @Test
    @Order(4)
    void disconnectIsIdempotent() {
        db.disconnect();
        assertDoesNotThrow(db::disconnect, "Second disconnect() call should not throw");
    }

    @Test
    @Order(5)
    @DisplayName("Running schema initialization twice on existing DB must not fail or wipe data")
    void schemaInitRunTwiceDoesNotFailOrWipeData(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws SQLException {
        java.nio.file.Path dbFile = tempDir.resolve("idempotent_test");
        String url = "jdbc:h2:" + dbFile.toAbsolutePath();

        // Run 1: Create DB and insert rows
        DatabaseManager db1 = new DatabaseManager();
        db1.connect(url);
        NameDao nameDao1 = new NameDao(db1.getConnection());
        BannedDao bannedDao1 = new BannedDao(db1.getConnection());

        nameDao1.insert("PersistentParticipant");
        bannedDao1.recordBan("PersistentBanned");
        db1.disconnect();

        // Run 2: Connect again (runs initSchema() again)
        DatabaseManager db2 = new DatabaseManager();
        assertDoesNotThrow(() -> db2.connect(url), "Second connect/initSchema should succeed without throwing");

        NameDao nameDao2 = new NameDao(db2.getConnection());
        BannedDao bannedDao2 = new BannedDao(db2.getConnection());

        // Verify data was NOT wiped or lost
        assertEquals(1, nameDao2.findAll().size(), "Participant in names should still exist");
        assertEquals("PersistentParticipant", nameDao2.findAll().get(0).getName());

        assertEquals(1, bannedDao2.count(), "Banned name in banned_names should still exist");
        assertEquals(1, bannedDao2.getBanCount("PersistentBanned"));

        db2.disconnect();
    }
}
