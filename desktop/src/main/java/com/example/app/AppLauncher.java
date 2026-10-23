package com.example.app;

/**
 * jpackage-compatible entry point for Ban Roulette.
 *
 * <p>When the application jar is bundled by {@code jpackage}, the JVM
 * looks for the {@code Main-Class} manifest attribute to start the app.
 * If that class extends {@link javafx.application.Application} (as
 * {@link App} does), the JVM cannot find the JavaFX bootstrap and throws
 * an error on non-modular class-path launches.
 *
 * <p>The fix is a thin launcher class that does <em>not</em> extend
 * {@code Application}. {@code jpackage} and the manifest both point here;
 * this class simply delegates to {@link App#main(String[])}.
 */
public class AppLauncher {
    public static void main(String[] args) {
        App.main(args);
    }
}
