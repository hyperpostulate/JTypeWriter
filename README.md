<p align="center">
  <img src="src/main/resources/icons/jtypewriter-512.png" alt="JTypeWriter" width="512" height="512">
</p>

<h1 align="center">JTypeWriter</h1>

<p align="center">
  <a href="https://github.com/hyperpostulate/JTypeWriter/blob/main/pom.xml"><img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java 25"></a>
  <img src="https://img.shields.io/badge/Platforms-Linux%20%7C%20Windows%20%7C%20macOS-lightgrey.svg" alt="Platforms">
  <img src="https://img.shields.io/badge/Languages-6-blue.svg" alt="6 languages">
  <a href="https://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License: Apache 2.0"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml/badge.svg" alt="Java CI"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhyperpostulate%2FJTypeWriter%2Fgh-pages%2Fbadges%2Ftests.json" alt="Tests"></a>
</p>

<p align="center">A quiet, offline desktop text editor for creative writers.</p>
<p align="center">It sounds like a typewriter, counts what you write, and makes losing that writing very hard.</p>

<p align="center">Türkçe sürüm: <a href="README-TR.md">README-TR.md</a></p>

JTypeWriter opens a single plain-text document and then stays out of the way: no accounts, no cloud, no project format. Your text lives in the file you picked, in the encoding that file already had. The packaged download carries its own Java runtime, so nothing has to be installed first; building from source or running the fat JAR needs JDK 25 and Maven 3.8+. It runs on Linux, Windows and macOS, and the interface speaks English, Turkish, German, French, Spanish and Italian.

**What you get?**

- **A draft you cannot wreck by accident.** YOLO mode turns off Backspace, Delete, Cut and Undo/Redo so you can only move forward. Every save is atomic and keeps rotating backups, and unsaved work is snapshotted every 30 seconds - after a crash the draft is offered back, and closing that prompt never deletes it.
- **The feel of typing.** A typewriter keystroke for every key you press, three themes (dark, light, sepia) and six text colours, all switchable while you write.
- **Numbers as you go.** Words, characters with and without spaces, lines, keystrokes, session time and a sustained words-per-minute figure that ignores your breaks.
- **Only text on screen.** `F11` goes fullscreen, `Ctrl+T` hides the toolbar, and the window title tells you which document is open and whether it is saved.
- **A session that comes back.** Language, theme, text colour, font size, last document and window geometry are restored on the next start.

---

## Table of Contents

