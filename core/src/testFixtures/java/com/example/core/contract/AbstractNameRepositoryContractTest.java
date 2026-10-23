package com.example.core.contract;

import com.example.core.repository.NameRepository;
import com.example.model.Participant;
import com.example.model.SavedName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Contract test suite defining the expected behavior for any {@link NameRepository}
 * implementation across platforms (H2 on Desktop, Room on Android).
 *
 * <p>Exposed as a test fixture from {@code :core} so that each platform module
 * simply extends this class, guaranteeing 100% behavioral parity.
 */
public abstract class AbstractNameRepositoryContractTest {

    protected NameRepository repository;

    /** Factory method providing a fresh, isolated repository instance. */
    protected abstract NameRepository createRepository() throws Exception;

    /** Hook called after each test for cleaning up database resources. */
    protected void cleanupRepository(NameRepository repo) throws Exception {}

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
        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findAllSaved().isEmpty());
        assertTrue(repository.findLastUsedNames().isEmpty());
    }

    @Test
    public void testAddAndFindAll() {
        Participant alice = repository.add("Alice");
        assertNotNull(alice);
        assertEquals("Alice", alice.getName());
        assertTrue(alice.getId() > 0);

        List<Participant> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals("Alice", all.get(0).getName());
        assertEquals(1, repository.count());

        Participant bob = repository.add("Bob");
        assertNotNull(bob);
        assertEquals("Bob", bob.getName());
        assertEquals(2, repository.count());
    }

    @Test
    public void testAddDuplicateNameCaseInsensitive() {
        Participant first = repository.add("Charlie");
        assertEquals("Charlie", first.getName());

        Participant second = repository.add("charlie");
        assertNotNull(second);

        // Catalog maintains unique names case-insensitively
        assertEquals(1, repository.count());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    public void testRemoveById() {
        Participant p = repository.add("David");
        assertEquals(1, repository.count());

        repository.remove(p.getId());
        assertEquals(0, repository.count());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    public void testRemoveByName() {
        repository.add("Eve");
        assertEquals(1, repository.count());

        repository.removeByName("eve");
        assertEquals(0, repository.count());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    public void testFindAllSaved() {
        repository.add("Frank");
        List<SavedName> saved = repository.findAllSaved();
        assertEquals(1, saved.size());
        assertEquals("Frank", saved.get(0).getName());
        assertNotNull(saved.get(0).getLastUsedAt());
    }

    @Test
    public void testRecordUsageAndFindLastUsedNamesWithinTwoMinutes() {
        repository.recordUsage(List.of("Heidi", "Grace"));

        List<String> lastUsed = repository.findLastUsedNames();
        assertEquals(2, lastUsed.size());
        // Ordered alphabetically case-insensitively
        assertEquals("Grace", lastUsed.get(0));
        assertEquals("Heidi", lastUsed.get(1));
    }

    @Test
    public void testClear() {
        repository.add("Ivan");
        repository.add("Judy");
        assertEquals(2, repository.count());

        repository.clear();
        assertEquals(0, repository.count());
        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findAllSaved().isEmpty());
    }
}
