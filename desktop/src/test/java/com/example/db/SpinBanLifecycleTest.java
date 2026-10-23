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

            SavedNamesDao savedNamesDao1 = new SavedNamesDao(dbSession1.getConnection());
            BannedDao bannedDao1 = new BannedDao(dbSession1.getConnection());

            // 1. Initialise participants in memory (including duplicate Bob)
            List<Participant> activeParticipants = List.of(
                    new Participant("Alice"),
                    new Participant("Bob"),
                    new Participant("Charlie"),
                    new Participant("Bob") // Can be inserted multiple times in participants
            );

            // Database should be empty before spinning
            assertEquals(0, savedNamesDao1.count(), "saved_names must be empty before spin");
            assertEquals(0, bannedDao1.count(), "banned_names must be empty before any bans");

            // 2. Wheel generated with all names
            Group wheel1 = wheelGenerator.generateWheel(activeParticipants, 200.0);
            assertNotNull(wheel1);

            // 3. User clicks Spin -> records usage of all names in the wheel to saved_names
            List<String> namesToSave = activeParticipants.stream()
                    .map(Participant::getName)
                    .distinct()
                    .toList();
            savedNamesDao1.recordUsage(namesToSave);

            assertEquals(3, savedNamesDao1.count(), "saved_names should have exactly 3 distinct names");
            assertNotNull(savedNamesDao1.findByName("Alice").getLastUsedAt());
            assertNotNull(savedNamesDao1.findByName("Bob").getLastUsedAt());
            assertNotNull(savedNamesDao1.findByName("Charlie").getLastUsedAt());

            // 4. Bob is selected for a ban (first ban)
            bannedDao1.recordBan("Bob");
            assertEquals(1, bannedDao1.count());
            assertEquals(1, bannedDao1.getBanCount("Bob"));

            // 5. Second ban for Bob
            bannedDao1.recordBan("Bob");
            assertEquals(1, bannedDao1.count(), "Still 1 distinct banned name");
            assertEquals(2, bannedDao1.getBanCount("Bob"), "Ban count should increment to 2");

            // 6. App closes cleanly
            dbSession1.disconnect();

            // ==========================================
            // SESSION 2: Reopen App -> Load DB -> Check State
            // ==========================================
            DatabaseManager dbSession2 = new DatabaseManager();
            dbSession2.connect(dbUrl);

            SavedNamesDao savedNamesDao2 = new SavedNamesDao(dbSession2.getConnection());
            BannedDao bannedDao2 = new BannedDao(dbSession2.getConnection());

            assertEquals(3, savedNamesDao2.count());
            assertEquals(1, bannedDao2.count());
            assertEquals("Bob", bannedDao2.findAll().get(0).getName());
            assertEquals(2, bannedDao2.findAll().get(0).getBanCount(), "Ban count must persist as 2 across reopen");

            // Ban Alice
            bannedDao2.recordBan("Alice");
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
            // SESSION 3: Remove ban for Bob (Action button)
            // ==========================================
            dbSession2.disconnect();

            DatabaseManager dbSession3 = new DatabaseManager();
            dbSession3.connect(dbUrl);

            BannedDao bannedDao3 = new BannedDao(dbSession3.getConnection());
            SavedNamesDao savedNamesDao3 = new SavedNamesDao(dbSession3.getConnection());

            BannedName bobBan = bannedDao3.findByName("Bob");
            assertNotNull(bobBan);
            bannedDao3.delete(bobBan.getId());

            assertEquals(0, bannedDao3.getBanCount("Bob"), "Bob ban count reset to 0");
            assertEquals(1, bannedDao3.count(), "Only Alice remains in ban table");

            // Saved names catalog is untouched
            assertEquals(3, savedNamesDao3.count());

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
