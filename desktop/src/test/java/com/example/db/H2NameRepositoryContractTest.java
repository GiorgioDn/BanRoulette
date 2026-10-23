package com.example.db;

import com.example.core.contract.AbstractNameRepositoryContractTest;
import com.example.core.repository.NameRepository;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Desktop contract test verifying that {@link H2NameRepository} satisfies
 * {@link AbstractNameRepositoryContractTest}.
 */
public class H2NameRepositoryContractTest extends AbstractNameRepositoryContractTest {

    private DatabaseManager dbManager;

    @Override
    protected NameRepository createRepository() throws SQLException {
        dbManager = new DatabaseManager();
        String memUrl = "jdbc:h2:mem:contract_name_" + UUID.randomUUID().toString().replace("-", "") + ";DB_CLOSE_DELAY=-1";
        dbManager.connect(memUrl);
        return new H2NameRepository(dbManager.getConnection());
    }

    @Override
    protected void cleanupRepository(NameRepository repo) {
        if (dbManager != null) {
            dbManager.disconnect();
        }
    }
}
