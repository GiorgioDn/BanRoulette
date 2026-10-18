package com.example.ui;

import com.example.db.DatabaseManager;
import com.example.db.SavedNamesDao;
import com.example.model.SavedName;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
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
}
