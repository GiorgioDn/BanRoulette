# 🎡 Ban Roulette

[![Build & Release](https://github.com/GiorgioDn/BanRoulette/actions/workflows/deploy.yml/badge.svg)](https://github.com/GiorgioDn/BanRoulette/actions/workflows/deploy.yml)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.3-blue.svg)](https://openjfx.io/)
[![Jetpack Compose](https://img.shields.io/badge/Compose-2024.11.00-green.svg)](https://developer.android.com/jetpack/compose)
[![Gradle](https://img.shields.io/badge/Gradle-8.11-02303A.svg?logo=gradle)](https://gradle.org/)
[![H2 Database](https://img.shields.io/badge/Desktop%20DB-H2%20Embedded-informational.svg)](https://www.h2database.com/)
[![Room Database](https://img.shields.io/badge/Android%20DB-Room%20SQLite-success.svg)](https://developer.android.com/training/data-storage/room)

A modern, high-polish multi-platform application built for **Desktop** (Java 21 + JavaFX) and **Android** (Kotlin + Jetpack Compose) that adds excitement and fairness to choosing people for a ban. Spin the colorful segmented roulette wheel, watch the dramatic SVG Ban Hammer strike the selected slice, and track cumulative bans across sessions.

---

## 🏗️ Repository Architecture

Ban Roulette is structured as a single multi-project Gradle build sharing core business logic, wheel geometry calculations, and vector assets:

```
ban-roulette/
├── core/                  Plain Java 17 library: domain logic, models, math, shared assets & contract fixtures
├── desktop/               JavaFX 21 + H2 SQL desktop app (depends on :core)
├── android/               Kotlin + Jetpack Compose + Room SQLite Android app (depends on :core)
├── scripts/               Packaging scripts (desktop installers + local Android release builder)
└── .github/workflows/     CI/CD pipeline (tests, multi-platform installers, signed Android APK)
```

- **`:core`**: Pure Java library with **zero** platform UI or database framework dependencies. Contains models (`Participant`, `SavedName`, `BannedName`), validators (`NameValidator`), winner selection (`WinnerSelector`), wheel geometry & physics (`WheelGeometry`, `SpinCalculator`), shared palettes (`palette.json`), and shared contract test fixtures.
- **`:desktop`**: Rich JavaFX 21 UI featuring responsive window scaling, keyboard accelerators, and an embedded H2 database (`~/.banroulette/data.mv.db`).
- **`:android`**: Jetpack Compose application featuring hardware-accelerated Canvas wheel drawing, animated hammer strike layers, Room SQLite database, dark mode support, and back button navigation.

---

## ✨ Features

- **Dynamic Shared Geometry Roulette Wheel**:
  - Pure SVG path calculations from `:core` ensure the wheel appears visually identical across Desktop and Android.
  - Slices adjust dynamically from 2 up to 40 participants with adaptive font scaling and text rotation.
  - Responsive layout: the wheel, pointer, and hammer automatically scale proportionally with screen and window dimensions.
- **Fair Physics & Smooth Spin Animation**:
  - Cryptographically secure pseudo-random winner pre-selection (`SecureRandom`).
  - Realistic multi-revolution spin with easing deceleration (`Interpolator.EASE_OUT` on desktop, `FastOutSlowInEasing` via `Animatable` on Android).
- **Animated SVG Ban Hammer**:
  - Custom SVG ban hammer executes a multi-phase animation: wind-up $\rightarrow$ high-velocity strike $\rightarrow$ impact wheel shake vibration $\rightarrow$ retreat.
  - Ban result dialog overlays the screen announcing the selected participant and their cumulative ban count.
- **Cumulative Ban Tracking**:
  - Storing a ban does not delete or block names from the active participants list or wheel.
  - Each time a participant is selected, their cumulative ban count increments (`ban_count`).
  - Participant chips and lists display live badges (`X×`) next to any participant with active bans.
  - Dedicated **Banned History** view showing Name, Ban Count (`1×`, `2×`, ...), Last Ban Date/Time, and a **Remove** action button to reset records.
- **Saved Names Catalog**:
  - Searchable catalog of all names ever spun or saved in the database.
  - **Click-to-Add**: quickly add saved participants back onto the active roulette wheel.
  - **Spin-to-Save Lifecycle**: adding or editing names on the wheel is in-memory. Clicking **⚡ Spin!** records usage and persists participants to `saved_names` in the database.
  - **2-Minute Interval Resumption**: on app startup, participants from the most recent session within a 2-minute window are automatically loaded onto the wheel.

---

## 🖥️ System Requirements

- **To run pre-built releases**:
  - **macOS**: macOS 12+ (Apple Silicon or Intel)
  - **Windows**: Windows 10 or 11 (64-bit)
  - **Linux**: Ubuntu 20.04+, Debian 11+, or any distribution supporting `.deb`
  - **Android**: Android 7.0 (API 24) or newer
- **To build from source**:
  - **JDK 21** or later (e.g. Eclipse Temurin)
  - **Android SDK** (for building the `:android` module)
  - **Gradle Wrapper** (`./gradlew`) is included — no manual Gradle installation required.

---

## 🚀 Getting Started

### 1. Open and Run with Android Studio (Recommended for Mobile)

You can develop, run, and debug the Android app directly within **Android Studio** (Koala / Ladybug or newer recommended):

1. **Open the Project**:
   - Launch Android Studio.
   - Click **Open** (or **File $\rightarrow$ Open...**).
   - Select the **root repository folder** (`BanRoulette/`).
   > [!IMPORTANT]
   > **Always open the root repository folder**, not the `android/` subfolder. The Android app depends on the `:core` Java library, so opening the repository root allows Gradle to resolve `:core` and `:android` together.

2. **Sync Gradle**:
   - Allow Android Studio to finish the initial Gradle sync. The project uses the bundled Gradle Wrapper and AGP 8.7.3.
   - Make sure your Android Studio JDK is set to **JDK 17 or JDK 21** (**Settings/Preferences $\rightarrow$ Build, Execution, Deployment $\rightarrow$ Build Tools $\rightarrow$ Gradle $\rightarrow$ Gradle JDK**).

3. **Run the App**:
   - In the run configurations dropdown in the top toolbar, ensure **`android`** is selected.
   - Select an Android Virtual Device (Emulator) or a physical device running Android 7.0 (API 24) or newer.
   - Click the green **Run** button (<kbd>▶</kbd>) or press <kbd>Shift</kbd> + <kbd>F10</kbd> (<kbd>Control</kbd> + <kbd>R</kbd> on macOS).

4. **Database Inspection**:
   - While the app is running in debug mode, open **View $\rightarrow$ Tool Windows $\rightarrow$ App Inspection $\rightarrow$ Database Inspector**.
   - You can inspect tables `saved_names` and `banned_names` in real time as spins and bans occur.

5. **Run Tests in the IDE**:
   - Right-click `android/src/test/java` $\rightarrow$ **Run 'Tests in 'java''** to execute all Robolectric, Room DAO, and ViewModel unit tests directly in the IDE.

---

### 2. Run via Command Line (Gradle)

**Desktop App:**
```bash
git clone https://github.com/GiorgioDn/BanRoulette.git
cd BanRoulette
./gradlew :desktop:run
```
*(On Windows PowerShell / CMD: `.\gradlew.bat :desktop:run`)*

**Android App (Command Line):**
- Install debug APK to a connected device or running emulator:
  ```bash
  ./gradlew :android:installDebug
  ```
- Or assemble the debug APK:
  ```bash
  ./gradlew :android:assembleDebug
  ```
- Or build a signed release APK using the local helper script:
  ```bash
  bash scripts/android-build.sh
  ```

---

### 3. Build and Test Suite

To run all unit tests, contract tests, and quality gates across all modules:
```bash
./gradlew test
```

Or test specific modules:
```bash
./gradlew :core:test                    # Core algorithms, geometry math, validators
./gradlew :desktop:test                 # Desktop JavaFX and H2 contract tests
./gradlew :android:testDebugUnitTest    # Android ViewModel, Room DAOs, and Robolectric contract tests
```

#### Shared Contract Tests
Both H2 on Desktop and Room on Android inherit from shared abstract test suites exposed by `:core` via `java-test-fixtures`:
- `AbstractNameRepositoryContractTest`: proves identical participant deduplication, retrieval, and 2-minute interval queries.
- `AbstractBannedRepositoryContractTest`: proves identical atomic ban increments, sorting, and restore operations.

---

## 🗄️ Database Storage

Ban Roulette stores participants, saved names, and ban history in local embedded SQL databases with strictly two tables (`saved_names` and `banned_names`):

| Platform | Database Engine | File / Sandbox Location |
| :--- | :--- | :--- |
| **macOS** | Embedded H2 | `~/.banroulette/data.mv.db` |
| **Linux** | Embedded H2 | `~/.banroulette/data.mv.db` |
| **Windows** | Embedded H2 | `%USERPROFILE%\.banroulette\data.mv.db` |
| **Android** | Room (SQLite) | `/data/data/com.example.banroulette/databases/ban_roulette.db` |

### Database Characteristics
- **Idempotent Schema**: Tables are created automatically on first run and safely opened afterwards.
- **Non-Blocking I/O**: Android database queries execute strictly on background worker threads (`Dispatchers.IO`), never blocking the main UI thread.
- **In-Memory Fallback**: Desktop falls back to an in-memory session if file locks or permission issues prevent opening the file.

---

## 📦 Packaging & Release Pipeline

The CI/CD pipeline located at [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml) automatically runs on pushes to `main`:

1. **Quality Gates**: Runs `:core:test`, `:desktop:test`, and `:android:testDebugUnitTest`. A failure in any module immediately halts the pipeline and blocks the release.
2. **Desktop Matrix Packaging**: Uses `jpackage` to bundle JavaFX runtime and H2 dependencies into native installers (`.pkg`, `.exe`, `.deb`).
3. **Android Signing**: Builds and signs `BanRoulette.apk` using repository secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
4. **Unified GitHub Release**: Automatically publishes a single GitHub Release containing all 3 desktop installers and the Android APK with matching version tags.

### Local Desktop Packaging

Make sure `./gradlew :desktop:assemble` has been run first to stage `desktop/build/jpackage-input/`:

- **macOS (`.pkg`)**: `bash scripts/package.sh` $\rightarrow$ `desktop/build/installer/BanRoulette-1.0.0.pkg`
- **Windows (`.exe`)**: `pwsh scripts/package.ps1` $\rightarrow$ `desktop/build/installer/BanRoulette-1.0.0.exe`
- **Linux (`.deb`)**: `bash scripts/package.sh` $\rightarrow$ `desktop/build/installer/banroulette_1.0.0_amd64.deb`

### Distribution Note
- **Desktop Installers**: Unsigned standalone installers (right-click $\rightarrow$ Open on macOS Gatekeeper, or "More info" $\rightarrow$ "Run anyway" on Windows SmartScreen).
- **Android APK**: Distributed directly via GitHub Releases. Users must enable "Install unknown apps" in Android settings. (Google Play Store distribution requires an AAB bundle and developer account).

---
