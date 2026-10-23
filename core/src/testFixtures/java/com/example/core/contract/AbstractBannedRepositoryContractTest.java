package com.example.core.contract;

import com.example.core.repository.BannedRepository;
import com.example.model.BannedName;
import com.example.model.Participant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Contract test suite defining the expected behavior for any {@link BannedRepository}
 * implementation across platforms (H2 on Desktop, Room on Android).
 *
 * <p>Exposed as a test fixture from {@code :core} so that each platform module
 * simply extends this class, guaranteeing 100% behavioral parity.
 */
public abstract class AbstractBannedRepositoryContractTest {

    protected BannedRepository repository;

    /** Factory method providing a fresh, isolated repository instance. */
    protected abstract BannedRepository createRepository() throws Exception;

    /** Hook called after each test for cleaning up database resources. */
    protected void cleanupRepository(BannedRepository repo) throws Exception {}

    @Before
    public void setUpContractTest() throws Exception {
        repository = createRepository();
        repository.clear();
    }

    @After
    public void tearDownContractTest() throws Exception {
        if (repository != null) {
            cleanupRepository(repository);
        }
    }

    @Test
    public void testEmptyInitially() {
        assertEquals(0, repository.count());
        assertEquals(0, repository.totalBans());
        assertTrue(repository.findAll().isEmpty());
        assertNull(repository.findByName("Nobody"));
        assertEquals(0, repository.getBanCount("Nobody"));
    }

    @Test
    public void testFirstBanRecordsCountOneAndTimestamp() {
        BannedName banned = repository.ban("Alice");
        assertNotNull(banned);
        assertEquals("Alice", banned.getName());
        assertEquals(1, banned.getBanCount());
        assertNotNull(banned.getBannedAt());

        assertEquals(1, repository.getBanCount("Alice"));
        assertEquals(1, repository.getBanCount("alice")); // Case-insensitive lookup
        assertEquals(1, repository.count());
        assertEquals(1, repository.totalBans());

        BannedName found = repository.findByName("Alice");
        assertNotNull(found);
        assertEquals(1, found.getBanCount());
    }

    @Test
    public void testSecondBanIncrementsCount() {
        repository.ban("Alice");
        BannedName second = repository.ban("Alice");

        assertEquals(2, second.getBanCount());
        assertEquals(2, repository.getBanCount("Alice"));
        assertEquals(1, repository.count());
        assertEquals(2, repository.totalBans());
    }

    @Test
    public void testMultipleBannedNamesOrdering() {
        repository.ban("Bob");
        repository.ban("Alice");
        repository.ban("Bob");

        assertEquals(2, repository.count());
        assertEquals(3, repository.totalBans());

        List<BannedName> list = repository.findAll();
        assertEquals(2, list.size());
        // Ordered by ban count descending: Bob (2) first, Alice (1) second
        assertEquals("Bob", list.get(0).getName());
        assertEquals(2, list.get(0).getBanCount());
        assertEquals("Alice", list.get(1).getName());
        assertEquals(1, list.get(1).getBanCount());
    }

    @Test
    public void testBanParticipantOverload() {
        Participant p = new Participant(42L, "Charlie");
        BannedName b = repository.ban(p);
        assertNotNull(b);
        assertEquals("Charlie", b.getName());
        assertEquals(1, b.getBanCount());
    }

    @Test
    public void testRestoreById() {
        BannedName banned = repository.ban("David");
        assertEquals(1, repository.count());

        repository.restore(banned.getId());
        assertEquals(0, repository.count());
        assertEquals(0, repository.getBanCount("David"));
        assertNull(repository.findByName("David"));
    }

    @Test
    public void testClear() {
        repository.ban("Eve");
        repository.ban("Frank");
        assertEquals(2, repository.count());

        repository.clear();
        assertEquals(0, repository.count());
        assertEquals(0, repository.totalBans());
        assertTrue(repository.findAll().isEmpty());
    }
}
