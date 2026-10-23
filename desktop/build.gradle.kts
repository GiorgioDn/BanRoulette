plugins {
    java
    application
    alias(libs.plugins.javafx)
}

// ── Coordinates ────────────────────────────────────────────────────────────────
group = "com.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

// ── JavaFX ────────────────────────────────────────────────────────────────────
javafx {
    version = libs.versions.javafx.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}

// ── Application entry-point ────────────────────────────────────────────────────
// AppLauncher is a plain main() that delegates to App.launch() to avoid the
// "Main-Class must not extend Application" restriction on the class-path.
application {
    mainClass = "com.example.app.AppLauncher"
    applicationDefaultJvmArgs = listOf(
        "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics",
        "--add-opens", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
    )
}

// ── Dependencies ───────────────────────────────────────────────────────────────
dependencies {
    implementation(project(":core"))

    // H2 embedded database
    implementation(libs.h2)

    // JUnit 5
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// ── Test configuration ─────────────────────────────────────────────────────────
tasks.test {
    useJUnitPlatform()

    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }

    // The JavaFX plugin adds the JavaFX JARs to the classpath but the test worker
    // process needs them explicitly on the --module-path so that --add-modules can
    // resolve javafx.controls and javafx.graphics.
    doFirst {
        val jfxJars = configurations.testRuntimeClasspath.get()
            .filter { it.name.contains("javafx") }
            .joinToString(File.pathSeparator)

        jvmArgs(
            "--module-path", jfxJars,
            "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics",
            "--add-opens", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
            "--add-opens", "javafx.graphics/com.sun.javafx.util=ALL-UNNAMED",
            "--add-opens", "javafx.base/com.sun.javafx.reflect=ALL-UNNAMED",
        )
    }
}

// ── JAR manifest ──────────────────────────────────────────────────────────────
tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.example.app.AppLauncher"
        )
    }
}

// ── jpackage staging ─────────────────────────────────────────────────────────
// Copies the application JAR + all runtime dependencies into
//   build/jpackage-input/
// which is the directory read by scripts/package.sh and scripts/package.ps1.
val jpackageInputDir = layout.buildDirectory.dir("jpackage-input")

val stageJpackage by tasks.registering(Copy::class) {
    dependsOn(tasks.jar)

    description = "Copies the app JAR + runtime dependencies into build/jpackage-input/ for jpackage."
    group = "distribution"

    // Runtime deps (JavaFX, H2, …)
    from(configurations.runtimeClasspath)
    // Application JAR
    from(tasks.jar.flatMap { it.archiveFile })

    into(jpackageInputDir)
}

// Make assemble stage the jpackage inputs automatically
tasks.assemble {
    dependsOn(stageJpackage)
}
