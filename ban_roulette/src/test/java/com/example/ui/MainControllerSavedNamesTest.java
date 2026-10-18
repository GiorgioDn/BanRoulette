package com.example.ui;

import com.example.db.BannedDao;
import com.example.db.DatabaseManager;
import com.example.db.SavedNamesDao;
import com.example.model.SavedName;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for right sidebar switching and Saved Names catalog features in {@link MainController}.
 */
class MainControllerSavedNamesTest {

    private DatabaseManager db;
    private MainController controller;
    private BorderPane root;

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        db = new DatabaseManager();
        db.connect(DatabaseManager.IN_MEMORY_URL);
        new SavedNamesDao(db.getConnection()).deleteAll();
        new BannedDao(db.getConnection()).deleteAll();

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/app/main.fxml"));
                root = loader.load();
                controller = loader.getController();
                controller.setDatabaseManager(db);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS), "FXML loading timed out");
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        Thread.sleep(150);
        if (db != null) {
            db.disconnect();
        }
    }

    @Test
    void fxmlNodesInjectedProperly() {
        assertNotNull(controller.getRightSidebar(), "rightSidebar should be injected");
        assertNotNull(controller.getBannedBox(), "bannedBox should be injected");
        assertNotNull(controller.getSavedNamesBox(), "savedNamesBox should be injected");
        assertNotNull(controller.getSavedNamesList(), "savedNamesList should be injected");
        assertNotNull(controller.getToggleSavedNamesBtn(), "toggleSavedNamesBtn should be injected");
        assertNotNull(controller.getToggleHistoryBtn(), "toggleHistoryBtn should be injected");
    }

    @Test
    void rightSidebarTogglingBetweenHistoryAndSavedNames() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // Initial state: Banned history is active
            assertEquals(MainController.RightSidebarView.BANNED, controller.getCurrentRightView());
            assertTrue(controller.getRightSidebar().isVisible());
            assertTrue(controller.getBannedBox().isVisible());
            assertFalse(controller.getSavedNamesBox().isVisible());

            // 1. Click Saved Names button: closes Banned List, opens Saved Names in same place
            controller.onToggleSavedNames();
            assertEquals(MainController.RightSidebarView.SAVED_NAMES, controller.getCurrentRightView());
            assertTrue(controller.getRightSidebar().isVisible());
            assertFalse(controller.getBannedBox().isVisible(), "bannedBox should be closed/hidden");
            assertTrue(controller.getSavedNamesBox().isVisible(), "savedNamesBox should be open/visible");
            assertEquals("Hide Saved", controller.getToggleSavedNamesBtn().getText());
            assertEquals("📜 History", controller.getToggleHistoryBtn().getText());

            // 2. Click Saved Names button again: toggles sidebar closed
            controller.onToggleSavedNames();
            assertEquals(MainController.RightSidebarView.NONE, controller.getCurrentRightView());
            assertFalse(controller.getRightSidebar().isVisible());

            // 3. Click History button: opens Banned list
            controller.onToggleHistory();
            assertEquals(MainController.RightSidebarView.BANNED, controller.getCurrentRightView());
            assertTrue(controller.getRightSidebar().isVisible());
            assertTrue(controller.getBannedBox().isVisible());
            assertFalse(controller.getSavedNamesBox().isVisible());

            // 4. Click Saved Names while History is open: switches immediately in same place
            controller.onToggleSavedNames();
            assertEquals(MainController.RightSidebarView.SAVED_NAMES, controller.getCurrentRightView());
            assertTrue(controller.getRightSidebar().isVisible());
            assertFalse(controller.getBannedBox().isVisible());
            assertTrue(controller.getSavedNamesBox().isVisible());

            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void clickingAddButtonAddsToRouletteParticipants() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // Add a saved name
            controller.getSavedNamesMasterList().add(new SavedName("Giovanni"));
            assertEquals(1, controller.getSavedNamesMasterList().size());
            assertFalse(controller.isParticipantInWheel("Giovanni"));

            // Simulate clicking the "+ Add" button from the saved names list
            controller.onAddParticipantFromSaved("Giovanni");

            // Participant should now be in the wheel
            assertTrue(controller.isParticipantInWheel("Giovanni"), "Giovanni should now be in the wheel");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void deleteSavedNameRemovesFromCatalog() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            SavedName sn = new SavedName(10, "Marco");
            controller.getSavedNamesMasterList().add(sn);
            assertEquals(1, controller.getSavedNamesMasterList().size());

            controller.onDeleteSavedName(sn);
            assertEquals(0, controller.getSavedNamesMasterList().size());
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    void participantListMutationsDoNotCallDb() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // 1. Adding participant does NOT save to DB
            controller.getNameInput().setText("Luna");
            controller.onAddName();
            assertEquals(1, controller.getParticipants().size());

            // 2. Can insert same name multiple times in participants
            controller.getNameInput().setText("Luna");
            controller.onAddName();
            assertEquals(2, controller.getParticipants().size(), "Same name can be added multiple times in participants");

            // 3. Removing does not call DB
            controller.onRemoveParticipant(controller.getParticipants().get(0));
            assertEquals(1, controller.getParticipants().size());

            // 4. Clearing does not call DB
            controller.onClearAll();
            assertEquals(0, controller.getParticipants().size());

            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Sleep briefly to let any rogue background tasks finish
        Thread.sleep(100);

        // Database should be completely empty
        SavedNamesDao savedNamesDao = new SavedNamesDao(db.getConnection());
        assertEquals(0, savedNamesDao.count(), "DB saved_names must not have been called");
        BannedDao bannedDao = new BannedDao(db.getConnection());
        assertEquals(0, bannedDao.count(), "DB banned_names must not have been called");
    }

    @Test
    void spinButtonSavesAllNamesToSavedNamesTable() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // Add participants (including duplicates)
            controller.getNameInput().setText("Alice");
            controller.onAddName();
            controller.getNameInput().setText("Bob");
            controller.onAddName();
            controller.getNameInput().setText("Alice"); // Duplicate
            controller.onAddName();

            assertEquals(3, controller.getParticipants().size());

            // Click spin button
            controller.onSpin();

            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Wait for background persistence
        Thread.sleep(300);

        SavedNamesDao savedNamesDao = new SavedNamesDao(db.getConnection());
        // Duplicate Alice should only be saved ONCE in saved_names
        assertEquals(2, savedNamesDao.count(), "saved_names must contain Alice and Bob exactly once");

        SavedName alice = savedNamesDao.findByName("Alice");
        assertNotNull(alice);
        assertNotNull(alice.getLastUsedAt(), "last_used_at must be populated in DB");

        SavedName bob = savedNamesDao.findByName("Bob");
        assertNotNull(bob);
        assertNotNull(bob.getLastUsedAt(), "last_used_at must be populated in DB");
    }

    @Test
    void removeButtonInBannedTableRemovesFromDb() throws Exception {
        BannedDao bannedDao = new BannedDao(db.getConnection());
        bannedDao.recordBan("Zoe");
        assertEquals(1, bannedDao.count());

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            controller.setDatabaseManager(db);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);

        assertEquals(1, controller.getBannedList().size());

        CountDownLatch removeLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            controller.onRemoveBannedName(controller.getBannedList().get(0));
            removeLatch.countDown();
        });
        assertTrue(removeLatch.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);

        assertEquals(0, controller.getBannedList().size());
        assertEquals(0, bannedDao.count(), "Zoe must be removed from banned_names table");
    }

    @Test
    void savedNamesCellDisplaysOnlyNameAndAddButtonAndXButton() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();

        Platform.runLater(() -> {
            try {
                // Get the CellFactory from savedNamesList
                var cellFactory = controller.getSavedNamesList().getCellFactory();
                assertNotNull(cellFactory);

                ListCell<SavedName> cell = cellFactory.call(controller.getSavedNamesList());
                SavedName item = new SavedName(1, "Mario");

                // Invoke updateItem on SavedNameCell
                var method = cell.getClass().getDeclaredMethod("updateItem", SavedName.class, boolean.class);
                method.setAccessible(true);
                method.invoke(cell, item, false);

                assertNotNull(cell.getGraphic());
                assertTrue(cell.getGraphic() instanceof HBox);
                HBox row = (HBox) cell.getGraphic();

                // Requirement: Displays ONLY: all the name saved in the db, the add button for adding in the wheel, an "x" button for deleting it from the db
                assertEquals(3, row.getChildren().size(), "Row must contain exactly 3 nodes: name label, add button, delete button");

                assertTrue(row.getChildren().get(0) instanceof Label);
                Label nameLbl = (Label) row.getChildren().get(0);
                assertEquals("Mario", nameLbl.getText());

                assertTrue(row.getChildren().get(1) instanceof Button);
                Button addBtn = (Button) row.getChildren().get(1);
                assertEquals("+ Add", addBtn.getText());

                assertTrue(row.getChildren().get(2) instanceof Button);
                Button delBtn = (Button) row.getChildren().get(2);
                assertEquals("×", delBtn.getText());

                // Check Banned Table columns
                assertEquals(4, controller.getBannedTable().getColumns().size());
                assertEquals("Name", controller.getBannedTable().getColumns().get(0).getText());
                assertEquals("Bans", controller.getBannedTable().getColumns().get(1).getText());
                assertEquals("Last Ban", controller.getBannedTable().getColumns().get(2).getText());
                assertEquals("Action", controller.getBannedTable().getColumns().get(3).getText());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Test execution timed out");
        if (failure.get() != null) {
            fail(failure.get());
        }
    }

    @Test
    void startupLoadsLastUsedNamesIntoParticipantsList() throws Exception {
        SavedNamesDao dao = new SavedNamesDao(db.getConnection());
        dao.deleteAll();

        // Older batch from 5 minutes ago (outside the 2-minute interval)
        long older = System.currentTimeMillis() - 300_000;
        dao.recordUsage("OldUser", new java.sql.Timestamp(older));

        // Latest spin batch
        long recent = System.currentTimeMillis();
        java.sql.Timestamp batchTime = new java.sql.Timestamp(recent);
        dao.recordUsage("Alice", batchTime);
        dao.recordUsage("Bob", batchTime);

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            controller.getParticipants().clear();
            controller.loadLastUsedParticipantsFromDb();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Wait for background load task to complete and populate participants
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline && controller.getParticipants().size() < 2) {
            Thread.sleep(50);
        }

        CountDownLatch verifyLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            assertEquals(2, controller.getParticipants().size());
            assertTrue(controller.isParticipantInWheel("Alice"));
            assertTrue(controller.isParticipantInWheel("Bob"));
            assertFalse(controller.isParticipantInWheel("OldUser"));
            verifyLatch.countDown();
        });
        assertTrue(verifyLatch.await(5, TimeUnit.SECONDS));
    }
}
