package com.example.db;

import com.example.anim.SpinAnimation;
import com.example.model.BannedName;
import com.example.model.Participant;
import com.example.svg.WheelGenerator;
import javafx.scene.Group;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lifecycle tests for the ban tracking system:
 * - A name in banned_names does NOT delete it from names or block it from being reinserted.
 * - Stores how many times each name was chosen for a ban.
 * - Names appear in the ban table only after at least one ban.
 * - Names and ban counts persist cleanly across DB disconnect and reopen.
 */
class SpinBanLifecycleTest {

    @Test
    @DisplayName("Lifecycle: spin, record ban count, names not deleted or blocked, count persisted across reopen, reset")
    void testSpinBanCountLifecycle() throws SQLException, IOException {
        Path tempDir = Files.createTempDirectory("ban_roulette_test_");
        Path dbFile = tempDir.resolve("test_roulette");
        String dbUrl = "jdbc:h2:" + dbFile.toAbsolutePath();

        WheelGenerator wheelGenerator = new WheelGenerator();

        try {
            // ==========================================
            // SESSION 1: Startup -> Add names -> Spin -> Ban (x2) -> Close
            // ==========================================
            DatabaseManager dbSession1 = new DatabaseManager();
            dbSession1.connect(dbUrl);

            NameDao nameDao1 = new NameDao(dbSession1.getConnection());
            BannedDao bannedDao1 = new BannedDao(dbSession1.getConnection());

            // 1. Initialise participants
            long idAlice = nameDao1.insert("Alice");
            long idBob = nameDao1.insert("Bob");
            long idCharlie = nameDao1.insert("Charlie");

            List<Participant> active1 = nameDao1.findAll();
            assertEquals(3, active1.size());

            // Requirement: "insert the name in the ban table only with atleast one ban"
            assertEquals(0, bannedDao1.count(), "Ban table must be empty before any bans");
            assertEquals(0, bannedDao1.getBanCount("Bob"));

            // 2. Wheel generated with all 3 names
            Group wheel1 = wheelGenerator.generateWheel(active1, 200.0);
            assertNotNull(wheel1);

            // 3. Pre-select Bob for a ban
            nameDao1.ban(idBob, bannedDao1);

            // Requirement: "doesn't block the name to being reinserted or delete it from the list of name used"
            List<Participant> afterBan1 = nameDao1.findAll();
            assertEquals(3, afterBan1.size(), "Banned name must NOT be deleted from names table");
            assertTrue(afterBan1.stream().anyMatch(p -> p.getName().equals("Bob")));

            // Requirement: "stored how many times the name was choosen by the roulet for the ban"
            assertEquals(1, bannedDao1.count());
            assertEquals(1, bannedDao1.getBanCount("Bob"));

            // 4. Second ban for Bob
            nameDao1.ban(idBob, bannedDao1);
            assertEquals(3, nameDao1.findAll().size(), "Name still present after second ban");
            assertEquals(1, bannedDao1.count(), "Still 1 distinct banned name");
            assertEquals(2, bannedDao1.getBanCount("Bob"), "Ban count should increment to 2");

            // Requirement: "doesn't block the name to being reinserted"
            assertDoesNotThrow(() -> nameDao1.insert("Bob"),
                    "Reinserting a banned name must not be blocked");
            assertEquals(4, nameDao1.findAll().size());

            // 5. App closes cleanly
            dbSession1.disconnect();

            // ==========================================
            // SESSION 2: Reopen App -> Load DB -> Check State
            // ==========================================
            DatabaseManager dbSession2 = new DatabaseManager();
            dbSession2.connect(dbUrl);

            NameDao nameDao2 = new NameDao(dbSession2.getConnection());
            BannedDao bannedDao2 = new BannedDao(dbSession2.getConnection());

            // Load saved names and banned history
            List<Participant> loadedParticipants = nameDao2.findAll();
            List<BannedName> loadedBanned = bannedDao2.findAll();

            assertEquals(4, loadedParticipants.size());
            assertEquals(1, loadedBanned.size());
            assertEquals("Bob", loadedBanned.get(0).getName());
            assertEquals(2, loadedBanned.get(0).getBanCount(), "Ban count must persist as 2 across reopen");

            // Wheel generates with loaded participants
            Group wheel2 = wheelGenerator.generateWheel(loadedParticipants, 200.0);
            assertNotNull(wheel2);

            // Ban Alice
            long idAlice2 = loadedParticipants.stream()
                    .filter(p -> p.getName().equals("Alice"))
                    .findFirst().orElseThrow().getId();
            nameDao2.ban(idAlice2, bannedDao2);

            assertEquals(2, bannedDao2.count());
            assertEquals(1, bannedDao2.getBanCount("Alice"));
            assertEquals(3, bannedDao2.totalBans(), "Total bans should be Bob(2) + Alice(1) = 3");

            // Verify order: highest ban count first
            List<BannedName> ordered = bannedDao2.findAll();
            assertEquals("Bob", ordered.get(0).getName(), "Bob has 2 bans so should be first");
            assertEquals(2, ordered.get(0).getBanCount());
            assertEquals("Alice", ordered.get(1).getName());
            assertEquals(1, ordered.get(1).getBanCount());

            // ==========================================
            // SESSION 3: Reset ban for Bob
            // ==========================================
            dbSession2.disconnect();

            DatabaseManager dbSession3 = new DatabaseManager();
            dbSession3.connect(dbUrl);

            NameDao nameDao3 = new NameDao(dbSession3.getConnection());
            BannedDao bannedDao3 = new BannedDao(dbSession3.getConnection());

            BannedName bobBan = bannedDao3.findByName("Bob");
            assertNotNull(bobBan);
            bannedDao3.delete(bobBan.getId());

            assertEquals(0, bannedDao3.getBanCount("Bob"), "Bob ban count reset to 0");
            assertEquals(1, bannedDao3.count(), "Only Alice remains in ban table");

            // Active names table is untouched
            assertEquals(4, nameDao3.findAll().size());

            dbSession3.disconnect();

        } finally {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {}
                        });
            }
        }
    }
}
