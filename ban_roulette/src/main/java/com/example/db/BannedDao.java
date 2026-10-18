package com.example.db;

import com.example.model.BannedName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-Access Object for the {@code banned_names} table.
 *
 * <p>Rows are inserted by {@link NameDao#ban} inside a transaction — this DAO
 * never writes outside that context unless called directly (e.g. from tests).
 * All queries use {@link PreparedStatement}s and try-with-resources.
 */
public class BannedDao {

    private final Connection conn;

    public BannedDao(Connection conn) {
        this.conn = conn;
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    /**
     * Inserts a new banned name record.
     *
     * <p>Normally called by {@link NameDao#ban} inside a transaction.
     * The {@code banned_at} timestamp is set automatically by the DB default.
     *
     * @param name the name that was just banned
     * @return the generated primary-key value
     * @throws SQLException on any JDBC error
     */
    public long insert(String name) throws SQLException {
        final String sql = "INSERT INTO banned_names (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("INSERT into banned_names returned no generated key");
            }
        }
    }

    /**
     * Returns all banned names ordered by ban time (most recent first).
     *
     * @return an immutable snapshot list; never {@code null}
     * @throws SQLException on any JDBC error
     */
    public List<BannedName> findAll() throws SQLException {
        final String sql =
                "SELECT id, name, banned_at FROM banned_names ORDER BY banned_at DESC, id DESC";
        List<BannedName> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new BannedName(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getTimestamp("banned_at").toLocalDateTime()));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Restores a banned name: deletes the row from {@code banned_names}.
     * The caller is responsible for re-inserting it into {@code names} if needed.
     *
     * @param id the {@code banned_names.id} to delete
     * @throws SQLException on any JDBC error
     */
    public void delete(long id) throws SQLException {
        final String sql = "DELETE FROM banned_names WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Finds the name string for a given banned row ID.
     *
     * @param id the primary key of the banned name
     * @return the name string
     * @throws SQLException if no row exists with the given ID
     */
    public String findNameById(long id) throws SQLException {
        final String sql = "SELECT name FROM banned_names WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
                throw new SQLException("No banned name with id=" + id);
            }
        }
    }

    /**
     * Atomically restores a banned name: removes it from {@code banned_names}
     * and re-inserts it into {@code names} in a single transaction.
     *
     * @param id the {@code banned_names.id} to restore
     * @param nameDao the DAO used to re-insert into {@code names}
     * @return a restored {@link com.example.model.Participant} with its newly generated ID
     * @throws SQLException on any JDBC error
     */
    public com.example.model.Participant restore(long id, NameDao nameDao) throws SQLException {
        String name = findNameById(id);
        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            delete(id);
            long newId = nameDao.insert(name);
            conn.commit();
            return new com.example.model.Participant(newId, name);
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    /**
     * Returns the total number of banned names.
     *
     * @throws SQLException on any JDBC error
     */
    public int count() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM banned_names";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Removes all rows from {@code banned_names}.
     *
     * @throws SQLException on any JDBC error
     */
    public void deleteAll() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM banned_names");
        }
    }
}
