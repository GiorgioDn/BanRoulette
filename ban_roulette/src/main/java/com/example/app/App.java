package com.example.app;

import com.example.db.DatabaseManager;
import com.example.ui.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
 *   <li>{@link #start(Stage)} — loads the FXML, injects the DB manager into
 *       the controller, and shows the window.</li>
 *   <li>{@link #stop()} — disconnects the DB so H2 releases its file lock.</li>
 * </ol>
 *
 * <p>This class is <em>not</em> the jpackage main class — use
 * {@link AppLauncher} instead (see comments there for why).
 */
public class App extends Application {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private final DatabaseManager dbManager = new DatabaseManager();

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
            LOG.log(Level.SEVERE, "Failed to open database", ex);
            // The window will still open; the controller handles a null DB gracefully.
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

        URL cssUrl = App.class.getResource("/css/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("Ban Roulette");
        stage.setMinWidth(820);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
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
