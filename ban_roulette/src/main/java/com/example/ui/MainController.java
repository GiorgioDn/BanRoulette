package com.example.ui;

import com.example.db.BannedDao;
import com.example.db.DatabaseManager;
import com.example.db.NameDao;
import com.example.model.Participant;
import com.example.svg.WheelGenerator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for {@code main.fxml} — the primary application window.
 *
 * <p>Responsibilities (Step 3):
 * <ul>
 *   <li>Receive a {@link DatabaseManager} via {@link #setDatabaseManager}
 *       (called by {@code App} after FXML loading).</li>
 *   <li>On startup, load saved participants from {@code names} table off the
 *       FX thread and update the UI with {@code Platform.runLater}.</li>
 *   <li>Persist every add / clear to the DB via a background {@link Task}.</li>
 *   <li>Keep the UI fully functional when the DB is unavailable (null guard).</li>
 * </ul>
 *
 * <p>Steps 5–7 will wire in the spin animation, hit animation, and ban persistence.
 */
public class MainController {

    private static final Logger LOG = Logger.getLogger(MainController.class.getName());

    // ── FXML-injected nodes ────────────────────────────────────────────────────

    @FXML private TextField  nameInput;
    @FXML private Button     addButton;
    @FXML private Button     clearButton;
    @FXML private Button     spinButton;
    @FXML private ListView<Participant> participantList;
    @FXML private StackPane  wheelContainer;
    @FXML private Label      wheelPlaceholder;
    @FXML private Label      statusLabel;
    @FXML private Label      bottomStatus;

    // ── State ─────────────────────────────────────────────────────────────────

    private final ObservableList<Participant> participants =
            FXCollections.observableArrayList();

    private final WheelGenerator wheelGenerator = new WheelGenerator();

    /** Injected by {@code App.start()} after FXML loading. May be null if DB failed to open. */
    private DatabaseManager dbManager;

    // ── DB injection ───────────────────────────────────────────────────────────

    /**
     * Called by {@link com.example.app.App} right after FXML loading.
     * Kicks off a background task to load saved names from the database.
     *
     * @param dbManager the open database manager; may be {@code null} if the
     *                  DB connection failed (app still works in-memory only)
     */
    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        if (dbManager != null && dbManager.getConnection() != null) {
            loadNamesFromDb();
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        participantList.setItems(participants);
        participantList.setCellFactory(lv -> new ParticipantCell());

        // Allow pressing Enter in the text field to add a name
        nameInput.setOnAction(e -> onAddName());

        updateUI();
    }

    // ── Event handlers ─────────────────────────────────────────────────────────

    @FXML
    private void onAddName() {
        String raw  = nameInput.getText();
        String name = (raw == null) ? "" : raw.trim();

        if (name.isEmpty()) {
            flashStatus("Type a name first.");
            return;
        }
        if (name.length() > 50) {
            flashStatus("Name too long — max 50 characters.");
            return;
        }

        // Optimistic UI update: add in-memory immediately so the user sees instant feedback
        Participant p = new Participant(name);
        participants.add(p);
        nameInput.clear();
        nameInput.requestFocus();
        refreshWheel();
        updateUI();

        // Persist to DB in the background, then back-fill the generated id
        if (dbManager != null) {
            Task<Long> insertTask = new Task<>() {
                @Override protected Long call() throws Exception {
                    return new NameDao(dbManager.getConnection()).insert(name);
                }
            };
            insertTask.setOnSucceeded(e -> p.setId(insertTask.getValue()));
            insertTask.setOnFailed(e ->
                    LOG.log(Level.WARNING, "Failed to persist participant", insertTask.getException()));
            new Thread(insertTask, "db-insert").start();
        }
    }

    @FXML
    private void onClearAll() {
        participants.clear();
        statusLabel.setText("");
        refreshWheel();
        updateUI();

        if (dbManager != null) {
            Task<Void> clearTask = new Task<>() {
                @Override protected Void call() throws Exception {
                    new NameDao(dbManager.getConnection()).deleteAll();
                    return null;
                }
            };
            clearTask.setOnFailed(e ->
                    LOG.log(Level.WARNING, "Failed to clear participants in DB", clearTask.getException()));
            new Thread(clearTask, "db-clear").start();
        }
    }

    @FXML
    private void onSpin() {
        if (participants.size() < 2) {
            flashStatus("Need at least 2 participants to spin.");
            return;
        }
        flashStatus("Spinning… (full animation arrives in Step 5)");
        // TODO Step 5: pick winner, compute targetAngle, run SpinAnimation
        // TODO Step 6: on spin finish, run HitAnimation, show ban dialog
        // TODO Step 7: ban() in NameDao, redraw wheel
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Loads all saved participants from the {@code names} table on a background
     * thread, then populates the UI list with {@code Platform.runLater}.
     */
    private void loadNamesFromDb() {
        Task<List<Participant>> loadTask = new Task<>() {
            @Override protected List<Participant> call() throws Exception {
                return new NameDao(dbManager.getConnection()).findAll();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Participant> saved = loadTask.getValue();
            Platform.runLater(() -> {
                participants.setAll(saved);
                refreshWheel();
                updateUI();
                if (!saved.isEmpty()) {
                    flashStatus(saved.size() + " participant(s) loaded from database.");
                }
            });
        });

        loadTask.setOnFailed(e ->
                LOG.log(Level.WARNING, "Failed to load participants from DB", loadTask.getException()));

        new Thread(loadTask, "db-load").start();
    }

    private void refreshWheel() {
        // Remove any previously generated wheel Group
        wheelContainer.getChildren().removeIf(n -> n instanceof Group);

        if (participants.size() < 2) {
            wheelPlaceholder.setVisible(true);
        } else {
            wheelPlaceholder.setVisible(false);
            // WheelGenerator returns an empty Group until Step 4 is implemented
            Group wheel = wheelGenerator.generateWheel(participants, WheelGenerator.DEFAULT_RADIUS);
            wheelContainer.getChildren().add(0, wheel);
        }
    }

    private void updateUI() {
        int n = participants.size();
        spinButton.setDisable(n < 2);

        if (n == 0) {
            bottomStatus.setText("Ready — add names to get started");
        } else if (n == 1) {
            bottomStatus.setText("Add one more name to enable the spin");
        } else {
            bottomStatus.setText(n + " participant" + (n == 1 ? "" : "s") + " · click ⚡ Spin! to begin");
        }
    }

    private void flashStatus(String message) {
        statusLabel.setText(message);
    }

    // ── Inner cell factory ─────────────────────────────────────────────────────

    /**
     * Renders each participant as a labelled row.
     * Step 4 will add a per-row remove button.
     */
    private static class ParticipantCell extends ListCell<Participant> {
        @Override
        protected void updateItem(Participant item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.getName());
                setGraphic(null);
                // TODO Step 4: add a small × button to remove this participant
            }
        }
    }
}
