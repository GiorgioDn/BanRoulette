package com.example.app;

import com.example.db.DatabaseManager;
import com.example.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX application entry point for Ban Roulette.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link #init()} — opens the H2 database (off the FX thread).</li>
 *   <li>{@link #start(Stage)} — loads the FXML, injects DB manager, registers
 *       keyboard shortcuts and application icon, and shows the window.</li>
 *   <li>{@link #stop()} — disconnects the DB cleanly to release H2's file lock.</li>
 * </ol>
 */
public class App extends Application {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private final DatabaseManager dbManager = new DatabaseManager();
    private Exception initDbError;

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    /**
     * Called by the JavaFX platform before {@link #start(Stage)}, on a
     * non-FX thread — the ideal place to open the database.
     */
    @Override
    public void init() {
        try {
            dbManager.connect();
        } catch (SQLException ex) {
            initDbError = ex;
            LOG.log(Level.SEVERE, "Failed to open database", ex);
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        URL fxmlUrl = App.class.getResource("main.fxml");
        if (fxmlUrl == null) {
            throw new IOException("Cannot find main.fxml on the classpath");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(loader.load());

        // Inject the DB manager into the controller before the window is shown
        MainController controller = loader.getController();
        controller.setDatabaseManager(dbManager);

        // Stylesheet
        URL cssUrl = App.class.getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Application Icon (Step 8 polish)
        URL iconUrl = App.class.getResource("/icon.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        // Keyboard Shortcuts for spinning (Step 8 polish):
        // 1. Accelerator: Ctrl+Enter (Win/Linux) or Cmd+Enter (macOS)
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN),
                controller::triggerSpinFromShortcut
        );

        // 2. Space key: spins when not typing in the text field
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                Node focused = scene.getFocusOwner();
                if (!(focused instanceof TextField)) {
                    controller.triggerSpinFromShortcut();
                    event.consume();
                }
            }
        });

        stage.setTitle("Ban Roulette");
        stage.setMinWidth(960);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> stop());
        stage.show();

        // Error dialog for DB initialization failure (Step 8 polish)
        if (initDbError != null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(stage);
            alert.setTitle("Database Warning");
            alert.setHeaderText("Database Offline (In-Memory Mode)");
            alert.setContentText("Could not connect to the database:\n" + initDbError.getMessage() +
                    "\n\nBan Roulette will run in in-memory mode for this session.");
            if (cssUrl != null) {
                alert.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            }
            alert.show();
        }
    }

    /**
     * Called by the JavaFX platform when the last window closes.
     * Closes the database so H2 releases its file lock.
     */
    @Override
    public void stop() {
        dbManager.disconnect();
    }

    /** Called by {@link AppLauncher#main(String[])} */
    public static void main(String[] args) {
        launch(args);
    }
}
