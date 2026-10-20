package com.example.db;

import com.example.model.SavedName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Data-Access Object for the {@code saved_names} table.
 *
 * <p>Manages the persistent catalog of names saved in the database.
 * Every time the roulette wheel is spun, all participants in the wheel are
 * saved to this table: new names are added, and existing names update their
 * {@code last_used_at} timestamp.
 */
public class SavedNamesDao {

    private final Connection conn;

    public SavedNamesDao(Connection conn) {
        this.conn = conn;
    }

    /**
     * Inserts a name into {@code saved_names} if it does not already exist (case-insensitive).
     * If the name already exists, updates its {@code last_used_at} timestamp.
     *
     * @param name the name to save (must not be blank)
     * @return the primary-key id of the saved name
     * @throws SQLException on any JDBC error
     */
    public long insert(String name) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or blank");
        }
        String trimmed = name.trim();

        // Check if name already exists (case-insensitive)
        final String checkSql = "SELECT id FROM saved_names WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setString(1, trimmed);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    // Update last_used_at
                    final String updateSql = "UPDATE saved_names SET last_used_at = CURRENT_TIMESTAMP WHERE id = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setLong(1, id);
                        updatePs.executeUpdate();
                    }
                    return id;
                }
            }
        }

        final String insertSql = "INSERT INTO saved_names (name, last_used_at) VALUES (?, CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trimmed);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new SQLException("INSERT into saved_names returned no generated key");
            }
        }
    }

    /**
     * Records usage of names when the spin button is clicked:
     * - If the name is already saved, updates its {@code last_used_at} to the given timestamp.
     * - If the name is not yet saved, inserts it with {@code last_used_at} set to the given timestamp.
     *
     * @param name the participant name to record
     * @param timestamp the timestamp of usage
     * @throws SQLException on any JDBC error
     */
    public void recordUsage(String name, java.sql.Timestamp timestamp) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String trimmed = name.trim();
        final String updateSql = "UPDATE saved_names SET last_used_at = ? WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setTimestamp(1, timestamp);
            ps.setString(2, trimmed);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                final String insertSql = "INSERT INTO saved_names (name, last_used_at) VALUES (?, ?)";
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                    insertPs.setString(1, trimmed);
                    insertPs.setTimestamp(2, timestamp);
                    insertPs.executeUpdate();
                }
            }
        }
    }

    /**
     * Records usage of a single participant name with the current time.
     *
     * @param name the participant name to record
     * @throws SQLException on any JDBC error
     */
    public void recordUsage(String name) throws SQLException {
        recordUsage(name, new java.sql.Timestamp(System.currentTimeMillis()));
    }

    /**
     * Records usage for a collection of participant names, applying the exact same
     * timestamp to all of them so they form a coherent "last used" batch.
     *
     * @param names the names from the wheel
     * @throws SQLException on any JDBC error
     */
    public void recordUsage(Collection<String> names) throws SQLException {
        if (names == null || names.isEmpty()) {
            return;
        }
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        for (String name : names) {
            recordUsage(name, now);
        }
    }

    /**
     * Returns the names that were used on the wheel within a 2-minute interval
     * of the most recent usage.
     *
     * @return list of distinct last used names within the 2-minute interval,
     *         ordered alphabetically (case-insensitive); never {@code null}
     * @throws SQLException on any JDBC error
     */
    public List<String> findLastUsedNames() throws SQLException {
        final String sql = """
                SELECT name FROM saved_names
                WHERE last_used_at IS NOT NULL
                  AND last_used_at >= (
                      SELECT DATEADD('MINUTE', -2, MAX(last_used_at))
                      FROM saved_names
                      WHERE last_used_at IS NOT NULL
                  )
                ORDER BY LOWER(name) ASC, id ASC
                """;
        List<String> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(rs.getString("name"));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns all saved names ordered alphabetically (case-insensitive).
     *
     * @return an unmodifiable list of saved names; never {@code null}
     * @throws SQLException on any JDBC error
     */
    public List<SavedName> findAll() throws SQLException {
        final String sql = "SELECT id, name, last_used_at FROM saved_names ORDER BY LOWER(name) ASC, id ASC";
        List<SavedName> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new SavedName(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getTimestamp("last_used_at").toLocalDateTime()));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Finds a saved name by its name (case-insensitive).
     *
     * @param name the name to look up
     * @return the SavedName or null if not found
     * @throws SQLException on any JDBC error
     */
    public SavedName findByName(String name) throws SQLException {
        if (name == null) return null;
        final String sql = "SELECT id, name, last_used_at FROM saved_names WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SavedName(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getTimestamp("last_used_at").toLocalDateTime());
                }
            }
        }
        return null;
    }

    /**
     * Deletes a saved name row by its primary key.
     *
     * @param id the id to delete
     * @throws SQLException on any JDBC error
     */
    public void delete(long id) throws SQLException {
        final String sql = "DELETE FROM saved_names WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a saved name row by its name (case-insensitive).
     *
     * @param name the name to delete
     * @throws SQLException on any JDBC error
     */
    public void deleteByName(String name) throws SQLException {
        final String sql = "DELETE FROM saved_names WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.executeUpdate();
        }
    }

    /**
     * Clears all saved names from the database.
     *
     * @throws SQLException on any JDBC error
     */
    public void deleteAll() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM saved_names");
        }
    }

    /**
     * Returns the total count of saved names in the database.
     *
     * @return the number of saved names
     * @throws SQLException on any JDBC error
     */
    public int count() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM saved_names";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
}
