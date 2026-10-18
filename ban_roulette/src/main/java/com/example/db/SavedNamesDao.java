package com.example.db;

import com.example.model.SavedName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-Access Object for the {@code saved_names} table.
 *
 * <p>Manages the persistent catalog of names saved in the database.
 * Every participant added to the roulette is automatically stored in this catalog.
 * Users can view all saved names in the GUI and click any name to re-add it to the roulette.
 */
public class SavedNamesDao {

    private final Connection conn;

    public SavedNamesDao(Connection conn) {
        this.conn = conn;
    }

    /**
     * Inserts a name into {@code saved_names} if it does not already exist (case-insensitive).
     * If the name already exists, returns its existing id without throwing an error.
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
                    return rs.getLong("id");
                }
            }
        }

        final String insertSql = "INSERT INTO saved_names (name) VALUES (?)";
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
     * Returns all saved names ordered alphabetically (case-insensitive).
     *
     * @return an unmodifiable list of saved names; never {@code null}
     * @throws SQLException on any JDBC error
     */
    public List<SavedName> findAll() throws SQLException {
        final String sql = "SELECT id, name, created_at FROM saved_names ORDER BY LOWER(name) ASC, id ASC";
        List<SavedName> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new SavedName(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
            }
        }
        return List.copyOf(result);
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
