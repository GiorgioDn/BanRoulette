package com.example.db;

import com.example.core.repository.BannedRepository;
import com.example.core.repository.NameRepository;
import com.example.core.service.BanService;

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
 * <h2>Database Schema</h2>
 * <ul>
 *   <li><b>saved_names</b> — Persistent catalog of distinct names saved in the database
 *       along with their {@code last_used_at} timestamp. When the wheel is spun, all
 *       participant names in the wheel are added or updated in this table.</li>
 *   <li><b>banned_names</b> — Stores distinct participants who have received at least one ban,
 *       along with their cumulative {@code ban_count} and {@code banned_at} timestamp.</li>
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

    private static final String DDL_SAVED_NAMES = """
            CREATE TABLE IF NOT EXISTS saved_names (
                id           IDENTITY PRIMARY KEY,
                name         VARCHAR(100) NOT NULL UNIQUE,
                last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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

    // ── State ──────────────────────────────────────────────────────────────────

    private Connection connection;

    private Thread shutdownHook;

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

        if (shutdownHook == null && !jdbcUrl.contains(":mem:")) {
            shutdownHook = new Thread(this::disconnect, "db-shutdown-hook");
            try {
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM already shutting down
            }
        }
    }

    /** @return the active JDBC {@link Connection} */
    public Connection getConnection() { return connection; }

    /**
     * Closes the connection cleanly so H2 releases its file lock.
     * Thread-safe and idempotent — safe to call multiple times or from shutdown hooks.
     */
    public synchronized void disconnect() {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // JVM already shutting down
            }
            shutdownHook = null;
        }

        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    LOG.info("Database disconnected.");
                }
            } catch (SQLException ex) {
                LOG.log(Level.WARNING, "Error closing database connection", ex);
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Returns an H2-backed {@link NameRepository} for participant and catalog persistence.
     */
    public NameRepository getNameRepository() {
        if (connection == null) {
            throw new IllegalStateException("Database is not connected");
        }
        return new H2NameRepository(connection);
    }

    /**
     * Returns an H2-backed {@link BannedRepository} with atomic transactions.
     */
    public BannedRepository getBannedRepository() {
        if (connection == null) {
            throw new IllegalStateException("Database is not connected");
        }
        return new H2BannedRepository(connection);
    }

    /**
     * Returns a {@link BanService} configured with the desktop H2 repositories.
     */
    public BanService getBanService() {
        return new BanService(getNameRepository(), getBannedRepository());
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Runs idempotent DDL to create the two application tables.
     * Called once per {@link #connect()} invocation.
     */
    private void initSchema() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute(DDL_SAVED_NAMES);
            st.execute(DDL_BANNED);

            // Migrations: ensure columns exist for existing databases
            st.execute("ALTER TABLE banned_names ADD COLUMN IF NOT EXISTS ban_count INT NOT NULL DEFAULT 1");
            st.execute("ALTER TABLE saved_names ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            // One-time migration from legacy 'names' table if it existed in previous version
            try {
                st.execute("INSERT INTO saved_names (name, last_used_at) " +
                           "SELECT DISTINCT name, created_at FROM names " +
                           "WHERE LOWER(name) NOT IN (SELECT LOWER(name) FROM saved_names)");
                st.execute("DROP TABLE IF EXISTS names");
            } catch (SQLException ignored) {
                // Table 'names' did not exist, nothing to migrate
            }
        }
    }
}
