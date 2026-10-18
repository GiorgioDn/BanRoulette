package com.example.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the embedded H2 database connection for Ban Roulette.
 *
 * <p>The database file lives in a per-user directory so that installed
 * applications (which often cannot write to their own install path) can
 * always reach it:
 * <pre>
 *   ~/.banroulette/data.mv.db   (H2 appends .mv.db automatically)
 * </pre>
 *
 * <p>On startup, {@link #connect()} creates the directory if needed, opens
 * (or creates) the H2 file database, and runs the schema initialiser.
 * The DDL uses {@code CREATE TABLE IF NOT EXISTS}, so existing data is
 * never wiped — a second startup simply reads back the saved rows.
 *
 * <p>On shutdown, call {@link #disconnect()} from
 * {@link javafx.application.Application#stop()} to release H2's file lock.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li><b>Duplicates allowed</b> — no {@code UNIQUE} constraint on {@code name}.
 *       Real-world groups often have two people with the same first name and
 *       a {@code UNIQUE} constraint would be more annoying than helpful.</li>
 *   <li><b>Ban = move</b> — when a participant is banned, their row is deleted
 *       from {@code names} and inserted into {@code banned_names} in a single
 *       transaction ({@link com.example.db.NameDao#ban}). A crash cannot
 *       leave the two tables inconsistent.</li>
 * </ul>
 */
public class DatabaseManager {

    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());

    // ── Constants ──────────────────────────────────────────────────────────────

    static final String DB_DIR   = System.getProperty("user.home") + "/.banroulette";
    static final String DB_PATH  = DB_DIR + "/data";
    static final String JDBC_URL = "jdbc:h2:file:" + DB_PATH + ";AUTO_SERVER=FALSE";

    /** In-memory URL used by unit tests — keeps data for the lifetime of the JVM. */
    public static final String IN_MEMORY_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

    // ── DDL ────────────────────────────────────────────────────────────────────

    private static final String DDL_NAMES = """
            CREATE TABLE IF NOT EXISTS names (
                id         IDENTITY PRIMARY KEY,
                name       VARCHAR(100) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String DDL_BANNED = """
            CREATE TABLE IF NOT EXISTS banned_names (
                id        IDENTITY PRIMARY KEY,
                name      VARCHAR(100) NOT NULL UNIQUE,
                ban_count INT NOT NULL DEFAULT 1,
                banned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String DDL_SAVED_NAMES = """
            CREATE TABLE IF NOT EXISTS saved_names (
                id         IDENTITY PRIMARY KEY,
                name       VARCHAR(100) NOT NULL UNIQUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    // ── State ──────────────────────────────────────────────────────────────────

    private Connection connection;

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Opens (or creates) the H2 database and runs the schema initialiser.
     * Idempotent — safe to call on every startup; existing data is preserved.
     *
     * @throws SQLException if the connection or DDL execution fails
     */
    public void connect() throws SQLException {
        connect(JDBC_URL);
    }

    /**
     * Opens the database at the given JDBC URL.
     * Used directly by unit tests to supply the in-memory URL.
     *
     * @param jdbcUrl the JDBC connection string
     * @throws SQLException if the connection or DDL execution fails
     */
    public void connect(String jdbcUrl) throws SQLException {
        if (!jdbcUrl.contains(":mem:")) {
            // Create the on-disk directory only for file-based databases
            File dir = new File(DB_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new SQLException("Cannot create database directory: " + DB_DIR);
            }
        }

        connection = DriverManager.getConnection(jdbcUrl, "sa", "");
        initSchema();
        LOG.info("Database connected: " + jdbcUrl);
    }

    /** @return the active JDBC {@link Connection} */
    public Connection getConnection() { return connection; }

    /**
     * Closes the connection cleanly so H2 releases its file lock.
     * Call from {@link javafx.application.Application#stop()}.
     */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                LOG.info("Database disconnected.");
            } catch (SQLException ex) {
                LOG.log(Level.WARNING, "Error closing database connection", ex);
            } finally {
                connection = null;
            }
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Runs idempotent DDL to create the two application tables.
     * Called once per {@link #connect()} invocation.
     */
    private void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(DDL_NAMES);
            st.execute(DDL_BANNED);
            // Schema migration: ensure ban_count exists if table was created in an earlier version
            st.execute("ALTER TABLE banned_names ADD COLUMN IF NOT EXISTS ban_count INT NOT NULL DEFAULT 1");
            st.execute(DDL_SAVED_NAMES);
            // Seed saved_names from existing names and banned_names if any
            st.execute("INSERT INTO saved_names (name) SELECT DISTINCT name FROM names WHERE LOWER(name) NOT IN (SELECT LOWER(name) FROM saved_names)");
            st.execute("INSERT INTO saved_names (name) SELECT DISTINCT name FROM banned_names WHERE LOWER(name) NOT IN (SELECT LOWER(name) FROM saved_names)");
        }
    }
}
