package com.example.ui;

import com.example.anim.HitAnimation;
import com.example.anim.SpinAnimation;
import com.example.db.BannedDao;
import com.example.db.DatabaseManager;
import com.example.db.SavedNamesDao;
import com.example.model.BannedName;
import com.example.model.Participant;
import com.example.model.SavedName;
import com.example.svg.HammerLoader;
import com.example.svg.WheelGenerator;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.security.SecureRandom;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for {@code main.fxml} — the primary application window.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Manage participants for the roulette wheel.</li>
 *   <li>Dynamically scale the wheel, pointer, and hammer with window resizing.</li>
 *   <li>Keyboard shortcuts for spinning (Space, Ctrl+Enter / Cmd+Enter).</li>
 *   <li>When a participant is chosen for a ban: record/increment their count in {@code banned_names}
 *       without deleting them from the active names list or wheel.</li>
 *   <li>Display both the names used and the banned names with their cumulative count.</li>
 *   <li>User-facing alerts for database failures and graceful in-memory fallbacks.</li>
 * </ul>
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
    @FXML private VBox       wheelPlaceholderBox;
    @FXML private Label      wheelPlaceholder;
    @FXML private Label      statusLabel;
    @FXML private Label      bottomStatus;

    // Ban dialog overlay nodes (Step 6)
    @FXML private StackPane  banDialogOverlay;
    @FXML private Label      banDialogMessage;
    @FXML private Button     banDialogDismissBtn;

    // Right sidebar — Banned history & Saved names panels
    @FXML private VBox                         rightSidebar;
    @FXML private VBox                         bannedBox;
    @FXML private Label                        bannedCountBadge;
    @FXML private TableView<BannedName>        bannedTable;
    @FXML private TableColumn<BannedName, String> bannedNameCol;
    @FXML private TableColumn<BannedName, String> bannedCountCol;
    @FXML private TableColumn<BannedName, String> bannedDateCol;
    @FXML private TableColumn<BannedName, Void>   bannedActionCol;
    @FXML private Button                       clearBannedBtn;
    @FXML private Button                       toggleHistoryBtn;

    // Right sidebar — Saved Names Catalog
    @FXML private VBox                         savedNamesBox;
    @FXML private Label                        savedCountBadge;
    @FXML private TextField                    savedNamesFilter;
    @FXML private ListView<SavedName>          savedNamesList;
    @FXML private Button                       addAllSavedBtn;
    @FXML private Button                       clearSavedBtn;
    @FXML private Button                       toggleSavedNamesBtn;

    // ── State ─────────────────────────────────────────────────────────────────

    private final ObservableList<Participant> participants =
            FXCollections.observableArrayList();

    private final ObservableList<BannedName> bannedList =
            FXCollections.observableArrayList();

    private final ObservableList<SavedName> savedNamesMasterList =
            FXCollections.observableArrayList();

    private FilteredList<SavedName> filteredSavedNames;

    /**
     * View mode for the right sidebar.
     */
    public enum RightSidebarView {
        NONE,
        BANNED,
        SAVED_NAMES
    }

    private RightSidebarView currentRightView = RightSidebarView.BANNED;

    private final WheelGenerator wheelGenerator = new WheelGenerator();
    private final SpinAnimation  spinAnimation  = new SpinAnimation();
    private final HitAnimation   hitAnimation   = new HitAnimation();
    private final HammerLoader   hammerLoader   = new HammerLoader();

    /**
     * Group wrapper holding wheel, pointer, and hammer for responsive proportional scaling.
     */
    private Group wheelScaleGroup;

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
            loadBannedFromDb();
            loadSavedNamesFromDb();
            loadLastUsedParticipantsFromDb();
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        participantList.setItems(participants);
        // Pass 'this' so the cell can display ban count badge and handle remove
        participantList.setCellFactory(lv -> new ParticipantCell(this));

        // Tooltip with keyboard shortcut hint
        if (spinButton != null) {
            spinButton.setTooltip(new Tooltip("Spin the wheel (Space / ⌘+Enter)"));
        }

        // Responsive wheel scaling when center container resizes
        wheelContainer.widthProperty().addListener((obs, oldW, newW) -> updateWheelScale());
        wheelContainer.heightProperty().addListener((obs, oldH, newH) -> updateWheelScale());

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
                    private final Button removeBtn = new Button("Remove");
                    {
                        removeBtn.getStyleClass().add("btn-restore");
                        removeBtn.setFocusTraversable(false);
                        removeBtn.setOnAction(e -> {
                            BannedName item = getTableView().getItems().get(getIndex());
                            if (item != null) {
                                onRemoveBannedName(item);
                            }
                        });
                    }
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : removeBtn);
                        setAlignment(Pos.CENTER);
                    }
                });
            }
        }

        // Configure Saved Names List
        filteredSavedNames = new FilteredList<>(savedNamesMasterList, p -> true);
        if (savedNamesFilter != null) {
            savedNamesFilter.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredSavedNames.setPredicate(sn -> {
                    if (newVal == null || newVal.isBlank()) return true;
                    return sn.getName().toLowerCase().contains(newVal.trim().toLowerCase());
                });
            });
        }
        if (savedNamesList != null) {
            savedNamesList.setItems(filteredSavedNames);
            savedNamesList.setCellFactory(lv -> new SavedNameCell(this));
        }

        // Initialize right sidebar view (Banned List by default)
        setRightSidebarView(RightSidebarView.BANNED);

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

    /**
     * Dynamically scales the wheel, pointer, and hammer group to fit inside the wheel container.
     */
    public void updateWheelScale() {
        if (wheelScaleGroup == null || wheelContainer == null) return;
        double w = wheelContainer.getWidth();
        double h = wheelContainer.getHeight();
        if (w <= 0 || h <= 0) return;
        double available = Math.min(w - 40.0, h - 40.0);
        double factor = Math.max(0.45, Math.min(1.4, available / 640.0));
        wheelScaleGroup.setScaleX(factor);
        wheelScaleGroup.setScaleY(factor);
    }

    /**
     * Triggered by keyboard shortcuts (Space, Ctrl+Enter / Cmd+Enter).
     */
    public void triggerSpinFromShortcut() {
        if (!isSpinning && participants.size() >= 2) {
            onSpin();
        }
    }

    /** Returns the active participants observable list. */
    public ObservableList<Participant> getParticipants() { return participants; }

    /** Returns the name input text field. */
    public TextField getNameInput() { return nameInput; }

    /** Returns the participant ListView. */
    public ListView<Participant> getParticipantList() { return participantList; }

    // ── Event handlers ─────────────────────────────────────────────────────────

    @FXML
    void onAddName() {
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

        // Add to participants purely in-memory (no DB call)
        Participant p = new Participant(name);
        participants.add(p);
        nameInput.clear();
        nameInput.requestFocus();
        refreshWheel();
        updateUI();
    }

    @FXML
    void onClearAll() {
        if (isSpinning) {
            return;
        }
        participants.clear();
        statusLabel.setText("");
        refreshWheel();
        updateUI();
    }


    @FXML
    void onSpin() {
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

        // Save all names currently on the wheel to the database (saved_names)
        List<String> participantNames = participants.stream()
                .map(Participant::getName)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .toList();

        if (dbManager != null && dbManager.getConnection() != null) {
            Task<Void> saveTask = new Task<>() {
                @Override protected Void call() throws Exception {
                    new SavedNamesDao(dbManager.getConnection()).recordUsage(participantNames);
                    return null;
                }
            };
            saveTask.setOnSucceeded(e -> loadSavedNamesFromDb());
            saveTask.setOnFailed(e -> {
                LOG.log(Level.WARNING, "Failed to persist participant names on spin", saveTask.getException());
                showDbErrorAlert("Save Participants on Spin", saveTask.getException());
            });
            new Thread(saveTask, "db-save-on-spin").start();
        } else {
            for (String pName : participantNames) {
                boolean found = false;
                for (SavedName sn : savedNamesMasterList) {
                    if (sn.getName().equalsIgnoreCase(pName)) {
                        sn.setLastUsedAt(LocalDateTime.now());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    savedNamesMasterList.add(new SavedName(pName, LocalDateTime.now()));
                }
            }
            savedNamesMasterList.sort(Comparator.comparing(sn -> sn.getName().toLowerCase()));
            updateSavedCountBadge();
            if (savedNamesList != null) savedNamesList.refresh();
        }

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

        if (wheelScaleGroup != null) {
            wheelScaleGroup.getChildren().add(hammer);
        } else {
            wheelContainer.getChildren().add(hammer);
        }

        SequentialTransition hit = hitAnimation.buildHit(hammer, currentWheel, () -> {
            if (wheelScaleGroup != null) {
                wheelScaleGroup.getChildren().remove(hammer);
            } else {
                wheelContainer.getChildren().remove(hammer);
            }

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
                    showDbErrorAlert("Record Ban", banTask.getException());
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

        loadTask.setOnFailed(e -> {
            LOG.log(Level.WARNING, "Failed to load banned names from DB", loadTask.getException());
            showDbErrorAlert("Load Banned History", loadTask.getException());
        });

        new Thread(loadTask, "db-load-banned").start();
    }

    /**
     * Loads all saved participant names from the {@code saved_names} table on a
     * background thread and updates {@link #savedNamesMasterList} on the FX thread.
     */
    public void loadSavedNamesFromDb() {
        if (dbManager == null || dbManager.getConnection() == null) return;

        Task<List<SavedName>> loadTask = new Task<>() {
            @Override protected List<SavedName> call() throws Exception {
                return new SavedNamesDao(dbManager.getConnection()).findAll();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<SavedName> saved = loadTask.getValue();
            Platform.runLater(() -> {
                savedNamesMasterList.setAll(saved);
                updateSavedCountBadge();
                if (savedNamesList != null) {
                    savedNamesList.refresh();
                }
            });
        });

        loadTask.setOnFailed(e -> {
            LOG.log(Level.WARNING, "Failed to load saved names from DB", loadTask.getException());
            showDbErrorAlert("Load Saved Names", loadTask.getException());
        });

        new Thread(loadTask, "db-load-saved-names").start();
    }

    /**
     * Loads the last used participants from the database on startup
     * and populates the roulette wheel participants list.
     */
    void loadLastUsedParticipantsFromDb() {
        if (dbManager == null || dbManager.getConnection() == null) return;

        Task<List<String>> loadTask = new Task<>() {
            @Override protected List<String> call() throws Exception {
                return new SavedNamesDao(dbManager.getConnection()).findLastUsedNames();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<String> lastUsed = loadTask.getValue();
            if (lastUsed != null && !lastUsed.isEmpty()) {
                Platform.runLater(() -> {
                    // Fill participants list with the last used names if currently empty
                    if (participants.isEmpty()) {
                        for (String name : lastUsed) {
                            participants.add(new Participant(name));
                        }
                        refreshWheel();
                        updateUI();
                        flashStatus(lastUsed.size() + " last used participant(s) loaded.");
                    }
                });
            }
        });

        loadTask.setOnFailed(e -> {
            LOG.log(Level.WARNING, "Failed to load last used names from DB", loadTask.getException());
        });

        new Thread(loadTask, "db-load-last-used").start();
    }

    public void updateSavedCountBadge() {
        if (savedCountBadge != null) {
            savedCountBadge.setText(String.valueOf(savedNamesMasterList.size()));
        }
    }

    private void refreshWheel() {
        wheelContainer.getChildren().removeIf(
                n -> n instanceof Group || "wheel-pointer".equals(n.getId()) || "wheel-scale-group".equals(n.getId()));

        if (participants.size() < 2) {
            if (wheelPlaceholderBox != null) {
                wheelPlaceholderBox.setVisible(true);
                wheelPlaceholderBox.setManaged(true);
            }
            wheelPlaceholder.setVisible(true);
            currentWheel = null;
            wheelScaleGroup = null;
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

            wheelScaleGroup = new Group(currentWheel, pointer);
            wheelScaleGroup.setId("wheel-scale-group");

            int overlayIdx = (banDialogOverlay != null)
                    ? wheelContainer.getChildren().indexOf(banDialogOverlay) : -1;
            if (overlayIdx >= 0) {
                wheelContainer.getChildren().add(overlayIdx, wheelScaleGroup);
            } else {
                wheelContainer.getChildren().add(0, wheelScaleGroup);
            }
            updateWheelScale();
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
        if (clearBannedBtn != null) {
            clearBannedBtn.setDisable(spinning);
        }
        if (bannedTable != null) {
            bannedTable.setDisable(spinning);
        }
        if (savedNamesList != null) {
            savedNamesList.setDisable(spinning);
        }
        if (addAllSavedBtn != null) {
            addAllSavedBtn.setDisable(spinning);
        }
        if (clearSavedBtn != null) {
            clearSavedBtn.setDisable(spinning);
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
        if (savedNamesList != null) {
            savedNamesList.refresh();
        }
        flashStatus("Removed \u201c" + p.getName() + "\u201d.");
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

    /** Returns the observable list of all saved names in the catalog. */
    public ObservableList<SavedName> getSavedNamesMasterList() { return savedNamesMasterList; }

    /** Returns the saved names ListView. */
    public ListView<SavedName> getSavedNamesList() { return savedNamesList; }

    /** Returns the saved names VBox container. */
    public VBox getSavedNamesBox() { return savedNamesBox; }

    /** Returns the banned history VBox container. */
    public VBox getBannedBox() { return bannedBox; }

    /** Returns the right sidebar VBox container. */
    public VBox getRightSidebar() { return rightSidebar; }

    /** Returns the toggle saved names button. */
    public Button getToggleSavedNamesBtn() { return toggleSavedNamesBtn; }

    /** Returns the toggle history button. */
    public Button getToggleHistoryBtn() { return toggleHistoryBtn; }

    /** Returns the active view mode of the right sidebar. */
    public RightSidebarView getCurrentRightView() { return currentRightView; }

    // ── Restore & Banned actions ──────────────────────────────────────────────

    /**
     * Removes a banned participant: removes them from the banned list
     * and clears the ban record in the database.
     */
    void onRemoveBannedName(BannedName item) {
        if (isSpinning || item == null) {
            return;
        }

        bannedList.remove(item);
        updateBannedCountBadge();
        participantList.refresh();
        if (savedNamesList != null) {
            savedNamesList.refresh();
        }
        flashStatus("Removed \u201c" + item.getName() + "\u201d from banned list.");

        if (dbManager != null && dbManager.getConnection() != null) {
            Task<Void> deleteTask = new Task<>() {
                @Override protected Void call() throws Exception {
                    new BannedDao(dbManager.getConnection()).delete(item.getId());
                    return null;
                }
            };
            deleteTask.setOnFailed(e -> {
                LOG.log(Level.WARNING, "Failed to remove ban in DB", deleteTask.getException());
                showDbErrorAlert("Remove Banned Name", deleteTask.getException());
            });
            new Thread(deleteTask, "db-delete-banned").start();
        }
    }

    /** Alias for backward compatibility. */
    void onRestoreParticipant(BannedName item) {
        onRemoveBannedName(item);
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
        if (savedNamesList != null) {
            savedNamesList.refresh();
        }
        flashStatus("Banned history cleared.");

        if (dbManager != null && dbManager.getConnection() != null) {
            Task<Void> clearTask = new Task<>() {
                @Override protected Void call() throws Exception {
                    new BannedDao(dbManager.getConnection()).deleteAll();
                    return null;
                }
            };
            clearTask.setOnFailed(e -> {
                LOG.log(Level.WARNING, "Failed to clear banned history in DB", clearTask.getException());
                showDbErrorAlert("Clear Banned History", clearTask.getException());
            });
            new Thread(clearTask, "db-clear-banned").start();
        }
    }

    // ── Right Sidebar Toggling (History vs. Saved Names) ─────────────────────

    @FXML
    public void onToggleHistory() {
        if (rightSidebar == null) return;
        if (rightSidebar.isVisible() && currentRightView == RightSidebarView.BANNED) {
            setRightSidebarView(RightSidebarView.NONE);
        } else {
            setRightSidebarView(RightSidebarView.BANNED);
        }
    }

    @FXML
    public void onToggleSavedNames() {
        if (rightSidebar == null) return;
        if (rightSidebar.isVisible() && currentRightView == RightSidebarView.SAVED_NAMES) {
            setRightSidebarView(RightSidebarView.NONE);
        } else {
            setRightSidebarView(RightSidebarView.SAVED_NAMES);
        }
    }

    /**
     * Switches the right sidebar between displaying the Banned History,
     * the Saved Names catalog, or collapsed/closed.
     */
    public void setRightSidebarView(RightSidebarView view) {
        currentRightView = view;
        switch (view) {
            case BANNED -> {
                if (rightSidebar != null) {
                    rightSidebar.setVisible(true);
                    rightSidebar.setManaged(true);
                }
                if (bannedBox != null) {
                    bannedBox.setVisible(true);
                    bannedBox.setManaged(true);
                }
                if (savedNamesBox != null) {
                    savedNamesBox.setVisible(false);
                    savedNamesBox.setManaged(false);
                }
                if (toggleHistoryBtn != null) toggleHistoryBtn.setText("Hide History");
                if (toggleSavedNamesBtn != null) toggleSavedNamesBtn.setText("📁 Saved Names");
            }
            case SAVED_NAMES -> {
                if (rightSidebar != null) {
                    rightSidebar.setVisible(true);
                    rightSidebar.setManaged(true);
                }
                if (bannedBox != null) {
                    bannedBox.setVisible(false);
                    bannedBox.setManaged(false);
                }
                if (savedNamesBox != null) {
                    savedNamesBox.setVisible(true);
                    savedNamesBox.setManaged(true);
                }
                if (toggleHistoryBtn != null) toggleHistoryBtn.setText("📜 History");
                if (toggleSavedNamesBtn != null) toggleSavedNamesBtn.setText("Hide Saved");
                loadSavedNamesFromDb();
            }
            case NONE -> {
                if (rightSidebar != null) {
                    rightSidebar.setVisible(false);
                    rightSidebar.setManaged(false);
                }
                if (bannedBox != null) {
                    bannedBox.setVisible(false);
                    bannedBox.setManaged(false);
                }
                if (savedNamesBox != null) {
                    savedNamesBox.setVisible(false);
                    savedNamesBox.setManaged(false);
                }
                if (toggleHistoryBtn != null) toggleHistoryBtn.setText("📜 History");
                if (toggleSavedNamesBtn != null) toggleSavedNamesBtn.setText("📁 Saved Names");
            }
        }
    }

    // ── Saved Names Catalog Actions ──────────────────────────────────────────

    /**
     * Adds a participant from the saved names catalog to the active roulette wheel.
     * Purely in-memory: does not call the database.
     *
     * @param name the name to add
     */
    public void onAddParticipantFromSaved(String name) {
        if (isSpinning) {
            return;
        }
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String trimmed = name.trim();
        if (participants.size() >= WheelGenerator.MAX_PARTICIPANTS) {
            flashStatus("Limit reached — max " + WheelGenerator.MAX_PARTICIPANTS + " participants.");
            return;
        }

        Participant p = new Participant(trimmed);
        participants.add(p);
        refreshWheel();
        updateUI();
        flashStatus("Added \u201c" + trimmed + "\u201d to roulette.");
        if (savedNamesList != null) {
            savedNamesList.refresh();
        }
    }

    /**
     * Deletes a name from the saved names catalog in-memory and in the database.
     *
     * @param item the saved name to remove
     */
    public void onDeleteSavedName(SavedName item) {
        if (isSpinning || item == null) {
            return;
        }
        savedNamesMasterList.remove(item);
        updateSavedCountBadge();
        flashStatus("Removed \u201c" + item.getName() + "\u201d from saved names.");

        if (dbManager != null && dbManager.getConnection() != null) {
            Task<Void> delTask = new Task<>() {
                @Override protected Void call() throws Exception {
                    new SavedNamesDao(dbManager.getConnection()).delete(item.getId());
                    return null;
                }
            };
            delTask.setOnFailed(e -> {
                LOG.log(Level.WARNING, "Failed to delete saved name from DB", delTask.getException());
                showDbErrorAlert("Delete Saved Name", delTask.getException());
            });
            new Thread(delTask, "db-delete-saved-name").start();
        }
    }

    /**
     * Adds all currently visible saved names to the active roulette wheel.
     * Purely in-memory: does not call the database.
     */
    @FXML
    private void onAddAllSavedToWheel() {
        if (isSpinning) {
            return;
        }
        int added = 0;
        List<SavedName> toAdd = (filteredSavedNames != null)
                ? List.copyOf(filteredSavedNames)
                : List.copyOf(savedNamesMasterList);

        for (SavedName sn : toAdd) {
            if (participants.size() >= WheelGenerator.MAX_PARTICIPANTS) {
                break;
            }
            String name = sn.getName();
            Participant p = new Participant(name);
            participants.add(p);
            added++;
        }
        refreshWheel();
        updateUI();
        if (savedNamesList != null) {
            savedNamesList.refresh();
        }
        flashStatus("Added " + added + " saved participant(s) to roulette.");
    }

    /**
     * Clears all saved names from the database and UI.
     */
    @FXML
    private void onClearSavedNames() {
        if (isSpinning) {
            return;
        }
        savedNamesMasterList.clear();
        updateSavedCountBadge();
        flashStatus("Cleared all saved names from database.");

        if (dbManager != null && dbManager.getConnection() != null) {
            Task<Void> clearTask = new Task<>() {
                @Override protected Void call() throws Exception {
                    new SavedNamesDao(dbManager.getConnection()).deleteAll();
                    return null;
                }
            };
            clearTask.setOnFailed(e -> {
                LOG.log(Level.WARNING, "Failed to clear saved names in DB", clearTask.getException());
                showDbErrorAlert("Clear Saved Names", clearTask.getException());
            });
            new Thread(clearTask, "db-clear-saved-names").start();
        }
    }

    /** Returns whether a participant with the given name is currently in the roulette wheel. */
    public boolean isParticipantInWheel(String name) {
        if (name == null) return false;
        for (Participant p : participants) {
            if (name.equalsIgnoreCase(p.getName())) {
                return true;
            }
        }
        return false;
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

    /**
     * Shows a user-facing error alert when a database operation fails.
     */
    public void showDbErrorAlert(String action, Throwable ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText("Database operation failed: " + action);
            alert.setContentText(ex != null && ex.getMessage() != null ? ex.getMessage() : "Unknown error");
            URL cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) {
                alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
            alert.show();
        });
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

    /**
     * Renders each saved name as a row with ONLY the name, an + Add button,
     * and a × delete button.
     */
    private static class SavedNameCell extends ListCell<SavedName> {

        private final MainController controller;

        SavedNameCell(MainController controller) {
            this.controller = controller;
        }

        @Override
        protected void updateItem(SavedName item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setOnMouseClicked(null);
            } else {
                Label nameLabel = new Label(item.getName());
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                nameLabel.getStyleClass().add("cell-name-label");

                Button addBtn = new Button("+ Add");
                addBtn.getStyleClass().add("btn-cell-add");
                addBtn.setFocusTraversable(false);
                addBtn.setOnAction(e -> {
                    e.consume();
                    controller.onAddParticipantFromSaved(item.getName());
                });

                Button deleteBtn = new Button("\u00d7");
                deleteBtn.getStyleClass().add("btn-cell-remove");
                deleteBtn.setTooltip(new Tooltip("Delete from database"));
                deleteBtn.setFocusTraversable(false);
                deleteBtn.setOnAction(e -> {
                    e.consume();
                    controller.onDeleteSavedName(item);
                });

                HBox row = new HBox(8, nameLabel, addBtn, deleteBtn);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("cell-row");
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                setOnMouseClicked(null);
                setText(null);
                setGraphic(row);
            }
        }
    }
}
