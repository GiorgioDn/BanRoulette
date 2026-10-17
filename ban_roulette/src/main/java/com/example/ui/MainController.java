package com.example.ui;

import com.example.db.BannedDao;
import com.example.db.DatabaseManager;
import com.example.db.NameDao;
import com.example.model.Participant;
import com.example.svg.WheelGenerator;
import com.example.anim.SpinAnimation;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.security.SecureRandom;
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
    @FXML private Button     loadSavedButton;
    @FXML private ListView<Participant> participantList;
    @FXML private StackPane  wheelContainer;
    @FXML private VBox       wheelPlaceholderBox;
    @FXML private Label      wheelPlaceholder;
    @FXML private Label      statusLabel;
    @FXML private Label      bottomStatus;

    // ── State ─────────────────────────────────────────────────────────────────

    private final ObservableList<Participant> participants =
            FXCollections.observableArrayList();

    private final WheelGenerator wheelGenerator = new WheelGenerator();
    private final SpinAnimation  spinAnimation  = new SpinAnimation();

    /**
     * The rotating slice {@link Group} most recently built by {@link #refreshWheel()}.
     * Step 5 reads this to apply the {@link SpinAnimation}.
     */
    private Group currentWheel;

    /** The pointer node placed on top of the wheel. */
    private Node currentPointerNode;

    /** Active pointer wobble animation during spin. */
    private RotateTransition pointerWobble;

    /** Flag indicating a spin is currently in progress. */
    private boolean isSpinning = false;

    /** The participant pre-selected as the winner of the current/last spin. */
    private Participant currentWinner;

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
        // Pass 'this' so the cell can call back into onRemoveParticipant()
        participantList.setCellFactory(lv -> new ParticipantCell(this));

        // Allow pressing Enter in the text field to add a name
        nameInput.setOnAction(e -> onAddName());

        // Trigger the spin by clicking directly on the wheel container
        wheelContainer.setOnMouseClicked(e -> {
            if (!isSpinning && participants.size() >= 2) {
                onSpin();
            }
        });

        updateUI();
    }

    // ── Event handlers ─────────────────────────────────────────────────────────

    @FXML
    private void onAddName() {
        if (isSpinning) {
            return;
        }
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
        if (participants.size() >= WheelGenerator.MAX_PARTICIPANTS) {
            flashStatus("Limit reached — max " + WheelGenerator.MAX_PARTICIPANTS + " participants.");
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
        if (isSpinning) {
            return;
        }
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
    private void onLoadSavedNames() {
        if (isSpinning) {
            return;
        }
        if (dbManager != null && dbManager.getConnection() != null) {
            flashStatus("Loading saved participants…");
            loadNamesFromDb();
        } else {
            flashStatus("Database not available.");
        }
    }

    @FXML
    private void onSpin() {
        if (isSpinning) {
            return;
        }
        if (participants.size() < 2) {
            flashStatus("Need at least 2 participants to spin.");
            return;
        }
        if (currentWheel == null) {
            return;
        }

        isSpinning = true;
        setUiSpinningState(true);

        // 1. Fair pre-selection of winner (testable, independent of animation physics)
        SecureRandom random = new SecureRandom();
        int winnerIndex = SpinAnimation.pickRandomWinner(participants.size(), random);
        currentWinner = participants.get(winnerIndex);

        // 2. Random offset within ±40% of slice angle for natural stopping variation
        double offsetFactor = (random.nextDouble() * 2.0) - 1.0; // [-1.0, 1.0]
        double offset = SpinAnimation.computeRandomOffset(participants.size(), offsetFactor);
        int fullSpins = 5 + random.nextInt(3); // 5 to 7 full revolutions

        // 3. Compute target angle guaranteeing forward rotation and exact landing
        double targetAngle = SpinAnimation.computeTargetAngle(
                currentWheel.getRotate(), winnerIndex, participants.size(), fullSpins, offset);

        flashStatus("Spinning the wheel…");
        bottomStatus.setText("Spinning… good luck!");

        // 4. Subtle pointer wobble while spinning
        if (currentPointerNode != null) {
            pointerWobble = SpinAnimation.buildPointerWobble(currentPointerNode);
            pointerWobble.play();
        }

        // 5. RotateTransition on wheel group (4-8s, Interpolator.EASE_OUT)
        RotateTransition rt = spinAnimation.buildSpin(currentWheel, targetAngle, () -> {
            if (pointerWobble != null) {
                pointerWobble.stop();
                if (currentPointerNode != null) {
                    currentPointerNode.setRotate(0);
                }
            }
            isSpinning = false;
            setUiSpinningState(false);
            updateUI();

            flashStatus("🎯 Selected: " + currentWinner.getName() + "!");
            bottomStatus.setText("Landed on " + currentWinner.getName() + " · ready for Step 6 hammer hit");

            // TODO Step 6: run HitAnimation and show ban dialog
            // TODO Step 7: ban() in NameDao, redraw wheel
        });

        rt.play();
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
                } else {
                    flashStatus("No saved participants found in database.");
                }
            });
        });

        loadTask.setOnFailed(e ->
                LOG.log(Level.WARNING, "Failed to load participants from DB", loadTask.getException()));

        new Thread(loadTask, "db-load").start();
    }

    private void refreshWheel() {
        // Remove old wheel group and old pointer (anything that is not the placeholder VBox)
        wheelContainer.getChildren().removeIf(
                n -> n instanceof Group || "wheel-pointer".equals(n.getId()));

        if (participants.size() < 2) {
            if (wheelPlaceholderBox != null) {
                wheelPlaceholderBox.setVisible(true);
                wheelPlaceholderBox.setManaged(true);
            }
            wheelPlaceholder.setVisible(true);
            currentWheel = null;
        } else {
            if (wheelPlaceholderBox != null) {
                wheelPlaceholderBox.setVisible(false);
                wheelPlaceholderBox.setManaged(false);
            }
            wheelPlaceholder.setVisible(false);

            // generateWheel() returns a Group containing slices, labels, dividers
            // AND the pointer as its last child. We pull the pointer out so it stays
            // stationary while only the slice group rotates (Step 5).
            Group full    = wheelGenerator.generateWheel(participants, WheelGenerator.DEFAULT_RADIUS);
            int   lastIdx = full.getChildren().size() - 1;
            var   pointer = full.getChildren().remove(lastIdx); // the pointer SVGPath
            pointer.setId("wheel-pointer");

            currentWheel = full; // the rotating part
            currentWheel.setCursor(Cursor.HAND);
            currentPointerNode = pointer;

            // wheel first, fixed pointer on top
            wheelContainer.getChildren().add(0, currentWheel);
            wheelContainer.getChildren().add(pointer);
        }
        wheelContainer.setCursor(participants.size() >= 2 ? Cursor.HAND : Cursor.DEFAULT);
    }

    private void updateUI() {
        int n = participants.size();
        spinButton.setDisable(isSpinning || n < 2);
        wheelContainer.setCursor(isSpinning ? Cursor.WAIT : (n >= 2 ? Cursor.HAND : Cursor.DEFAULT));

        if (n == 0) {
            bottomStatus.setText("Ready — add names to get started");
        } else if (n == 1) {
            bottomStatus.setText("Add one more name to enable the spin");
        } else {
            bottomStatus.setText(n + " participant" + (n == 1 ? "" : "s") + " · click ⚡ Spin! to begin");
        }
    }

    private void setUiSpinningState(boolean spinning) {
        nameInput.setDisable(spinning);
        addButton.setDisable(spinning);
        clearButton.setDisable(spinning);
        if (loadSavedButton != null) {
            loadSavedButton.setDisable(spinning);
        }
        spinButton.setDisable(spinning || participants.size() < 2);
        participantList.setDisable(spinning);
        wheelContainer.setCursor(spinning ? Cursor.WAIT : (participants.size() >= 2 ? Cursor.HAND : Cursor.DEFAULT));
    }

    private void flashStatus(String message) {
        statusLabel.setText(message);
    }

    // ── Per-participant remove ──────────────────────────────────────────────────

    /**
     * Removes a participant from the in-memory list and persists the delete.
     * Called back from {@link ParticipantCell} when the × button is clicked.
     */
    void onRemoveParticipant(Participant p) {
        if (isSpinning) {
            return;
        }
        participants.remove(p);
        refreshWheel();
        updateUI();
        flashStatus("Removed \u201c" + p.getName() + "\u201d.");

        if (dbManager != null && p.getId() > 0) {
            Task<Void> deleteTask = new Task<>() {
                @Override protected Void call() throws Exception {
                    new NameDao(dbManager.getConnection()).delete(p.getId());
                    return null;
                }
            };
            deleteTask.setOnFailed(e ->
                    LOG.log(Level.WARNING, "Failed to delete participant from DB", deleteTask.getException()));
            new Thread(deleteTask, "db-delete").start();
        }
    }

    /** Returns the currently displayed wheel Group (used by SpinAnimation in Step 5). */
    public Group getCurrentWheel() { return currentWheel; }

    /** Returns whether a spin animation is currently active. */
    public boolean isSpinning() { return isSpinning; }

    /** Returns the pre-selected winner of the most recent spin. */
    public Participant getCurrentWinner() { return currentWinner; }

    // ── Inner cell factory ─────────────────────────────────────────────────────

    /**
     * Renders each participant as a row with a name label and a × remove button.
     */
    private static class ParticipantCell extends ListCell<Participant> {

        private final MainController controller;

        ParticipantCell(MainController controller) {
            this.controller = controller;
        }

        @Override
        protected void updateItem(Participant item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                javafx.scene.control.Label nameLabel =
                        new javafx.scene.control.Label(item.getName());
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                nameLabel.getStyleClass().add("cell-name-label");

                javafx.scene.control.Button removeBtn =
                        new javafx.scene.control.Button("\u00d7");
                removeBtn.getStyleClass().add("btn-cell-remove");
                removeBtn.setOnAction(e -> controller.onRemoveParticipant(item));
                removeBtn.setFocusTraversable(false);

                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(nameLabel, removeBtn);
                row.getStyleClass().add("cell-row");
                javafx.scene.layout.HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                setText(null);
                setGraphic(row);
            }
        }
    }
}