- [Features](#features)
- [Your Data](#your-data)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Themes](#themes)
- [Supported Languages](#supported-languages)
- [Requirements](#requirements)
- [Installation](#installation)
- [Build & Run](#build--run)
- [CI/CD](#cicd)
- [Architecture](#architecture)
- [Testing](#testing)
- [License](#license)
- [Developer](#developer)
- [Contributing](#contributing)

---

## Features

### Typewriter Sound Effects

Authentic typewriter keystroke sound on every non-modifier key press. Can be toggled on/off from the toolbar's Sound dropdown.

### YOLO Mode

"YOLO Mode" (You Only Live Once) prevents accidental deletion. When enabled, Backspace, Delete, Cut and Undo/Redo are disabled (the right-click menu shows Cut greyed out, with only Copy/Paste/Select All usable), so no edit can remove characters that are already in the document. Perfect for first-draft writing where you push forward without looking back. The mode stays visible through the `[YOLO]` title badge and the toolbar's YOLO dropdown; theme colours stay untouched in both states.

### Real-Time Statistics

Live stats bar showing:
- Word count, character count (with/without spaces), line count
- Total keystrokes
- Session duration (HH:MM:SS)
- Words per minute (WPM) - sustained speed based on active typing time, so breaks do not drag it down
- The status bar text is 13 px by default and cycles between small, medium and large when you double-click it; its colour meets WCAG AA contrast (at least 4.5:1) in all three themes
- A successful save stamps the status bar with its time, e.g. `Saved 12:04`; it clears on the next edit, and it marks a real save rather than the 30-second autosave draft snapshot

### Fullscreen & Toolbar

- **`F11` takes over the screen, `Escape` gives it back:** `F11` toggles fullscreen and `Escape` leaves it (the platform's default exit hint is suppressed)
- **The toolbar can disappear completely:** `Ctrl+T` removes it when you want nothing but text on screen
- **The toolbar never wraps to a second row:** at every width it stays one horizontal row. At the tested window sizes the six dropdowns show a caption with their value ("Theme: Dark", "Size: 16", "Language: Deutsch") and nothing overflows - fully captioned at 1280 px, captions collapsed to the bare value ("Dark", "16", "Deutsch") at 900 px in all six languages, measured from the actual localised labels. Below the collapsed row's own width the dropdowns keep shrinking and can clip, because the window has no minimum width. The five buttons never collapse and always stay visible
  - **Every control is reachable and explained.** `Tab` follows the pinned visual order: Open, Save, Undo, Redo, Theme, YOLO, Sound, Color, Size, About, Language. The order is set with `viewOrder`; JavaFX documents that property for rendering and picking, so the focus order is an application convention rather than a toolkit guarantee. Each control keeps a visible focus ring and a tooltip from the UI language that includes the shortcut where one exists, such as "Save (Ctrl+S)"
- **Your layout is remembered:** the toolbar's shown or hidden state, fullscreen and the status bar's size all survive a restart, and a choice is saved the moment you toggle it rather than only on exit
  - **Fullscreen never fights the window geometry.** A window left in fullscreen comes back in fullscreen; fullscreen wins at startup over the remembered position and size, and leaving it later lands on the remembered position

### Data Safety & Crash Recovery

- **Atomic, durable saves with backups:** every save is written to a temp file, fsynced and then renamed into place, so a crash mid-save or a power cut cannot leave a truncated document
  - **The fallback is still safe.** When the file system does not support an atomic move, the rename degrades to a plain move with the bytes already on disk.
  - **Overwrites keep history and permissions.** Rotating backups (`file.bak`, `.bak.1`, `.bak.2`) are kept and the document's existing file permissions are preserved.
- **Document awareness:** the window title shows the open document name and a `*` while there are unsaved changes (alongside the `[YOLO]` badge)
  - **A new document is a real decision.** `Ctrl+N` starts a fresh untitled document - after the same save/discard/cancel prompt as opening a file - and it also works while YOLO mode is on.
  - **Undo stops at the document boundary.** Loading or creating a document clears the undo history, so `Ctrl+Z` can never bring back the previous document's text.
- **Autosave & recovery:** unsaved work is snapshotted every 30 seconds and on focus loss to `~/.jtypewriter/recovery/`, and a draft left by a crash is offered for restore at the next start. The prompt offers restore, discard and later; closing it never deletes anything.
  - **Deleting is the exception.** The draft is deleted only when the window's close request was answered in favour of closing - the document was saved, or you explicitly chose to discard it. Every other way out (`Platform.exit()`, a session log-out, the window simply hiding) parks it as `pending-<uuid>.jtw`, so the next start offers it again.
  - **You keep writing.** A postponed draft leaves the live slot free, so autosave keeps running for the document you are editing; if it cannot be parked, the slot is frozen and the file is left untouched.
  - **A stale draft cannot roll the document back.** When the file on disk is strictly newer than the draft (equal timestamps are not stale), the text is still restored - recoverable text is never discarded - but without its original path, so the next save forces Save As.
  - **Failures are visible.** A recovery write that fails keeps a status-bar warning until a later write succeeds and raises one dialog with what to do. A delete that fails gets its own warning, which clears on the next successful delete, and no dialog - the dialog stays reserved for a copy that cannot be written.
- **Encoding safety:** a BOM-marked file is decoded with the encoding of its BOM; a file without a BOM must be valid UTF-8 or carry the UTF-16 byte pattern, otherwise you are asked whether it is Windows-1254 or UTF-8 instead of getting silent corruption
  - **A mismatched byte-order mark is a question, not a guess.** A BOM that declares UTF-8 or UTF-16 while some bytes are invalid for that encoding is not decoded silently: the app says the mark does not match the content, names the declared encoding and the file, and asks before opening. Opening anyway shows the broken bytes as replacement characters and logs it; cancelling leaves the current document untouched
  - **Saves keep the encoding.** Documents are written back in the same encoding unless you accept the offered switch to UTF-8 when the text no longer fits (e.g. a pasted emoji in a Windows-1254 file) - unmappable characters are never silently replaced with `?`
  - **A BOM-less UTF-16 file is recognised, not guessed or rewritten.** A document without a byte-order mark whose bytes follow the UTF-16 parity pattern is opened as UTF-16LE or UTF-16BE (and logged) instead of being accepted as NUL-heavy UTF-8 and rewritten on the next save; text that fits neither pattern still gets the Windows-1254/UTF-8 question rather than a silent assumption
- **Error visibility:** unexpected errors are logged to `~/.jtypewriter/logs/` with their full stack trace (including the cause chain); uncaught crashes and a recovery draft that cannot be written or parked also open an error dialog instead of failing silently.
  - **Some paths log only.** A failed settings write, a failing keystroke sound and a failed undo-history reset are logged without a dialog; a failed draft delete raises a status-bar warning, not a dialog.
  - **Bad state is set aside, not overwritten.** A corrupt settings file is moved aside as `settings.properties.corrupt` and its content kept for diagnostics.
  - **Logging cannot block startup.** If the log file cannot be created - a home directory the JVM denies or a read-only log location - the app starts with console logging only.

### Settings & Session Restore

- **What is remembered:** language, theme, text colour, font size, sound, the last document and its folder, and the window geometry are persisted to `~/.jtypewriter/settings.properties`, so the next session starts where the last one ended - same look, same document, same window
  - **Nothing blocks startup.** A document that has disappeared in the meantime is skipped silently, and a broken settings file falls back to the defaults instead of blocking startup
  - **YOLO mode is deliberately *not* persisted.** A mode that blocks deletions should not still be armed at the next start

### Single Instance

Only one JTypeWriter instance runs per user account: a lock file in `~/.jtypewriter/`
stops a second start, which then shows a localized warning popup and exits instead of
opening another window. The operating system releases the lock automatically if the
running instance crashes, so a crash never locks you out.

### Multiple Themes

Three built-in themes. Dark is the default, and `Ctrl+Shift+T` cycles Dark → Sepia → Light (the toolbar's Theme dropdown picks any of them directly):
- **Light** - Clean and bright
- **Dark** - Default, easy on the eyes
- **Sepia** - Warm, paper-like feel

### Text Colors

Six text colors (one default plus five accents): Default, Sage, Lavender, Sand, Steel, Rose.

### Multi-Language Support

6 languages: English, Turkish, German, French, Italian, Spanish. Toolbar, dialogs, the window title and the status bar update immediately when the language is switched.

---

## Your Data

Everything JTypeWriter keeps stays on your machine. There is no account, no telemetry, no update check and no network request: the application itself sends nothing, and the only outbound action is the e-mail link in the About dialog, which hands a `mailto:` address to your own mail client.

- **Where it lives:** `~/.jtypewriter/` - `settings.properties` (language, theme, text colour, font size, sound, toolbar visibility, status-bar size, window geometry, and the last document and folder as absolute paths), `recovery/` (crash-recovery drafts), `logs/`, and `instance.lock`, the advisory single-instance lock that carries no document content.
- **Drafts contain your text.** Each `recovery/draft-<uuid>.jtw` holds the unsaved document verbatim; a postponed draft is parked as `pending-<uuid>.jtw` and survives a clean exit. A draft also records the absolute path the text came from, Base64-encoded so separators and newlines survive - that is encoding, not encryption - so a draft can outlive the document it came from.
- **Next to your documents:** an overwrite keeps `<document>.bak`, `.bak.1` and `.bak.2`, and a save stages a `jtw-<name>*.tmp` file beside the document before renaming it into place. Both contain document text.
- **Logs:** `logs/jtypewriter-0.log` to `jtypewriter-2.log` - three rotating generations of up to one megabyte each. They hold file paths, error messages and full stack traces.
- **Deleting all of it:** quit the application first, otherwise the log handler and `instance.lock` are recreated, then delete `~/.jtypewriter` and any `<document>.bak*` or leftover `jtw-*.tmp` next to your documents. There is no in-app "delete all data" command, and uninstalling the application does not remove this data - removing an installed app image or package leaves `~/.jtypewriter` in place.

---

## Keyboard Shortcuts

Shortcuts use `Ctrl` on Windows/Linux and `⌘` (Cmd) on macOS — plain `Ctrl` works on macOS too. Numpad `+`, `-` and `0` are accepted as equivalents of their main-keyboard neighbours.

| Shortcut | Action |
|----------|--------|
| `Ctrl+T` | Toggle toolbar |
| `Ctrl+Shift+T` | Cycle through the three themes (Dark → Sepia → Light) |
| `Ctrl+Shift+Y` | Toggle YOLO mode |
| `Ctrl+Shift+C` | Cycle text color |
| `Ctrl++` | Increase font size |
| `Ctrl+-` | Decrease font size |
| `Ctrl+0` | Reset font size to 16px |
| `Ctrl+Z` | Undo |
| `Ctrl+Shift+Z` | Redo |
| `Ctrl+Y` | Redo |
| `Ctrl+N` | New document |
| `Ctrl+O` | Open file |
| `Ctrl+S` | Save file |
| `Ctrl+Shift+S` | Save file as |
| `F11` | Toggle fullscreen |
| `Escape` | Exit fullscreen |

---

## Themes

CSS files are located in `src/main/resources/css/`:

| Theme | File | Description |
|-------|------|-------------|
| Base | `base.css` | Shared styles across all themes |
| Dark | `dark.css` | Dark background, light text |
| Light | `light.css` | Light background, dark text |
| Sepia | `sepia.css` | Warm sepia-toned background |

---

## Supported Languages

| Language | File | Code |
|----------|------|------|
| English | `messages.properties` | `EN` |
| Turkish | `messages_tr.properties` | `TR` |
| German | `messages_de.properties` | `DE` |
| French | `messages_fr.properties` | `FR` |
| Italian | `messages_it.properties` | `IT` |
| Spanish | `messages_es.properties` | `ES` |

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Packaged application | No Java required (bundled JDK runtime) |
| Building from source / fat JAR | Java 25+, Maven 3.8+ |

---

## Installation

### Download (end users)

Download the installer (`.deb`, `.msi`, `.dmg`) or the portable `.tar.gz` from
the GitHub Releases assets, together with the `SHA256SUMS` file that lets you
verify it. Both channels carry a bundled runtime, so neither needs Java
installed. The `git clone` and `mvn clean install` steps below are the
developer path, not the user path.

### Manual Build

```bash
git clone https://github.com/hyperpostulate/JTypeWriter.git
cd JTypeWriter
mvn clean install
```

---

## Build & Run

```bash
mvn clean javafx:run                 # Build and launch the application
mvn clean test                       # Run all tests
mvn clean package                    # Build + tests + thin JAR + fat JAR
./scripts/package.sh                 # Build + wrap into a native app image (releases/<os>/)
./scripts/package.sh --skip-build    # Reuse the last build, only re-wrap it
./scripts/package.sh --installer     # Same, plus the platform installer (.deb / .msi / .dmg)
mvn test -Dtest=SessionStatsTest     # Run a single test class
mvn test -Dtest=FileServiceTest      # Run a single test class
```

Packaging produces two JARs: a thin one (application classes only) and a
`jar-with-dependencies` that bundles JavaFX together with its native libraries.
Because those natives come from the build machine, the fat JAR runs only on the
platform it was built on - it is a convenience artifact, not a cross-platform
distribution; `scripts/package.sh` builds a native image, and because it selects
the host platform it has to be run on each target platform - the release
workflow runs it once per OS. `mvn javafx:run` is the development entry point.
`scripts/package.sh` wraps the fat JAR into a native application image with a
bundled runtime via `jpackage`, and `--installer` wraps that image into the
platform installer (`.deb`, `.msi`, `.dmg`) for Linux, Windows and macOS - the same
command the CI workflow runs. Every release publishes both channels for each
platform, the installer and a portable `.tar.gz`; which one becomes the product
default is still an open decision, so `--installer` is one supported path, not
the decided one. The application version has a single source of truth:
`pom.xml`. The script reads it back from the built artifact and validates it
against jpackage's rules (macOS, for one, rejects `0.x` bundle versions).

A wrong toolchain stops the build before it starts: `maven-enforcer-plugin` runs
at the `validate` phase, requires a JDK at least as new as `java.version` in
`pom.xml` (25) and Maven 3.8 or newer, and names the missing piece in one line
instead of failing later with a compiler error about `release 25`.

The launcher, window and taskbar icon is generated from the single source of the
artwork, `src/main/resources/icons/jtypewriter.svg`: `scripts/generate-icons.py`
regenerates the PNG set plus the Windows `.ico` and macOS `.icns` (`--check` verifies
they are current), and `scripts/package.sh` picks the right format per platform.

---

## CI/CD

GitHub Actions workflows: build and test on every push and pull request to `main` (`.github/workflows/maven.yml`), release on a version tag (`.github/workflows/release.yml`).

- **Trigger**: Runs on every push and pull request to `main`
- **Environment**: Ubuntu, Windows and macOS (build matrix)
- **JDK**: Temurin 25
- **Command**: `mvn -B package` (build + tests), then `scripts/package.sh --skip-build --installer`
- **Locales**: the test suite also runs under Turkish and German system locales, so i18n regressions (e.g. a language that silently resolves to the system language) cannot slip through
- **Artifacts**: native application image plus a platform installer (`.deb` / `.msi` / `.dmg`) per OS
- **Releases**: pushing a `v*` tag that matches the `pom.xml` version builds all three platforms and publishes them to a GitHub Release together with a `SHA256SUMS` file, so the binaries no longer live in Actions artifacts that expire after a few days

---

## Architecture

```
org.mesutormanli.jtypewriter
├── audio/
│   └── TypewriterSound.java              # Typewriter keystroke sound effect
├── locale/
│   ├── Language.java                     # Supported languages enum
│   ├── LocaleManager.java                # Language switching + listener support
│   └── Messages.java                     # ResourceBundle-based i18n accessor
├── service/
│   ├── AutoSaveService.java              # Periodic draft snapshots + recovery offer
│   ├── AutosaveHealth.java               # Sticky autosave failure state (DRAFT_WRITE / DRAFT_PARK / DRAFT_DELETE)
│   ├── BackupKeeper.java                 # Rotating .bak backups before overwrites
│   ├── DialogService.java                # FileChooser & Alert dialog management
│   ├── ExitDisposition.java              # Exit-path rule for the recovery draft (CLEAR / PARK / KEEP)
│   ├── FileService.java                  # Atomic file I/O (encoding-stable)
│   ├── RecoveryChoice.java               # Answer to the "recover draft" prompt (restore / discard / later)
│   ├── RecoveryStore.java                # Crash-recovery draft storage
│   └── TextCodec.java                    # Encoding detection & lossless round trips
├── stats/
│   └── SessionStats.java                 # Writing session statistics (WPM, counts)
├── ui/
│   ├── MainController.java               # Root layout controller
│   ├── MainStage.java                    # Stage configuration (fullscreen, close)
│   ├── AppIcons.java                     # Window/taskbar/dock icon set from /icons/
│   ├── WelcomeDialog.java                # Welcome screen with shortcuts
│   ├── AboutDialog.java                  # About dialog with author info
│   ├── KeyboardShortcutHandler.java      # Centralized keyboard shortcut handling
│   ├── CrashHandler.java                 # Global crash net: log + error dialog
│   ├── SingleInstanceWarning.java        # "Already running" warning window
│   ├── component/
│   │   ├── EditorArea.java               # TextArea enforcing the YOLO policy
│   │   ├── YoloPolicy.java               # YOLO mode edit policy (pure logic)
│   │   ├── EditorKeyHandler.java         # Keystroke sound + stats tracking
│   │   ├── UndoHistoryReset.java         # Clears undo/redo on document load (reflectively)
│   │   ├── StatsBar.java                 # Bottom status bar (clock + stats)
│   │   ├── StatsBarState.java            # Persisted status-bar size (non-Node state)
│   │   ├── ToolbarView.java              # Top toolbar (file/edit buttons + settings dropdowns)
│   │   ├── ToolbarFit.java               # Toolbar overflow policy (pure, headless measurement)
│   │   └── ToolbarState.java             # YOLO mode + sound on/off state
│   └── theme/
│       ├── Theme.java                    # Theme enum (Light/Dark/Sepia)
│       ├── ThemeManager.java             # CSS stylesheet loading & switching
│       ├── FontSizeManager.java          # Editor font size state
│       ├── TextColorManager.java         # Text color cycling state
│       └── TextColor.java                # Text color enum with CSS values
├── JTypeWriterApp.java                   # Main entry point
├── AppDirs.java                          # Per-user data/log/recovery/settings directories
├── AppGraph.java                         # Composition root (manual dependency injection)
├── AppLogging.java                       # Rotating log setup (java.util.logging)
├── AppSettings.java                      # Persisted preferences (settings.properties)
├── AppVersion.java                       # App version read from META-INF/maven pom.properties
├── AtomicFiles.java                      # Durable atomic writes (fsync + rename)
├── InstanceLock.java                     # Per-user instance lock (single instance)
└── FxApplication.java                    # JavaFX application lifecycle bridge
```

### Core Components

| Component | Description |
|-----------|-------------|
| `EditorArea` | TextArea subclass that routes every edit through the YOLO policy; loads documents through `setDocument` |
| `UndoHistoryReset` | Clears the TextArea undo/redo history on every document load (reflective, JavaFX has no API) |
| `YoloPolicy` | YOLO mode edit policy (blocks deletions, allows insertions) |
| `EditorKeyHandler` | Handles keystroke sound effects and session stats tracking |
| `StatsBar` | Renders the clock, the *saved HH:mm* confirmation and live session statistics in the bottom bar; shows a persistent warning naming the autosave failure, with separate text for a stale draft, and its text size cycles on double-click |
| `KeyboardShortcutHandler` | Centralized keyboard shortcut routing |
| `ToolbarView` | Top toolbar with all editor controls |
| `ToolbarFit` | Toolbar overflow policy: pure, JavaFX-free measurement that collapses the dropdown captions when the captioned row would not fit |
| `ToolbarState` | Manages YOLO mode and sound on/off state |
| `StatsBarState` | Persisted status-bar size; non-Node state over a neutral `AppSettings.StatsBarSize` |
| `AppIcons` | Window, taskbar and dock icon set loaded from `/icons/`; a missing asset never breaks startup |
| `FileService` | Atomic file I/O (temp write + fsync + move) with encoding stability |
| `DialogService` | UI dialog management (FileChooser, Alert) |
| `TextCodec` | Encoding detection (BOM/UTF-8), strict decoding that refuses to replace bytes with `U+FFFD`, and lossless round trips |
| `BackupKeeper` | Rotating `.bak` backups taken before overwrites |
| `RecoveryStore` | Crash-recovery draft storage: one atomic `draft-<uuid>.jtw` file per session under `~/.jtypewriter/recovery/`, plus `pending-<uuid>.jtw` for a draft the user postponed; a failed delete is reported at `WARNING` and returns `false` |
| `RecoveryChoice` | Answer to the recovery prompt: restore, discard or later; `LATER` is the safe default that neither deletes nor restores |
| `ExitDisposition` | Pure exit matrix for the recovery draft: `CLEAR` when a close request was answered in favour of closing, `PARK` for an unasked exit, `KEEP` while a postponed draft could not be parked; a frozen slot wins over every other signal |
| `AutosaveHealth` | JavaFX-free, sticky autosave failure state (`DRAFT_WRITE` / `DRAFT_PARK` / `DRAFT_DELETE`) whose listeners fire only on real transitions |
| `AutoSaveService` | Periodic draft snapshots, the startup recovery offer, the exit rule that clears the draft only after an answered close prompt, reporting of autosave failures to `AutosaveHealth`, and a stale-draft path guard: a draft older than its document is restored without its path so a save cannot overwrite the newer file |
| `CrashHandler` | Global crash net: logging plus user-facing error dialog |
| `InstanceLock` | Advisory file lock that rejects a second instance per user |
| `SingleInstanceWarning` | Minimal window that shows the "already running" warning and exits |
| `AppSettings` | Persisted preferences under `~/.jtypewriter/settings.properties` |
| `AppVersion` | Reads the application version from `META-INF/maven/org.mesutormanli/jtypewriter/pom.properties` for the About dialog |
| `AtomicFiles` | Durable atomic file writes (fsync, rename, directory flush) |
| `ThemeManager` | CSS theme loading and switching |
| `FontSizeManager` | Editor font size state (10-48px) |
| `TextColorManager` | Text color cycling state |
| `SessionStats` | Real-time writing statistics (WPM, word/char/line count) |
| `TypewriterSound` | Typewriter keystroke audio playback |
| `LocaleManager` | Multi-language support with change listeners |
| `Messages` | Resource bundle-based message accessor |

### Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| JavaFX Controls | 26.0.1 | UI components |
| JavaFX FXML | 26.0.1 | FXML loading |
| JavaFX Media | 26.0.1 | Audio playback |
| JUnit Jupiter | 6.0.3 | Test framework |
| Mockito | 5.23.0 | Test mocking |

JavaFX Graphics and Base come in transitively with Controls. Dependency injection is framework-free: `AppGraph` wires the object graph manually.

---

## Testing

### Running Tests

```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=SessionStatsTest

# Single test method
mvn test -Dtest=SessionStatsTest#wordCountIsCorrect
```

### Test Classes

| Test Class | Coverage |
|------------|----------|
| `SessionStatsTest` | Word count, char count, line count, keystrokes, reset, WPM from active typing time (idle decay regression) |
| `FileServiceTest` | Unsaved-change handling on open and on a new document (cancel/discard/save-first), atomic writes, rotated backups, encoding round trips incl. the UTF-8 upgrade for unrepresentable characters, encoding reset for a new document, lifecycle listeners, recovery attachment |
| `TextCodecTest` | Encoding detection (BOMs, strict UTF-8), legacy prompts, lossless encoding round trips, strict encode (unrepresentable characters fail instead of turning into `?`) |
| `EncodingMatrixTest` | The open→save and open→edit→save byte matrix per encoding (BOM-less UTF-16LE/BE, Windows-1254 via the legacy prompt, mixed UTF-8 with CRLF, tab and emoji), pinning that a valid-UTF-8 file with NULs on both parities stays UTF-8 and that BOM-less UTF-16 never reaches the legacy prompt |
| `BackupKeeperTest` | Pre-overwrite backup rotation and retention |
| `RecoveryStoreTest` | Crash-recovery draft lifecycle (save/restore/drop) in one atomic per-session `.jtw` file, newest-draft adoption, isolation between concurrent sessions, parking a postponed draft so the live slot stays free, tolerance for corrupt headers, bad timestamps, unusable paths and invalid UTF-8; a read-only directory makes `clear()` report failure and log a `WARNING` |
| `AutoSaveServiceTest` | Opening or creating a document drops the previous document's stale recovery draft; a postponed draft survives an exit and no longer silences autosave of the new document (regression); an answered exit clears the draft, an unasked exit parks it, a frozen slot keeps the file untouched, and failed snapshots or parked drafts report health; a failed delete keeps the warning without a dialog and a later successful delete withdraws it; a draft older than the document on disk is restored without its path |
| `ExitDispositionTest` | The four-way exit matrix: answered vs unasked exit against a live vs frozen slot, with a frozen slot winning over every other signal |
| `AutosaveHealthTest` | Sticky failure state (`DRAFT_WRITE` / `DRAFT_PARK` / `DRAFT_DELETE`) that survives repeated failures, clears on a successful write and notifies once per real transition |
| `ProcessCrashRecoveryTest` | Linux-only end-to-end kill test: `CrashHarness` (a helper, not a test class) builds the real service graph in a child JVM without starting JavaFX and snapshots a dirty document through the production autosave path; the parent then kills it with `SIGKILL` (`Process.destroyForcibly`) and a second JVM must recover the exact non-ASCII text. A paired case proves an answered close still drops the draft, and `-Duser.home` keeps logging inside a temp home. Synchronisation is the stdout marker plus `waitFor`, never `Thread.sleep` |
| `YoloPolicyTest` | YOLO mode edit policy (deletions blocked, insertions allowed, safe replacement) |
| `KeyboardShortcutHandlerTest` | Shortcut decoding incl. shift-sensitive combos (Ctrl+Shift+Y / Z / C / T / S), Ctrl+N for a new document and numpad equivalents |
| `MainControllerTest` | Window title composition (application + document name, unsaved `*` marker, `[YOLO]` badge) |
| `MainStageTest` | Off-screen window position guard (`intersectsAny`: restored positions on detached monitors) |
| `AppIconsTest` | The icon set on the classpath: every PNG decodes to the declared size, and the `.ico`/`.icns` files start with their expected magic bytes (pure byte checks, no toolkit) |
| `AppSettingsTest` | Preference persistence across restarts, restored toolbar/fullscreen/status-bar-size state and their corrupt-value fallbacks, defaults, tolerance for unknown values and unusable paths, quarantine of a corrupt settings file instead of silently overwriting it |
| `StatsBarStateTest` | Persisted status-bar size: starts from the setting, a fresh install starts at medium, cycling persists each step, a corrupt value degrades to medium, the mapping covers both enums both ways |
| `FontSizeManagerTest` | The font-size ladder, off-ladder sizes inserted without changing the setting, ladder stepping for Ctrl+±, clamping to the supported range and the persisted reset to the default size |
| `ToolbarOptionListsTest` | The six toolbar dropdowns: option order, no duplicates, and every dropdown able to display its own current value |
| `AtomicFilesTest` | Durable atomic writes: content integrity, no temp leftovers, cleanup after failures |
| `MessagesTest` | i18n bundle parity across all 6 languages, localised theme/colour names, locale-independent bundle resolution |
| `DialogServiceTest` | Save-as file naming rules (default extension handling) |
| `JTypeWriterAppTests` | Main method presence |
| `AppLoggingTest` | Log formatter builds its header without throwing (regression) and appends an indented stack trace with the cause chain for thrown exceptions; a log-file handler in a temp directory, an unusable log directory and a denied home falling back to console only, and repeated `init()` not stacking handlers |
| `InstanceLockTest` | Instance-lock acquisition, rejection of a second acquire in the same JVM, release and idempotent close, fail-open behaviour for an unusable lock file, and a read-only data directory reporting `UNAVAILABLE` instead of blocking startup |
| `LanguageTest` | Native language names are stable code constants under every system locale (no `getDisplayLanguage` trap) |
| `ToolbarStateTest` | The sound dropdown's single setter: persistence across a restart, the audio engine follows, repeats are no-ops |
| `ToolbarFitTest` | Toolbar overflow policy: how the captioned row is measured (buttons, dropdowns, spacer, chrome) with injected metrics, the FULL→COMPACT switch and its 40 px hysteresis, plus acceptance tests proving German and French are not clipped at 1280 px and that the collapsed row fits 900 px in all six languages |
| `PackagingNoticesTest` | The packaging contract: `META-INF/THIRD-PARTY-NOTICES` and both license texts are on the classpath, the notice names every bundled component with its real SPDX id, states that FFmpeg is not distributed, stays LF-only within 100 columns, and is byte-identical to the checked-in file so a stale build output cannot pass |
| `ContrastRatioTest` | WCAG contrast maths (black-on-white 21:1, identical colours 1:1, the 4.5 boundary) and CSS-parsing guards that fail the build if status text or the autosave warning drops below AA or 13 px, the focus ring drops below 3:1, highlighted popup-row text drops below AA, the popup cell font size drifts from the closed combo's, the focus colour becomes transparent again, or a non-existent JavaFX CSS property (`-fx-letter-spacing`, `-fx-transition`) returns |

---

## License

JTypeWriter is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for the full text and [NOTICE](NOTICE) for attribution notices. [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) lists every bundled component with its license id, copyright line and source, and the full texts sit in [legal/](legal); both ship inside the fat JAR as `META-INF/THIRD-PARTY-NOTICES` and `META-INF/licenses/`, and at the root of the native image. The bundled JavaFX libraries stay under GPLv2 with the Classpath Exception and GStreamer Lite under LGPL-2.1-or-later; the OpenJDK runtime carries its own notices under `lib/runtime/legal/`. The bundled keystroke sound (`src/main/resources/audio/keystroke.wav`) is the author's own work, contains no third-party sample, and is covered by the same Apache-2.0 licence; its provenance and SHA-256 are recorded in [src/main/resources/audio/PROVENANCE.md](src/main/resources/audio/PROVENANCE.md).

---

## Developer

**Mesut ORMANLI**

- Email: [mesutormanli@gmail.com](mailto:mesutormanli@gmail.com)
- GitHub: [@hyperpostulate](https://github.com/hyperpostulate)

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -m 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request
