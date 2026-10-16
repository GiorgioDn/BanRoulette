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
}
