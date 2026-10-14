package com.example.ui;

import com.example.model.Participant;
import com.example.svg.WheelGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

/**
 * Controller for {@code main.fxml} — the primary application window.
 *
 * <p>Responsibilities at this stage (Step 2 skeleton):
 * <ul>
 *   <li>Manage the observable participant list.</li>
 *   <li>Validate and add / remove names.</li>
 *   <li>Delegate wheel drawing to {@link WheelGenerator} (stub until Step 4).</li>
 *   <li>Keep the status bar and spin-button state in sync.</li>
 * </ul>
 *
 * <p>Steps 3–7 will wire in the DB layer, real animations, and ban persistence.
 */
public class MainController {

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

        participants.add(new Participant(name));
        nameInput.clear();
        nameInput.requestFocus();

        refreshWheel();
        updateUI();
    }

    @FXML
    private void onClearAll() {
        participants.clear();
        statusLabel.setText("");
        refreshWheel();
        updateUI();
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
        // TODO Step 7: persist ban in DB, redraw wheel
    }

    // ── Private helpers ────────────────────────────────────────────────────────

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
