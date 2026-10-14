package com.example.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * JavaFX application entry point for Ban Roulette.
 *
 * <p>Loads the main FXML layout, attaches the global stylesheet,
 * and configures the primary {@link Stage}.
 *
 * <p>This class is <em>not</em> the jpackage main class — use
 * {@link AppLauncher} instead (see comments there for why).
 */
public class App extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        URL fxmlUrl = App.class.getResource("main.fxml");
        if (fxmlUrl == null) {
            throw new IOException("Cannot find main.fxml on the classpath");
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(loader.load());

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

    /** Called by {@link AppLauncher#main(String[])} */
    public static void main(String[] args) {
        launch(args);
    }
}
