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
 * <p>Stores each participant chosen for a ban with their cumulative ban count.
 * A name is inserted only when it has received at least one ban.
 * Subsequent bans increment {@code ban_count} and update {@code banned_at}.
 * All queries use {@link PreparedStatement}s and try-with-resources.
 */
public class BannedDao {

    private final Connection conn;

    public BannedDao(Connection conn) {
        this.conn = conn;
    }

    // ── Queries & Mutations ───────────────────────────────────────────────────

    /**
     * Records a ban for the given name.
     * If the name already exists in {@code banned_names}, increments its {@code ban_count}
     * and updates {@code banned_at}. If it does not exist, inserts it with {@code ban_count = 1}.
     *
     * @param name the name chosen by the roulette for the ban
     * @return the updated or newly created {@link BannedName} record
     * @throws SQLException on any JDBC error
     */
    public BannedName recordBan(String name) throws SQLException {
        final String updateSql = """
                UPDATE banned_names
                SET ban_count = ban_count + 1,
                    banned_at = CURRENT_TIMESTAMP
                WHERE LOWER(name) = LOWER(?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, name);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                final String insertSql = """
                        INSERT INTO banned_names (name, ban_count, banned_at)
                        VALUES (?, 1, CURRENT_TIMESTAMP)
                        """;
                try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertPs.setString(1, name);
                    insertPs.executeUpdate();
                }
            }
        }
        try {
            new SavedNamesDao(conn).insert(name);
        } catch (Exception ignored) {
            // Best-effort catalog persistence
        }
        return findByName(name);
    }

    /**
     * Inserts or increments a banned name record, returning its generated or existing primary key.
     *
     * @param name the name that was just banned
     * @return the primary-key value in {@code banned_names}
     * @throws SQLException on any JDBC error
     */
    public long insert(String name) throws SQLException {
        BannedName bn = recordBan(name);
        return (bn != null) ? bn.getId() : 0L;
    }

    /**
     * Returns all banned names ordered by ban count (highest first), then by recency.
     *
     * @return an immutable snapshot list; never {@code null}
     * @throws SQLException on any JDBC error
     */
    public List<BannedName> findAll() throws SQLException {
        final String sql = """
                SELECT id, name, ban_count, banned_at
                FROM banned_names
                ORDER BY ban_count DESC, banned_at DESC, id DESC
                """;
        List<BannedName> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new BannedName(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getInt("ban_count"),
                        rs.getTimestamp("banned_at").toLocalDateTime()));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Looks up a banned name record by name (case-insensitive).
     *
     * @param name the name to look for
     * @return the {@link BannedName} record, or {@code null} if not found
     * @throws SQLException on any JDBC error
     */
    public BannedName findByName(String name) throws SQLException {
        final String sql = """
                SELECT id, name, ban_count, banned_at
                FROM banned_names
                WHERE LOWER(name) = LOWER(?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BannedName(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getInt("ban_count"),
                            rs.getTimestamp("banned_at").toLocalDateTime());
                }
                return null;
            }
        }
    }

    /**
     * Returns the current ban count for a name (or 0 if never banned).
     */
    public int getBanCount(String name) throws SQLException {
        BannedName bn = findByName(name);
        return (bn != null) ? bn.getBanCount() : 0;
    }

    /**
     * Deletes the row from {@code banned_names}, resetting its ban count to 0.
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
     * Clears a banned name record from {@code banned_names}.
     *
     * @param id the {@code banned_names.id} to remove
     * @return the name of the removed participant
     * @throws SQLException on any JDBC error
     */
    public String restore(long id) throws SQLException {
        String name = findNameById(id);
        delete(id);
        return name;
    }

    /**
     * Returns the total number of distinct banned names.
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
     * Returns the total number of bans accumulated across all names.
     *
     * @throws SQLException on any JDBC error
     */
    public int totalBans() throws SQLException {
        final String sql = "SELECT COALESCE(SUM(ban_count), 0) FROM banned_names";
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
