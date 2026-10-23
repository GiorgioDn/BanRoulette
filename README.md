# 🎡 Ban Roulette

[![Build & Release](https://github.com/GiorgioDn/BanRoulette/actions/workflows/deploy.yml/badge.svg)](https://github.com/GiorgioDn/BanRoulette/actions/workflows/deploy.yml)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.3-blue.svg)](https://openjfx.io/)
[![Gradle](https://img.shields.io/badge/Gradle-8.11-02303A.svg?logo=gradle)](https://gradle.org/)
[![H2 Database](https://img.shields.io/badge/Database-H2%20Embedded-informational.svg)](https://www.h2database.com/)

A modern, high-polish desktop application built with **Java 21** and **JavaFX** that adds excitement and fairness to choosing people for a ban. Spin the colorful segmented roulette wheel, watch the dramatic SVG Ban Hammer strike the selected slice, and track cumulative bans across sessions.

---

## ✨ Features

- **Dynamic SVG Roulette Wheel**:
  - Automatically draws proportional, color-curated slices with rotated name labels.
  - Slices adjust dynamically from 2 up to 40 participants.
  - Responsive layout: the wheel, pointer, and hammer automatically scale proportionally with the window.
- **Fair Physics & Smooth Spin Animation**:
  - Cryptographically secure pseudo-random winner pre-selection (`SecureRandom`).
  - Realistic multi-revolution spin with easing deceleration (`Interpolator.EASE_OUT`) and subtle pointer wobble physics.
- **Animated SVG Ban Hammer**:
  - Custom SVG ban hammer executes a multi-phase animation: wind-up $\rightarrow$ high-velocity strike $\rightarrow$ impact wheel shake $\rightarrow$ retreat.
  - Ban result dialog overlays the stage announcing the selected participant and their ban count.
- **Cumulative Ban Tracking**:
  - Storing a ban does not delete or block names from the active participants list or wheel.
  - Each time a participant is selected, their cumulative ban count increments (`ban_count`).
  - Left sidebar displays live badges (`🔨 count`) next to any participant with active bans.
  - Right sidebar provides a searchable/sortable **Banned History** view showing Name, Ban Count (`1×`, `2×`, ...), Last Ban Date/Time, and a **Remove** action button to remove records from the banned table.
- **Saved Names Catalog**:
  - Dedicated **"📁 Saved Names"** button in the bottom-right corner next to **"📜 History"**.
  - Opens in the same sidebar panel as the Banned List (toggling smoothly between History, Saved Names, and collapsed).
  - Displays all participant names saved in the database catalog with instant search filtering.
  - **Click-to-Add**: clicking the **`+ Add`** button on any saved participant adds them to the active roulette wheel.
  - An **`×`** button allows deleting names from the database catalog.
  - **Spin-to-Save Lifecycle**: adding, removing, or clearing names in the participants list is purely in-memory and does not call the database. Clicking the **⚡ Spin!** button automatically saves all wheel participants to `saved_names` in the database (adding new names and updating `last_used_at` for existing ones).
- **Keyboard Shortcuts**:
  - <kbd>Space</kbd>: Spin the wheel (when not typing in the name input).
  - <kbd>Ctrl</kbd> + <kbd>Enter</kbd> / <kbd>⌘</kbd> + <kbd>Enter</kbd>: Instant spin accelerator from anywhere.
  - <kbd>Enter</kbd>: Add participant from the text field.
- **Persistent Embedded Database**:
  - Backed by an embedded H2 database with strictly two tables (`saved_names` and `banned_names`) with automatic schema initialization and migration.
  - Graceful fallback: runs seamlessly in in-memory mode if the database file is unavailable.
- **Native Standalone Installers**:
  - Packaged via `jpackage` with bundled Java runtime — users do not need Gradle, Maven, or Java pre-installed.

---

## 🖥️ System Requirements

- **To run pre-built releases**:
  - **macOS**: macOS 12+ (Apple Silicon or Intel)
  - **Windows**: Windows 10 or 11 (64-bit)
  - **Linux**: Ubuntu 20.04+, Debian 11+, or any distribution supporting `.deb`
- **To build from source**:
  - **JDK 21** or later (e.g. Eclipse Temurin)
  - **Gradle Wrapper** (`./gradlew`) is included — no manual Gradle installation required.

---

## 🚀 Getting Started

### 1. Run Locally with Gradle

**Desktop App:**
```bash
git clone https://github.com/GiorgioDn/BanRoulette.git
cd BanRoulette
./gradlew :desktop:run
```
*(On Windows PowerShell / CMD: `.\gradlew.bat :desktop:run`)*

**Android App:**
```bash
./gradlew :android:assembleDebug
# or build signed release APK:
bash scripts/android-build.sh
```

### 2. Build and Test
To compile, validate all resources, and run the test suite across all modules:
```bash
./gradlew check
# or build everything including the desktop jpackage staging:
./gradlew :desktop:assemble
```
*(On Windows PowerShell / CMD: `.\gradlew.bat :desktop:assemble`)*

This executes all 74 unit, integration, and lifecycle tests (including in-memory H2 DAO tests, saved names catalog tests, wheel geometry tests, animation angle math tests, and schema-initialization idempotency tests).

---

## 🗄️ Database Storage

Ban Roulette stores participants, saved names, and ban history in a local, embedded H2 database file under the user's home directory:

| Operating System | Database File Location |
| :--- | :--- |
| **macOS** | `~/.banroulette/data.mv.db` |
| **Linux** | `~/.banroulette/data.mv.db` |
| **Windows** | `%USERPROFILE%\.banroulette\data.mv.db` |

### Database Characteristics
- **Idempotent Schema**: Tables `saved_names` and `banned_names` are created automatically using `CREATE TABLE IF NOT EXISTS` and migrated on startup.
- **File Locking Safety**: On application exit (`Application.stop()`), the connection is cleanly closed to release H2's file lock.
- **Resilience**: If a second instance opens or file permissions prevent database access, a user-friendly alert informs the user, and the session runs in in-memory mode without crashing.

---

## 📦 Packaging & Release Pipeline

The application uses `jpackage` to bundle the desktop application JAR, JavaFX runtime, and dependencies into self-contained native installers, and Gradle AGP to assemble the Android release APK.

### Local Packaging

Make sure `./gradlew :desktop:assemble` has been run first to stage `desktop/build/jpackage-input/`.

- **macOS (`.pkg`)**:
  ```bash
  bash scripts/package.sh
  ```
  Produces `desktop/build/installer/BanRoulette-1.0.0.pkg`.

- **Windows (`.exe`)**:
  ```powershell
  pwsh scripts/package.ps1
  ```
  Produces `desktop/build/installer/BanRoulette-1.0.0.exe` (requires WiX Toolset).

- **Linux (`.deb`)**:
  ```bash
  bash scripts/package.sh
  ```
  Produces `desktop/build/installer/banroulette_1.0.0_amd64.deb`.

- **Android (`.apk`)**:
  ```bash
  bash scripts/android-build.sh
  ```
  Produces `android/build/outputs/apk/release/android-release.apk`.

### Continuous Integration & Release (GitHub Actions)
The workflow located at [`.github/workflows/deploy.yml`](.github/workflows/deploy.yml) automatically triggers on pushes to `main`:
1. **Multi-Platform Matrix Build**: Compiles and verifies desktop on `ubuntu-latest`, `macos-latest`, and `windows-latest`, and Android on `ubuntu-latest`.
2. **Quality Gate**: Runs `./gradlew :desktop:check :desktop:assemble` and `./gradlew :android:assembleRelease` — failing tests immediately halt the pipeline.
3. **Installer Packaging**: Runs platform packaging scripts to generate `.pkg`, `.exe`, and `.deb` installers from `desktop/build/jpackage-input/`, and bundles `BanRoulette.apk`.
4. **Predictable Release Tagging**: Reads the project version from `desktop/build.gradle.kts` (e.g. `1.0.0`) and publishes a GitHub Release tagged `v1.0.0` attaching all native installer binaries and the Android APK.

> [!NOTE]
> **Bypassing Unsigned Installer Warnings**:
> - **macOS**: Right-click `BanRoulette-macos.pkg` $\rightarrow$ click **Open** $\rightarrow$ click **Open** to bypass Gatekeeper.
> - **Windows**: Click **More info** $\rightarrow$ **Run anyway** if Windows SmartScreen appears.

---

## ⌨️ Controls & Shortcuts Reference

| Action | Control / Shortcut |
| :--- | :--- |
| **Spin the Wheel** | <kbd>Space</kbd> or <kbd>Ctrl</kbd>/<kbd>⌘</kbd> + <kbd>Enter</kbd> or click ⚡ **Spin!** or click on the wheel |
| **Add Participant** | Type in name box and press <kbd>Enter</kbd> or click **+ Add** |
| **Remove Participant** | Click the **×** button on any participant row in the left sidebar |
| **Toggle History Sidebar** | Click **📜 History** in the bottom status bar |
| **Reset Ban Count** | Click **Reset** on any row in the Banned List table |
| **Clear Active Participants** | Click **Clear All** in the left sidebar |
| **Clear Ban History** | Click **Clear History** in the right sidebar |

---

## 📄 License
This project is open-source under the MIT License.
