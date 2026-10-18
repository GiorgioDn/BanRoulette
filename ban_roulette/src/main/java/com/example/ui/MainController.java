package com.example.ui;

import com.example.anim.HitAnimation;
import com.example.anim.SpinAnimation;
import com.example.db.BannedDao;
import com.example.db.DatabaseManager;
import com.example.db.NameDao;
import com.example.model.BannedName;
import com.example.model.Participant;
import com.example.svg.HammerLoader;
import com.example.svg.WheelGenerator;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // Ban dialog overlay nodes (Step 6)
    @FXML private StackPane  banDialogOverlay;
    @FXML private Label      banDialogMessage;
    @FXML private Button     banDialogDismissBtn;

    // Right sidebar — Banned history TableView (Step 7)
    @FXML private VBox                         rightSidebar;
    @FXML private Label                        bannedCountBadge;
    @FXML private TableView<BannedName>        bannedTable;
    @FXML private TableColumn<BannedName, String> bannedNameCol;
    @FXML private TableColumn<BannedName, String> bannedDateCol;
    @FXML private TableColumn<BannedName, Void>   bannedActionCol;
    @FXML private Button                       clearBannedBtn;
    @FXML private Button                       toggleHistoryBtn;

    // ── State ─────────────────────────────────────────────────────────────────

    private final ObservableList<Participant> participants =
            FXCollections.observableArrayList();

    private final ObservableList<BannedName> bannedList =
            FXCollections.observableArrayList();

    private final WheelGenerator wheelGenerator = new WheelGenerator();
    private final SpinAnimation  spinAnimation  = new SpinAnimation();
    private final HitAnimation   hitAnimation   = new HitAnimation();
    private final HammerLoader   hammerLoader   = new HammerLoader();

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
            loadBannedFromDb();
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        participantList.setItems(participants);
        // Pass 'this' so the cell can call back into onRemoveParticipant()
        participantList.setCellFactory(lv -> new ParticipantCell(this));

        // Configure Banned TableView (Step 7)
        if (bannedTable != null) {
            bannedTable.setItems(bannedList);

            if (bannedNameCol != null) {
                bannedNameCol.setCellValueFactory(cd ->
                        new SimpleStringProperty(cd.getValue().getName()));
            }

            if (bannedDateCol != null) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM HH:mm");
                bannedDateCol.setCellValueFactory(cd -> {
                    LocalDateTime dt = cd.getValue().getBannedAt();
                    return new SimpleStringProperty(dt != null ? dt.format(dtf) : "—");
                });
            }

            if (bannedActionCol != null) {
                bannedActionCol.setCellFactory(col -> new TableCell<>() {
                    private final Button restoreBtn = new Button("Restore");
                    {
                        restoreBtn.getStyleClass().add("btn-restore");
                        restoreBtn.setFocusTraversable(false);
                        restoreBtn.setOnAction(e -> {
                            BannedName item = getTableView().getItems().get(getIndex());
                            if (item != null) {
                                onRestoreParticipant(item);
                            }
                        });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : restoreBtn);
                        setAlignment(Pos.CENTER);
                    }
                });
            }
        }

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

            flashStatus("⚡ Ban hammer incoming for " + currentWinner.getName() + "…");
            bottomStatus.setText("Hammer strike incoming…");

            // Step 6: Trigger the ban hammer hit sequence
            playHammerHitAnimation(currentWinner);
        });

        rt.play();
    }

    /**
     * Executes Step 6: loads the hammer SVG, places it above the winning slice,
     * runs the wind-up -> strike -> impact shake -> retreat sequence, and shows
     * the result dialog on completion.
     */
    void playHammerHitAnimation(Participant winner) {
        Group hammer = hammerLoader.loadHammer();
        // Position hammer above-right of the 12 o'clock pointer so the strike swings into the slice
        hammer.setTranslateX(110.0);
        hammer.setTranslateY(-380.0);

        // Add above wheel/pointer but behind the dialog overlay
        int overlayIdx = (banDialogOverlay != null)
                ? wheelContainer.getChildren().indexOf(banDialogOverlay) : -1;
        if (overlayIdx >= 0) {
            wheelContainer.getChildren().add(overlayIdx, hammer);
        } else {
            wheelContainer.getChildren().add(hammer);
        }

        SequentialTransition hit = hitAnimation.buildHit(hammer, currentWheel, () -> {
            wheelContainer.getChildren().remove(hammer);

            // Step 7: Persist the ban and tie everything together
            // 1. Remove from active participants in memory
            participants.remove(winner);

            // 2. Add to banned list in memory
            BannedName bannedRecord = new BannedName(0, winner.getName(), LocalDateTime.now());
            bannedList.add(0, bannedRecord);
            if (bannedCountBadge != null) {
                bannedCountBadge.setText(String.valueOf(bannedList.size()));
            }

            // 3. Redraw the wheel without the banned name; if fewer than 2 remain, spinning is disabled
            refreshWheel();
            updateUI();

            // 4. In background: persist ban in a SINGLE transaction (atomic move)
            if (dbManager != null && dbManager.getConnection() != null) {
                Task<Void> banTask = new Task<>() {
                    @Override protected Void call() throws Exception {
                        Connection conn = dbManager.getConnection();
                        NameDao nameDao = new NameDao(conn);
                        BannedDao bannedDao = new BannedDao(conn);
                        if (winner.getId() > 0) {
                            nameDao.ban(winner.getId(), bannedDao);
                        } else {
                            bannedDao.insert(winner.getName());
                        }
                        return null;
                    }
                };
                banTask.setOnSucceeded(ev -> loadBannedFromDb());
                banTask.setOnFailed(ev ->
                        LOG.log(Level.WARNING, "Failed to persist ban in single transaction", banTask.getException()));
                new Thread(banTask, "db-ban").start();
            }

            // 5. Show ban dialog overlay
            showBanDialog(winner.getName());
            flashStatus("🔨 " + winner.getName() + " has been banned!");
            bottomStatus.setText("Banned: " + winner.getName() +
                    (participants.size() < 2 ? " · need at least 2 participants to spin" : " · ready"));
        });

        hit.play();
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

    /**
     * Loads all banned participants from the {@code banned_names} table on a
     * background thread and updates {@link #bannedList} on the FX thread.
     */
    private void loadBannedFromDb() {
        if (dbManager == null || dbManager.getConnection() == null) return;

        Task<List<BannedName>> loadTask = new Task<>() {
            @Override protected List<BannedName> call() throws Exception {
                return new BannedDao(dbManager.getConnection()).findAll();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<BannedName> saved = loadTask.getValue();
            Platform.runLater(() -> {
                bannedList.setAll(saved);
                if (bannedCountBadge != null) {
                    bannedCountBadge.setText(String.valueOf(saved.size()));
                }
            });
        });

        loadTask.setOnFailed(e ->
                LOG.log(Level.WARNING, "Failed to load banned names from DB", loadTask.getException()));

        new Thread(loadTask, "db-load-banned").start();
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
        if (banDialogOverlay != null) {
            banDialogOverlay.toFront();
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
        if (clearBannedBtn != null) {
            clearBannedBtn.setDisable(spinning);
        }
        if (bannedTable != null) {
            bannedTable.setDisable(spinning);
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

    /** Returns the observable list of banned names. */
    public ObservableList<BannedName> getBannedList() { return bannedList; }

    /** Returns the banned names TableView. */
    public TableView<BannedName> getBannedTable() { return bannedTable; }

    // ── Restore & Banned actions (Step 7) ──────────────────────────────────────

    /**
     * Restores a banned participant: removes them from the banned list,
     * re-adds them to the active participants list, redraws the wheel,
     * and performs the atomic DB restore in the background.
     */
    void onRestoreParticipant(BannedName item) {
        if (isSpinning || item == null) {
            return;
        }

        // 1. Remove from banned list
        bannedList.remove(item);
        if (bannedCountBadge != null) {
            bannedCountBadge.setText(String.valueOf(bannedList.size()));
        }

        // 2. Add back to active participants
        Participant restored = new Participant(item.getName());
        participants.add(restored);

        // 3. Redraw wheel and update UI
        refreshWheel();
        updateUI();
        flashStatus("Restored \u201c" + item.getName() + "\u201d to the wheel.");

        // 4. Background DB atomic restore transaction
        if (dbManager != null && dbManager.getConnection() != null) {
            Task<Long> restoreTask = new Task<>() {
                @Override protected Long call() throws Exception {
                    Connection conn = dbManager.getConnection();
                    BannedDao bannedDao = new BannedDao(conn);
                    NameDao nameDao = new NameDao(conn);
                    if (item.getId() > 0) {
                        Participant p = bannedDao.restore(item.getId(), nameDao);
                        return p.getId();
                    } else {
                        return nameDao.insert(item.getName());
                    }
                }
            };
            restoreTask.setOnSucceeded(e -> restored.setId(restoreTask.getValue()));
            restoreTask.setOnFailed(e ->
                    LOG.log(Level.WARNING, "Failed to restore participant in DB", restoreTask.getException()));
            new Thread(restoreTask, "db-restore").start();
        }
    }

    @FXML
    private void onClearBannedHistory() {
        if (isSpinning) {
            return;
        }
        bannedList.clear();
        if (bannedCountBadge != null) {
            bannedCountBadge.setText("0");
        }
        flashStatus("Banned history cleared.");

        if (dbManager != null && dbManager.getConnection() != null) {
            Task<Void> clearTask = new Task<>() {
                @Override protected Void call() throws Exception {
                    new BannedDao(dbManager.getConnection()).deleteAll();
                    return null;
                }
            };
            clearTask.setOnFailed(e ->
                    LOG.log(Level.WARNING, "Failed to clear banned history in DB", clearTask.getException()));
            new Thread(clearTask, "db-clear-banned").start();
        }
    }

    @FXML
    private void onToggleHistory() {
        if (rightSidebar != null) {
            boolean visible = !rightSidebar.isVisible();
            rightSidebar.setVisible(visible);
            rightSidebar.setManaged(visible);
            if (toggleHistoryBtn != null) {
                toggleHistoryBtn.setText(visible ? "Hide History" : "📜 History");
            }
        }
    }

    // ── Ban dialog display & dismiss (Step 6) ──────────────────────────────────

    /**
     * Displays the result dialog overlay announcing the banned participant.
     */
    public void showBanDialog(String name) {
        if (banDialogMessage != null) {
            banDialogMessage.setText(name + " has been banned!");
        }
        if (banDialogOverlay != null) {
            banDialogOverlay.setOpacity(0.0);
            banDialogOverlay.setVisible(true);
            banDialogOverlay.setManaged(true);
            banDialogOverlay.toFront();

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), banDialogOverlay);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } else {
            isSpinning = false;
            setUiSpinningState(false);
            updateUI();
        }
    }

    /**
     * Dismisses the ban result dialog and unlocks the UI controls.
     */
    @FXML
    public void onDismissBanDialog() {
        if (banDialogOverlay != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), banDialogOverlay);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                banDialogOverlay.setVisible(false);
                banDialogOverlay.setManaged(false);
                isSpinning = false;
                setUiSpinningState(false);
                updateUI();
            });
            fadeOut.play();
        } else {
            isSpinning = false;
            setUiSpinningState(false);
            updateUI();
        }
    }

    /** Returns whether the ban dialog overlay is currently displayed. */
    public boolean isBanDialogVisible() {
        return banDialogOverlay != null && banDialogOverlay.isVisible();
    }

    /** Returns the text currently displayed in the ban dialog. */
    public String getBanDialogMessageText() {
        return (banDialogMessage != null) ? banDialogMessage.getText() : "";
    }

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
