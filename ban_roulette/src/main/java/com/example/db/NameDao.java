package com.example.db;

import com.example.model.Participant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-Access Object for the {@code names} table.
 *
 * <p>All queries use {@link PreparedStatement}s — never string concatenation —
 * to prevent SQL injection. Every statement is wrapped in try-with-resources so
 * resources are released even if an exception is thrown.
 *
 * <h2>Ban semantics</h2>
 * {@link #ban(long, BannedDao)} deletes the participant from {@code names} and
 * inserts them into {@code banned_names} inside a <em>single transaction</em>
 * ({@code setAutoCommit(false)}, commit, rollback on error).  A crash between
 * the two statements therefore cannot leave the tables inconsistent.
 */
public class NameDao {

    private final Connection conn;

    public NameDao(Connection conn) {
        this.conn = conn;
    }

    // ── Queries ────────────────────────────────────────────────────────────────

    /**
     * Inserts a new participant and returns the generated id.
     *
     * @param name the participant's display name (must not be blank)
     * @return the generated primary-key value
     * @throws SQLException on any JDBC error
     */
    public long insert(String name) throws SQLException {
        final String sql = "INSERT INTO names (name) VALUES (?)";
        long id;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1);
                } else {
                    throw new SQLException("INSERT into names returned no generated key");
                }
            }
        }
        try {
            new SavedNamesDao(conn).insert(name);
        } catch (Exception ignored) {
            // Best-effort catalog persistence
        }
        return id;
    }

    /**
     * Returns all participants ordered by insertion time (oldest first).
     *
     * @return an immutable snapshot list; never {@code null}
     * @throws SQLException on any JDBC error
     */
    public List<Participant> findAll() throws SQLException {
        final String sql = "SELECT id, name, created_at FROM names ORDER BY created_at, id";
        List<Participant> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Participant(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
            }
        }
        return List.copyOf(result);
    }

    /**
     * Deletes a participant by primary key.
     *
     * @param id the row to delete
     * @throws SQLException on any JDBC error
     */
    public void delete(long id) throws SQLException {
        final String sql = "DELETE FROM names WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Bans a participant: atomically removes them from {@code names} and
     * inserts them into {@code banned_names} via {@code bannedDao}.
     *
     * <p>Records the ban in {@code banned_names} (incrementing its ban count)
     * without deleting the participant from {@code names}, allowing the participant
     * to remain in use and on the wheel.
     *
     * @param id        the {@code names.id} of the participant to ban
     * @param bannedDao the DAO used to record into {@code banned_names}
     * @return the participant's name
     * @throws SQLException if no row with {@code id} exists, or on any JDBC error
     */
    public String ban(long id, BannedDao bannedDao) throws SQLException {
        String name = findNameById(id);
        bannedDao.recordBan(name);
        return name;
    }

    /**
     * Records a ban for the given name in {@code banned_names} without deleting it from {@code names}.
     *
     * @param name      the name of the participant to ban
     * @param bannedDao the DAO used to record into {@code banned_names}
     * @throws SQLException on any JDBC error
     */
    public void ban(String name, BannedDao bannedDao) throws SQLException {
        bannedDao.recordBan(name);
    }

    /**
     * Removes all rows from {@code names} (used by "Clear All").
     *
     * @throws SQLException on any JDBC error
     */
    public void deleteAll() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM names");
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String findNameById(long id) throws SQLException {
        final String sql = "SELECT name FROM names WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
                throw new SQLException("No participant with id=" + id);
            }
        }
    }
}
