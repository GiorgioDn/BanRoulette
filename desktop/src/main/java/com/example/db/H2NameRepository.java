package com.example.db;

import com.example.core.repository.NameRepository;
import com.example.model.Participant;
import com.example.model.SavedName;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * H2 implementation of {@link NameRepository} for the desktop application.
 *
 * <p>Backed by the {@code saved_names} table via {@link SavedNamesDao}.
 */
public class H2NameRepository implements NameRepository {

    private final Connection connection;
    private final SavedNamesDao dao;

    public H2NameRepository(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection must not be null");
        this.dao = new SavedNamesDao(connection);
    }

    public SavedNamesDao getDao() {
        return dao;
    }

    @Override
    public List<Participant> findAll() {
        try {
            return dao.findAll().stream()
                    .map(sn -> new Participant(sn.getId(), sn.getName(), sn.getLastUsedAt()))
                    .toList();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll participants from H2", e);
        }
    }

    @Override
    public Participant add(String name) {
        try {
            long id = dao.insert(name);
            return new Participant(id, name);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add participant to H2: " + name, e);
        }
    }

    @Override
    public void remove(long id) {
        try {
            dao.delete(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove saved name id=" + id + " from H2", e);
        }
    }

    @Override
    public void removeByName(String name) {
        try {
            dao.deleteByName(name);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove saved name=" + name + " from H2", e);
        }
    }

    @Override
    public List<SavedName> findAllSaved() {
        try {
            return dao.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAllSaved from H2", e);
        }
    }

    @Override
    public void recordUsage(Collection<String> names) {
        try {
            dao.recordUsage(names);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to recordUsage in H2", e);
        }
    }

    @Override
    public List<String> findLastUsedNames() {
        try {
            return dao.findLastUsedNames();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to findLastUsedNames from H2", e);
        }
    }

    @Override
    public int count() {
        try {
            return dao.count();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count saved names in H2", e);
        }
    }

    @Override
    public void clear() {
        try {
            dao.deleteAll();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to deleteAll saved names in H2", e);
        }
    }
}
