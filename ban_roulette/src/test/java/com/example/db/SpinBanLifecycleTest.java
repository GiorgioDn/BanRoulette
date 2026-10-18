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
 * Step 7 acceptance criterion test:
 * "You can spin, ban, close the app, reopen it, and see the banned name in
 * the history and missing from the wheel."
 */
class SpinBanLifecycleTest {

    @Test
    @DisplayName("Step 7 Lifecycle: spin, ban, disconnect DB, reopen DB, verify banned in history & missing from wheel, and restore")
    void testSpinBanCloseReopenLifecycle() throws SQLException, IOException {
        // Use a persistent file-based H2 database in a temporary directory
        Path tempDir = Files.createTempDirectory("ban_roulette_test_");
        Path dbFile = tempDir.resolve("test_roulette");
        String dbUrl = "jdbc:h2:" + dbFile.toAbsolutePath();

        WheelGenerator wheelGenerator = new WheelGenerator();

        try {
            // ==========================================
            // SESSION 1: Startup -> Add names -> Spin -> Ban -> Close
            // ==========================================
            DatabaseManager dbSession1 = new DatabaseManager();
            dbSession1.connect(dbUrl);

            NameDao nameDao1 = new NameDao(dbSession1.getConnection());
            BannedDao bannedDao1 = new BannedDao(dbSession1.getConnection());

            // 1. Initialise and populate initial participants
            long idAlice = nameDao1.insert("Alice");
            long idBob = nameDao1.insert("Bob");
            long idCharlie = nameDao1.insert("Charlie");

            List<Participant> activeParticipants1 = nameDao1.findAll();
            assertEquals(3, activeParticipants1.size());
            assertEquals(0, bannedDao1.count());

            // 2. Wheel can be generated with 3 slices
            Group wheel1 = wheelGenerator.generateWheel(activeParticipants1, 200.0);
            assertNotNull(wheel1);

            // 3. Pre-select winner fairly (simulate spin)
            SecureRandom random = new SecureRandom();
            int winnerIdx = SpinAnimation.pickRandomWinner(activeParticipants1.size(), random);
            Participant winner = activeParticipants1.get(winnerIdx);
            assertNotNull(winner);

            // 4. Atomic ban in single transaction
            String bannedName = nameDao1.ban(winner.getId(), bannedDao1);
            assertEquals(winner.getName(), bannedName);

            // Verify active list is now 2, banned is 1
            assertEquals(2, nameDao1.findAll().size());
            assertEquals(1, bannedDao1.count());

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

            // Acceptance criterion:
            // "see the banned name in the history and missing from the wheel"
            assertEquals(1, loadedBanned.size());
            assertEquals(winner.getName(), loadedBanned.get(0).getName());

            assertEquals(2, loadedParticipants.size());
            boolean winnerPresentInActive = loadedParticipants.stream()
                    .anyMatch(p -> p.getName().equals(winner.getName()));
            assertFalse(winnerPresentInActive, "Banned name must be missing from active participants");

            // Wheel is generated with remaining 2 participants (banned name is missing)
            Group wheel2 = wheelGenerator.generateWheel(loadedParticipants, 200.0);
            assertNotNull(wheel2);

            // ==========================================
            // Secondary Ban: down to 1 participant (< 2 disables spin)
            // ==========================================
            Participant secondWinner = loadedParticipants.get(0);
            nameDao2.ban(secondWinner.getId(), bannedDao2);

            List<Participant> singleParticipantList = nameDao2.findAll();
            assertEquals(1, singleParticipantList.size(), "Only 1 participant should remain");
            assertEquals(2, bannedDao2.count());

            // Wheel requires >= 2 participants
            assertTrue(singleParticipantList.size() < 2,
                    "With < 2 participants, spinning is disabled");

            // ==========================================
            // SESSION 3: Reopen & Restore Participant
            // ==========================================
            dbSession2.disconnect();

            DatabaseManager dbSession3 = new DatabaseManager();
            dbSession3.connect(dbUrl);

            NameDao nameDao3 = new NameDao(dbSession3.getConnection());
            BannedDao bannedDao3 = new BannedDao(dbSession3.getConnection());

            List<BannedName> history = bannedDao3.findAll();
            assertEquals(2, history.size());

            // Restore the first banned participant
            BannedName toRestore = history.get(0);
            Participant restored = bannedDao3.restore(toRestore.getId(), nameDao3);
            assertNotNull(restored);
            assertEquals(toRestore.getName(), restored.getName());

            // Now 2 participants are active again -> spin re-enabled
            List<Participant> restoredActive = nameDao3.findAll();
            assertEquals(2, restoredActive.size());
            assertEquals(1, bannedDao3.count());

            Group wheel3 = wheelGenerator.generateWheel(restoredActive, 200.0);
            assertNotNull(wheel3);

            dbSession3.disconnect();

        } finally {
            // Clean up temporary database files
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
