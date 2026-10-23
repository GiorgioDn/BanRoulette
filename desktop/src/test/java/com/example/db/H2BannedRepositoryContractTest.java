package com.example.db;

import com.example.core.contract.AbstractBannedRepositoryContractTest;
import com.example.core.repository.BannedRepository;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Desktop contract test verifying that {@link H2BannedRepository} satisfies
 * {@link AbstractBannedRepositoryContractTest}.
 */
public class H2BannedRepositoryContractTest extends AbstractBannedRepositoryContractTest {

    private DatabaseManager dbManager;

    @Override
    protected BannedRepository createRepository() throws SQLException {
        dbManager = new DatabaseManager();
        String memUrl = "jdbc:h2:mem:contract_banned_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        dbManager.connect(memUrl);
        return new H2BannedRepository(dbManager.getConnection());
    }

    @Override
    protected void cleanupRepository(BannedRepository repo) {
        if (dbManager != null) {
            dbManager.disconnect();
        }
    }
}
