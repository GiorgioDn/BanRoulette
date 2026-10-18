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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
 * <p>Responsibilities:
 * <ul>
 *   <li>Manage participants for the roulette wheel.</li>
 *   <li>When a participant is chosen by the roulette for a ban:
 *       record/increment their count in {@code banned_names} in H2 without deleting them
 *       from the active names list or wheel.</li>
 *   <li>Display both the names used and the banned names with their cumulative count.</li>
 *   <li>Persist participant additions/deletions and ban counts via background tasks.</li>
 * </ul>
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
    @FXML private TableColumn<BannedName, String> bannedCountCol;
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
     * Kicks off background tasks to load saved names and banned history from the database.
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
        // Pass 'this' so the cell can display ban count badge and handle remove
        participantList.setCellFactory(lv -> new ParticipantCell(this));

        // Configure Banned TableView (Step 7)
        if (bannedTable != null) {
            bannedTable.setItems(bannedList);

            if (bannedNameCol != null) {
                bannedNameCol.setCellValueFactory(cd ->
                        new SimpleStringProperty(cd.getValue().getName()));
            }

            if (bannedCountCol != null) {
                bannedCountCol.setCellValueFactory(cd ->
                        new SimpleStringProperty(String.valueOf(cd.getValue().getBanCount())));
                bannedCountCol.setCellFactory(col -> new TableCell<>() {
                    private final Label countLabel = new Label();
                    {
                        countLabel.getStyleClass().add("banned-count-pill");
                    }
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                        } else {
                            countLabel.setText(item + "×");
                            setGraphic(countLabel);
                            setAlignment(Pos.CENTER);
                        }
                    }
                });
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
                    private final Button resetBtn = new Button("Reset");
                    {
                        resetBtn.getStyleClass().add("btn-restore");
                        resetBtn.setFocusTraversable(false);
                        resetBtn.setOnAction(e -> {
                            BannedName item = getTableView().getItems().get(getIndex());
                            if (item != null) {
                                onRestoreParticipant(item);
                            }
                        });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : resetBtn);
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

            // Trigger the ban hammer hit sequence
            playHammerHitAnimation(currentWinner);
        });

        rt.play();
    }

    /**
     * Executes the hammer strike animation.
     * When completed, records/increments the ban count in {@code banned_names} without
     * deleting the participant from active names or the wheel, and displays the result dialog.
     */
    void playHammerHitAnimation(Participant winner) {
        Group hammer = hammerLoader.loadHammer();
        hammer.setTranslateX(110.0);
        hammer.setTranslateY(-380.0);

        int overlayIdx = (banDialogOverlay != null)
                ? wheelContainer.getChildren().indexOf(banDialogOverlay) : -1;
        if (overlayIdx >= 0) {
            wheelContainer.getChildren().add(overlayIdx, hammer);
        } else {
            wheelContainer.getChildren().add(hammer);
        }

        SequentialTransition hit = hitAnimation.buildHit(hammer, currentWheel, () -> {
            wheelContainer.getChildren().remove(hammer);

            // Record the ban without deleting from participants or names table
            if (dbManager != null && dbManager.getConnection() != null) {
                Task<BannedName> banTask = new Task<>() {
                    @Override protected BannedName call() throws Exception {
                        BannedDao bannedDao = new BannedDao(dbManager.getConnection());
                        return bannedDao.recordBan(winner.getName());
                    }
                };
                banTask.setOnSucceeded(ev -> {
                    BannedName record = banTask.getValue();
                    int count = (record != null) ? record.getBanCount() : 1;
                    loadBannedFromDb();
                    participantList.refresh();
                    showBanDialog(winner.getName(), count);
                });
                banTask.setOnFailed(ev -> {
                    LOG.log(Level.WARNING, "Failed to persist ban in DB", banTask.getException());
                    int count = updateInMemoryBan(winner.getName());
                    participantList.refresh();
                    showBanDialog(winner.getName(), count);
                });
                new Thread(banTask, "db-ban").start();
            } else {
                int count = updateInMemoryBan(winner.getName());
                participantList.refresh();
                showBanDialog(winner.getName(), count);
            }

            flashStatus("🔨 " + winner.getName() + " was chosen by the roulette!");
            bottomStatus.setText("Selected: " + winner.getName() + " · ready for next spin");
        });

        hit.play();
    }

    private int updateInMemoryBan(String name) {
        for (BannedName bn : bannedList) {
            if (bn.getName().equalsIgnoreCase(name)) {
                bn.incrementCount();
                bn.setBannedAt(LocalDateTime.now());
                if (bannedTable != null) bannedTable.refresh();
                updateBannedCountBadge();
                return bn.getBanCount();
            }
        }
        BannedName newRecord = new BannedName(0, name, 1, LocalDateTime.now());
        bannedList.add(0, newRecord);
        updateBannedCountBadge();
        return 1;
    }

    private void updateBannedCountBadge() {
        if (bannedCountBadge != null) {
            int total = 0;
            for (BannedName bn : bannedList) {
                total += bn.getBanCount();
            }
            bannedCountBadge.setText(String.valueOf(total));
        }
    }

    /** Returns the current ban count for a name, or 0 if never chosen for a ban. */
    public int getBanCountFor(String name) {
        if (name == null) return 0;
        for (BannedName bn : bannedList) {
            if (name.equalsIgnoreCase(bn.getName())) {
                return bn.getBanCount();
            }
        }
        return 0;
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
                updateBannedCountBadge();
                participantList.refresh();
            });
        });

        loadTask.setOnFailed(e ->
                LOG.log(Level.WARNING, "Failed to load banned names from DB", loadTask.getException()));

        new Thread(loadTask, "db-load-banned").start();
    }

    private void refreshWheel() {
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

            Group full    = wheelGenerator.generateWheel(participants, WheelGenerator.DEFAULT_RADIUS);
            int   lastIdx = full.getChildren().size() - 1;
            var   pointer = full.getChildren().remove(lastIdx);
            pointer.setId("wheel-pointer");

            currentWheel = full;
            currentWheel.setCursor(Cursor.HAND);
            currentPointerNode = pointer;

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

    /** Returns the currently displayed wheel Group. */
    public Group getCurrentWheel() { return currentWheel; }

    /** Returns whether a spin animation is currently active. */
    public boolean isSpinning() { return isSpinning; }

    /** Returns the pre-selected winner of the most recent spin. */
    public Participant getCurrentWinner() { return currentWinner; }

    /** Returns the observable list of banned names. */
    public ObservableList<BannedName> getBannedList() { return bannedList; }

    /** Returns the banned names TableView. */
    public TableView<BannedName> getBannedTable() { return bannedTable; }

    // ── Restore & Banned actions ──────────────────────────────────────────────

    /**
     * Resets a banned participant's count: removes them from the banned list
     * and clears the ban record in the database.
     */
    void onRestoreParticipant(BannedName item) {
        if (isSpinning || item == null) {
            return;
        }

        bannedList.remove(item);
        updateBannedCountBadge();
        participantList.refresh();
        flashStatus("Reset ban count for \u201c" + item.getName() + "\u201d.");

        if (dbManager != null && dbManager.getConnection() != null) {
            Task<Void> deleteTask = new Task<>() {
                @Override protected Void call() throws Exception {
                    new BannedDao(dbManager.getConnection()).delete(item.getId());
                    return null;
                }
            };
            deleteTask.setOnFailed(e ->
                    LOG.log(Level.WARNING, "Failed to reset ban in DB", deleteTask.getException()));
            new Thread(deleteTask, "db-delete-banned").start();
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
        participantList.refresh();
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

    // ── Ban dialog display & dismiss ──────────────────────────────────────────

    /**
     * Displays the result dialog overlay announcing the participant chosen for ban and their count.
     */
    public void showBanDialog(String name, int banCount) {
        if (banDialogMessage != null) {
            String suffix = (banCount > 1) ? ("\n(Ban count: " + banCount + "×)") : "\n(First ban!)";
            banDialogMessage.setText(name + " has been chosen for a ban!" + suffix);
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
     * Displays the result dialog overlay announcing the participant chosen for ban.
     */
    public void showBanDialog(String name) {
        showBanDialog(name, getBanCountFor(name));
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
     * Renders each participant as a row with a name label, an optional ban count badge,
     * and a × remove button.
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
                Label nameLabel = new Label(item.getName());
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                nameLabel.getStyleClass().add("cell-name-label");

                int banCount = controller.getBanCountFor(item.getName());
                Label banBadge = null;
                if (banCount > 0) {
                    banBadge = new Label("🔨 " + banCount);
                    banBadge.getStyleClass().add("cell-ban-badge");
                }

                Button removeBtn = new Button("\u00d7");
                removeBtn.getStyleClass().add("btn-cell-remove");
                removeBtn.setOnAction(e -> controller.onRemoveParticipant(item));
                removeBtn.setFocusTraversable(false);

                HBox row;
                if (banBadge != null) {
                    row = new HBox(8, nameLabel, banBadge, removeBtn);
                } else {
                    row = new HBox(8, nameLabel, removeBtn);
                }
                row.getStyleClass().add("cell-row");
                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                row.setAlignment(Pos.CENTER_LEFT);

                setText(null);
                setGraphic(row);
            }
        }
    }
}
