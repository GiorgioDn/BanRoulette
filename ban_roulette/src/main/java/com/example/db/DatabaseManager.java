package com.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
 * <p>On startup, a schema initialiser creates both tables if they do not
 * exist yet. On shutdown ({@code Application.stop()}), the connection is
 * closed cleanly so H2 releases its file lock.
 *
 * <p><b>Fully implemented in Step 3.</b>
 */
public class DatabaseManager {

    static final String DB_DIR  =
            System.getProperty("user.home") + "/.banroulette";
    static final String DB_PATH = DB_DIR + "/data";
    static final String JDBC_URL =
            "jdbc:h2:file:" + DB_PATH + ";AUTO_SERVER=FALSE";

    private Connection connection;

    /**
     * Opens (or creates) the H2 database and runs the schema initialiser.
     * Safe to call multiple times — {@code CREATE TABLE IF NOT EXISTS} is
     * idempotent and never wipes existing data.
     *
     * @throws SQLException if the connection or DDL fails
     */
    public void connect() throws SQLException {
        // TODO: implement in Step 3
        //  - create DB_DIR if it does not exist
        //  - DriverManager.getConnection(JDBC_URL)
        //  - run schema initialiser (CREATE TABLE IF NOT EXISTS …)
    }

    /** @return the active JDBC connection */
    public Connection getConnection() { return connection; }

    /**
     * Closes the connection cleanly.
     * Call from {@link javafx.application.Application#stop()}.
     */
    public void disconnect() {
        // TODO: implement in Step 3
    }
}
