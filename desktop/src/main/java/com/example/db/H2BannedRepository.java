package com.example.db;

import com.example.core.repository.BannedRepository;
import com.example.model.BannedName;
import com.example.model.Participant;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

/**
 * H2 implementation of {@link BannedRepository} for the desktop application.
 *
 * <p>Backed by the {@code banned_names} table via {@link BannedDao}.
 * Guarantees that recording a ban is executed atomically inside a database transaction.
 */
public class H2BannedRepository implements BannedRepository {

    private final Connection connection;
    private final BannedDao dao;

    public H2BannedRepository(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        this.dao = new BannedDao(connection);
    }

    public BannedDao getDao() {
        return dao;
    }

    @Override
    public List<BannedName> findAll() {
        try {
            return dao.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll banned names from H2", e);
        }
    }

    @Override
    public BannedName ban(Participant participant) {
        Objects.requireNonNull(participant, "participant must not be null");
        return ban(participant.getName());
    }

    @Override
    public BannedName ban(String name) {
        Objects.requireNonNull(name, "name must not be null");
        boolean prevAutoCommit = true;
        try {
            prevAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            BannedName result = dao.recordBan(name);

            connection.commit();
            return result;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw new RuntimeException("Failed to record ban atomically for " + name, e);
        } finally {
            try {
                connection.setAutoCommit(prevAutoCommit);
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public void restore(long id) {
        try {
            dao.delete(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to restore banned name id=" + id, e);
        }
    }

    @Override
    public BannedName findByName(String name) {
        try {
            return dao.findByName(name);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to findByName in banned_names: " + name, e);
        }
    }

    @Override
    public int getBanCount(String name) {
        try {
            return dao.getBanCount(name);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to getBanCount for: " + name, e);
        }
    }

    @Override
    public int count() {
        try {
            return dao.count();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count banned names in H2", e);
        }
    }

    @Override
    public int totalBans() {
        try {
            return dao.totalBans();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to calculate totalBans in H2", e);
        }
    }

    @Override
    public void clear() {
        try {
            dao.deleteAll();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to deleteAll banned names in H2", e);
        }
    }
}
