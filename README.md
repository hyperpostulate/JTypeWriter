<p align="center">
  <img src="src/main/resources/icons/jtypewriter-512.png" alt="JTypeWriter" width="512" height="512">
</p>

<h1 align="center">JTypeWriter</h1>

<p align="center">
  <a href="https://github.com/hyperpostulate/JTypeWriter/blob/main/pom.xml"><img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java 25"></a>
  <img src="https://img.shields.io/badge/Platforms-Linux%20%7C%20Windows%20%7C%20macOS-lightgrey.svg" alt="Platforms">
  <img src="https://img.shields.io/badge/Languages-6-blue.svg" alt="6 languages">
  <a href="https://www.gnu.org/licenses/gpl-3.0"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPL v3"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml/badge.svg" alt="Java CI"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhyperpostulate%2FJTypeWriter%2Fgh-pages%2Fbadges%2Ftests.json" alt="Tests"></a>
</p>

A distraction-free text editor built with JavaFX. Uses plain-Java dependency injection (manual composition root, no DI framework) for focused writing with typewriter sound effects, real-time statistics, multiple themes, and multi-language support.

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Build & Run](#build--run)
- [CI/CD](#cicd)
- [Architecture](#architecture)
- [Features](#features)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Themes](#themes)
- [Supported Languages](#supported-languages)
- [Testing](#testing)
- [License](#license)
- [Developer](#developer)
- [Contributing](#contributing)

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java        | 25+     |
| Maven       | 3.8+    |

---

## Installation

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
distribution; use `scripts/package.sh` for that. `mvn javafx:run` is the development
entry point. `scripts/package.sh` wraps the fat JAR into a native application image
with a bundled runtime via `jpackage`, and `--installer` wraps that image into the
platform installer (`.deb`, `.msi`, `.dmg`) for Linux, Windows and macOS - the same
command the CI workflow runs. The application version has a single source of truth:
`pom.xml`. The script reads it back from the built artifact and validates it against
jpackage's rules (macOS, for one, rejects `0.x` bundle versions).

The launcher, window and taskbar icon is generated from the single source of the
artwork, `src/main/resources/icons/jtypewriter.svg`: `scripts/generate-icons.py`
regenerates the PNG set plus the Windows `.ico` and macOS `.icns` (`--check` verifies
they are current), and `scripts/package.sh` picks the right format per platform.

---

## CI/CD

GitHub Actions workflow (`.github/workflows/maven.yml`):

- **Trigger**: Runs on every push and pull request to `main`
- **Environment**: Ubuntu, Windows and macOS (build matrix)
- **JDK**: Temurin 25
- **Command**: `mvn -B package` (build + tests), then `scripts/package.sh --skip-build --installer`
- **Locales**: the test suite also runs under Turkish and German system locales, so i18n regressions (e.g. a language that silently resolves to the system language) cannot slip through
- **Artifacts**: native application image plus a platform installer (`.deb` / `.msi` / `.dmg`) per OS

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
│   ├── BackupKeeper.java                 # Rotating .bak backups before overwrites
│   ├── DialogService.java                # FileChooser & Alert dialog management
│   ├── FileService.java                  # Atomic file I/O (encoding-stable)
│   ├── RecoveryStore.java                # Crash-recovery draft storage
│   └── TextCodec.java                    # Encoding detection & lossless round trips
├── stats/
│   └── SessionStats.java                 # Writing session statistics (WPM, counts)
├── ui/
│   ├── MainController.java               # Root layout controller
│   ├── MainStage.java                    # Stage configuration (fullscreen, close)
│   ├── WelcomeDialog.java                # Welcome screen with shortcuts
│   ├── AboutDialog.java                  # About dialog with author info
│   ├── KeyboardShortcutHandler.java      # Centralized keyboard shortcut handling
│   ├── CrashHandler.java                 # Global crash net: log + error dialog
│   ├── SingleInstanceWarning.java        # "Already running" warning window
│   ├── component/
│   │   ├── EditorArea.java               # TextArea enforcing the YOLO policy
│   │   ├── YoloPolicy.java               # YOLO mode edit policy (pure logic)
│   │   ├── EditorKeyHandler.java         # Keystroke sound + stats tracking
│   │   ├── StatsBar.java                 # Bottom status bar (clock + stats)
│   │   ├── ToolbarView.java              # Top toolbar (buttons + labels)
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
| `StatsBar` | Renders the clock and live session statistics in the bottom bar |
| `KeyboardShortcutHandler` | Centralized keyboard shortcut routing |
| `ToolbarView` | Top toolbar with all editor controls |
| `ToolbarState` | Manages YOLO mode and sound on/off state |
| `FileService` | Atomic file I/O (temp write + fsync + move) with encoding stability |
| `DialogService` | UI dialog management (FileChooser, Alert) |
| `TextCodec` | Encoding detection (BOM/UTF-8) and lossless round trips |
| `BackupKeeper` | Rotating `.bak` backups taken before overwrites |
| `RecoveryStore` | Crash-recovery draft storage: one atomic `draft-<uuid>.jtw` file per session under `~/.jtypewriter/recovery/` |
| `AutoSaveService` | Periodic draft snapshots and startup recovery offer |
| `CrashHandler` | Global crash net: logging plus user-facing error dialog |
| `InstanceLock` | Advisory file lock that rejects a second instance per user |
| `SingleInstanceWarning` | Minimal window that shows the "already running" warning and exits |
| `AppSettings` | Persisted preferences under `~/.jtypewriter/settings.properties` |
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

## Features

### Typewriter Sound Effects

Authentic typewriter keystroke sound on every non-modifier key press. Can be toggled on/off via the toolbar.

### YOLO Mode

"YOLO Mode" (You Only Live Once) prevents accidental deletion. When enabled, Backspace, Delete, Cut and Undo/Redo are disabled (the right-click menu shows Cut greyed out, with only Copy/Paste/Select All usable), so no edit can remove characters that are already in the document. Perfect for first-draft writing where you push forward without looking back. The mode stays visible through the `[YOLO]` title badge and the toolbar status label; theme colours stay untouched in both states.

### Real-Time Statistics

Live stats bar showing:
- Word count, character count (with/without spaces), line count
- Total keystrokes
- Session duration (HH:MM:SS)
- Words per minute (WPM) - sustained speed based on active typing time, so breaks do not drag it down
- The status bar size cycles between small, medium and large when you double-click it

### Fullscreen & Toolbar

`F11` toggles fullscreen and `Escape` leaves it (the platform's default exit hint is
suppressed). The toolbar can be hidden with `Ctrl+T` when you want nothing but text
on screen.

### Data Safety & Crash Recovery

- **Atomic, durable saves with backups:** every save is written to a temp file, fsynced and then renamed into place, so a crash mid-save or a power cut cannot leave a truncated document; when the file system does not support an atomic move, the rename degrades to a plain move with the bytes already on disk. Overwrites keep rotating backups (`file.bak`, `.bak.1`, `.bak.2`) and preserve the document's existing file permissions
- **Document awareness:** the window title shows the open document name and a `*` while there are unsaved changes (alongside the `[YOLO]` badge). `Ctrl+N` starts a fresh untitled document (it also works while YOLO mode is on) after the same save/discard/cancel prompt as opening a file; loading or creating a document clears the undo history, so `Ctrl+Z` can never bring back the previous document's text
- **Autosave & recovery:** unsaved work is snapshotted every 30 seconds and on focus loss to `~/.jtypewriter/recovery/`; after a crash the draft is offered for restoration on the next startup. The prompt offers restore, discard and later - closing it never deletes the draft, and a postponed draft is offered again on the next start
- **Encoding safety:** a BOM-marked file is decoded with the encoding of its BOM; a file without a BOM must be valid UTF-8, otherwise you are asked whether it is Windows-1254 or UTF-8 instead of getting silent corruption. Documents are written back in the same encoding unless you accept the offered switch to UTF-8 when the text no longer fits (e.g. a pasted emoji in a Windows-1254 file) - unmappable characters are never silently replaced with `?`
- **Error visibility:** unexpected errors are logged to `~/.jtypewriter/logs/` and surfaced in an error dialog instead of failing silently. Thrown exceptions are written with their full stack trace (including the cause chain), and a corrupt settings file is moved aside as `settings.properties.corrupt` instead of being silently overwritten

### Settings & Session Restore

Language, theme, text colour, font size, sound, the last document and its folder,
and the window geometry are persisted to `~/.jtypewriter/settings.properties`. The
next session starts where the last one ended: same look, same document, same window.
A document that has disappeared in the meantime is skipped silently, and a broken
settings file falls back to the defaults instead of blocking startup. YOLO mode is
deliberately *not* persisted: a mode that blocks deletions should not still be armed
at the next start.

### Single Instance

Only one JTypeWriter instance runs per user account: a lock file in `~/.jtypewriter/`
stops a second start, which then shows a localized warning popup and exits instead of
opening another window. The operating system releases the lock automatically if the
running instance crashes, so a crash never locks you out.

### Multiple Themes

Three built-in themes:
- **Dark** - Default, easy on the eyes
- **Light** - Clean and bright
- **Sepia** - Warm, paper-like feel

### Text Colors

Six text colors (one default plus five accents): Default, Sage, Lavender, Sand, Steel, Rose.

### Multi-Language Support

6 languages: English, Turkish, German, French, Italian, Spanish. Toolbar, dialogs and the status bar update immediately when the language is switched.

---

## Keyboard Shortcuts

Shortcuts use `Ctrl` on Windows/Linux and `⌘` (Cmd) on macOS — plain `Ctrl` works on macOS too. Numpad `+`, `-` and `0` are accepted as equivalents of their main-keyboard neighbours.

| Shortcut | Action |
|----------|--------|
| `Ctrl+T` | Toggle toolbar |
| `Ctrl+Shift+T` | Cycle through the three themes |
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
| `BackupKeeperTest` | Pre-overwrite backup rotation and retention |
| `RecoveryStoreTest` | Crash-recovery draft lifecycle (save/restore/drop) in one atomic per-session `.jtw` file, newest-draft adoption, isolation between concurrent sessions, tolerance for corrupt headers, bad timestamps, unusable paths and invalid UTF-8 |
| `AutoSaveServiceTest` | Opening or creating a document drops the previous document's stale recovery draft; a postponed draft survives a clean exit (regression) |
| `YoloPolicyTest` | YOLO mode edit policy (deletions blocked, insertions allowed, safe replacement) |
| `KeyboardShortcutHandlerTest` | Shortcut decoding incl. shift-sensitive combos (Ctrl+Shift+Y / Z / C / T / S), Ctrl+N for a new document and numpad equivalents |
| `MainControllerTest` | Window title composition (application + document name, unsaved `*` marker, `[YOLO]` badge) |
| `MainStageTest` | Off-screen window position guard (`intersectsAny`: restored positions on detached monitors) |
| `AppSettingsTest` | Preference persistence across restarts, defaults, tolerance for unknown values and unusable paths, quarantine of a corrupt settings file instead of silently overwriting it |
| `FontSizeManagerTest` | Font size clamping to the supported range, persisted reset to the default size |
| `AtomicFilesTest` | Durable atomic writes: content integrity, no temp leftovers, cleanup after failures |
| `MessagesTest` | i18n bundle parity across all 6 languages, localised theme/colour names, locale-independent bundle resolution |
| `DialogServiceTest` | Save-as file naming rules (default extension handling) |
| `JTypeWriterAppTests` | Main method presence |
| `AppLoggingTest` | Log formatter builds its header without throwing (regression) and appends an indented stack trace with the cause chain for thrown exceptions |
| `InstanceLockTest` | Instance-lock acquisition, rejection of a second acquire in the same JVM, release and idempotent close, fail-open behaviour for an unusable lock file |

---

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.

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

---

<p align="center">
  <img src="src/main/resources/icons/jtypewriter-512.png" alt="JTypeWriter" width="512" height="512">
</p>

<h1 align="center">JTypeWriter</h1>

<p align="center">
  <a href="https://github.com/hyperpostulate/JTypeWriter/blob/main/pom.xml"><img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java 25"></a>
  <img src="https://img.shields.io/badge/Platforms-Linux%20%7C%20Windows%20%7C%20macOS-lightgrey.svg" alt="Platforms">
  <img src="https://img.shields.io/badge/Languages-6-blue.svg" alt="6 languages">
  <a href="https://www.gnu.org/licenses/gpl-3.0"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPL v3"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml/badge.svg" alt="Java CI"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhyperpostulate%2FJTypeWriter%2Fgh-pages%2Fbadges%2Ftests.json" alt="Tests"></a>
</p>

JavaFX ile oluşturulmuş, dikkat dağıtmayan bir metin editörü. Bağımlılık enjeksiyonu framework'süz, saf Java ile (manuel kompozisyon kökü) yapılır. Daktilo ses efektleri, gerçek zamanlı istatistikler, çoklu tema ve çoklu dil desteği ile odaklanarak yazma deneyimi sunar.

---

## İçindekiler

- [Gereksinimler](#gereksinimler)
- [Kurulum](#kurulum)
- [Derleme ve Çalıştırma](#derleme-ve-çalıştırma)
- [CI/CD](#cicd-1)
- [Mimari](#mimari)
- [Özellikler](#özellikler)
- [Klavye Kısayolları](#klavye-kısayolları)
- [Temalar](#temalar)
- [Desteklenen Diller](#desteklenen-diller)
- [Testler](#testler)
- [Lisans](#lisans-1)
- [Geliştirici](#geliştirici)
- [Katkıda Bulunma](#katkıda-bulunma)

---

## Gereksinimler

| Gereksinim | Sürüm |
|------------|-------|
| Java       | 25+   |
| Maven      | 3.8+  |

---

## Kurulum

### Manuel Derleme

```bash
git clone https://github.com/hyperpostulate/JTypeWriter.git
cd JTypeWriter
mvn clean install
```

---

## Derleme ve Çalıştırma

```bash
mvn clean javafx:run                 # Uygulamayı derle ve başlat
mvn clean test                       # Tüm testleri çalıştır
mvn clean package                    # Derleme + test + ince JAR + fat JAR
./scripts/package.sh                 # Derleme + yerel uygulama görüntüsü (releases/<os>/)
./scripts/package.sh --skip-build    # Son derlemeyi kullan, yalnızca yeniden sar
./scripts/package.sh --installer     # Aynı + platform kurulum paketi (.deb / .msi / .dmg)
mvn test -Dtest=SessionStatsTest     # Tek test sınıfı
mvn test -Dtest=FileServiceTest      # Tek test sınıfı
```

Paketleme iki JAR üretir: ince olanı (yalnızca uygulama sınıfları) ve JavaFX'i kendi native
kütüphaneleriyle birlikte taşıyan `jar-with-dependencies`. Bu native kütüphaneler derlemenin
yapıldığı makineden geldiği için fat JAR yalnızca derlendiği platformda çalışır; kolaylık
artefaktıdır, platformlar arası dağıtım için `scripts/package.sh` kullanılır. Geliştirme
girişi `mvn javafx:run` komutudur. `scripts/package.sh`, fat JAR'ı `jpackage` ile gömülü
çalışma zamanlı yerel bir uygulama görüntüsüne sarar; `--installer` seçeneği bu görüntüyü
Linux, Windows ve macOS için platform kurulum paketine (`.deb`, `.msi`, `.dmg`) dönüştürür —
CI iş akışının çalıştırdığı komut da budur. Uygulama sürümünün tek kaynağı `pom.xml`'dir:
script sürümü üretilen artefaktten geri okur ve jpackage kurallarına karşı doğrular
(örneğin macOS `0.x` paket sürümlerini reddeder).

Başlatıcı, pencere ve görev çubuğu simgesi, görselin tek kaynağı olan
`src/main/resources/icons/jtypewriter.svg` dosyasından üretilir: `scripts/generate-icons.py`
PNG setini, Windows `.ico` ve macOS `.icns` dosyalarını yeniden oluşturur (`--check`
güncelliklerini doğrular); `scripts/package.sh` platforma göre doğru biçimi seçip
`jpackage`'a aktarır.

---

## CI/CD

GitHub Actions iş akışı (`.github/workflows/maven.yml`):

- **Tetikleme**: `main` dalına her push ve pull request'te çalışır
- **Ortam**: Ubuntu, Windows ve macOS (build matrisi)
- **JDK**: Temurin 25
- **Komut**: `mvn -B package` (derleme + testler), ardından `scripts/package.sh --skip-build --installer`
- **Diller**: test paketi Türkçe ve Almanca sistem dillerinde de çalışır; böylece yerelleştirme regresyonları (ör. bir dilin sessizce sistem diline çözülmesi) gözden kaçamaz
- **Artefaktlar**: her işletim sistemi için yerel uygulama görüntüsü ve platform kurulum paketi (`.deb` / `.msi` / `.dmg`)

---

## Mimari

```
org.mesutormanli.jtypewriter
├── audio/
│   └── TypewriterSound.java              # Daktilo tuş sesi efekti
├── locale/
│   ├── Language.java                     # Desteklenen diller enum'u
│   ├── LocaleManager.java                # Dil değiştirme + dinleyici desteği
│   └── Messages.java                     # ResourceBundle tabanlı i18n erişici
├── service/
│   ├── AutoSaveService.java              # Dönemsel taslak kaydı + kurtarma teklifi
│   ├── BackupKeeper.java                 # Yazımdan önce dönen .bak yedekleri
│   ├── DialogService.java                # FileChooser ve Alert dialog yönetimi
│   ├── FileService.java                  # Atomik dosya G/Ç (kodlama korumalı)
│   ├── RecoveryStore.java                # Çökme kurtarma taslak deposu
│   └── TextCodec.java                    # Kodlama tespiti + kayıpsız gidiş-dönüş
├── stats/
│   └── SessionStats.java                 # Yazma oturumu istatistikleri (WPM, sayımlar)
├── ui/
│   ├── MainController.java               # Kök yerleşim denetleyicisi
│   ├── MainStage.java                    # Sahne yapılandırması (tam ekran, kapatma)
│   ├── WelcomeDialog.java                # Kısayollarla hoş geldin ekranı
│   ├── AboutDialog.java                  # Yazar bilgisiyle hakkında dialogu
│   ├── KeyboardShortcutHandler.java      # Merkezi klavye kısayolu yönetimi
│   ├── CrashHandler.java                 # Global çökme ağı: log + hata diyaloğu
│   ├── SingleInstanceWarning.java        # "Zaten çalışıyor" uyarı penceresi
│   ├── component/
│   │   ├── EditorArea.java               # YOLO politikasını uygulayan TextArea
│   │   ├── YoloPolicy.java               # YOLO modu düzenleme politikası (saf mantık)
│   │   ├── EditorKeyHandler.java         # Tuş vuruşu sesi + istatistik takibi
│   │   ├── StatsBar.java                 # Alt durum çubuğu (saat + istatistikler)
│   │   ├── ToolbarView.java              # Üst araç çubuğu (düğmeler + etiketler)
│   │   └── ToolbarState.java             # YOLO modu + ses aç/kapa durumu
│   └── theme/
│       ├── Theme.java                    # Tema enum'u (Açık/Koyu/Sepya)
│       ├── ThemeManager.java             # CSS stil sayfası yükleme ve değiştirme
│       ├── FontSizeManager.java          # Editör font boyutu durumu
│       ├── TextColorManager.java         # Metin rengi döngüsü durumu
│       └── TextColor.java                # CSS değerleriyle metin rengi enum'u
├── JTypeWriterApp.java                   # Ana giriş noktası
├── AppDirs.java                          # Kullanıcı veri/log/kurtarma/ayar dizinleri
├── AppGraph.java                         # Kompozisyon kökü (manuel bağımlılık enjeksiyonu)
├── AppLogging.java                       # Dönen log kurulumu (java.util.logging)
├── AppSettings.java                      # Kalıcı tercihler (settings.properties)
├── AtomicFiles.java                      # Dayanıklı atomik dosya yazımı (fsync + rename)
├── InstanceLock.java                     # Kullanıcı başına örnek kilidi (tek örnek)
└── FxApplication.java                    # JavaFX yaşam döngüsü köprüsü
```

### Temel Bileşenler

| Bileşen | Açıklama |
|---------|----------|
| `EditorArea` | Her düzenlemeyi YOLO politikasından geçiren TextArea alt sınıfı; belgeleri `setDocument` ile yükler |
| `UndoHistoryReset` | Her belge yüklemesinde TextArea'nın geri al/ileri al geçmişini temizler (yansıma ile; JavaFX'te API yok) |
| `YoloPolicy` | YOLO modu düzenleme politikası (silmeyi engeller, eklemeye izin verir) |
| `EditorKeyHandler` | Tuş vuruşu ses efektleri ve oturum istatistiklerini yönetir |
| `StatsBar` | Saati ve canlı oturum istatistiklerini alt çubukta gösterir |
| `KeyboardShortcutHandler` | Merkezi klavye kısayolu yönlendirmesi |
| `ToolbarView` | Tüm editör kontrollerini içeren üst araç çubuğu |
| `ToolbarState` | YOLO modu ve ses aç/kapa durumunu yönetir |
| `FileService` | Atomik dosya G/Ç (geçici dosya + fsync + taşıma), kodlama korumalı |
| `DialogService` | UI dialog yönetimi (FileChooser, Alert) |
| `TextCodec` | Kodlama tespiti (BOM/UTF-8) ve kayıpsız gidiş-dönüşler |
| `BackupKeeper` | Yazımdan önce alınan dönen `.bak` yedekleri |
| `RecoveryStore` | Çökme kurtarma taslakları: `~/.jtypewriter/recovery/` altında oturum başına tek atomik `draft-<uuid>.jtw` dosyası |
| `AutoSaveService` | Dönemsel taslak anlık görüntüleri ve açılışta kurtarma teklifi |
| `CrashHandler` | Global çökme ağı: log + kullanıcıya hata diyaloğu |
| `InstanceLock` | İkinci örneği kullanıcı başına reddeden danışma amaçlı dosya kilidi |
| `SingleInstanceWarning` | "Zaten çalışıyor" uyarısını gösterip kapanan minimal pencere |
| `AppSettings` | `~/.jtypewriter/settings.properties` altında kalıcı tercihler |
| `AtomicFiles` | Dayanıklı atomik dosya yazımı (fsync, rename, dizin flush) |
| `ThemeManager` | CSS tema yükleme ve değiştirme |
| `FontSizeManager` | Editör font boyutu durumu (10-48px) |
| `TextColorManager` | Metin rengi döngüsü durumu |
| `SessionStats` | Gerçek zamanlı yazma istatistikleri (WPM, kelime/karakter/satır sayısı) |
| `TypewriterSound` | Daktilo tuş sesi oynatma |
| `LocaleManager` | Değişiklik dinleyicileriyle çoklu dil desteği |
| `Messages` | Resource bundle tabanlı mesaj erişici |

### Bağımlılıklar

| Bağımlılık | Sürüm | Amaç |
|------------|-------|------|
| JavaFX Controls | 26.0.1 | UI bileşenleri |
| JavaFX FXML | 26.0.1 | FXML yükleme |
| JavaFX Media | 26.0.1 | Ses oynatma |
| JUnit Jupiter | 6.0.3 | Test çerçevesi |
| Mockito | 5.23.0 | Test mock'lama |

JavaFX Graphics ve Base, Controls ile dolaylı olarak gelir. Bağımlılık enjeksiyonu framework'süzdür: nesne grafiğini `AppGraph` elle kurar.

---

## Özellikler

### Daktilo Ses Efektleri

Değiştirici olmayan her tuş basımında otantik daktilo tuş sesi. Araç çubuğundaki düğmeyle açılıp kapatılabilir.

### YOLO Modu

"YOLO Modu" (You Only Live Once) kazara silmeyi önler. Etkinleştirildiğinde Backspace, Delete, Cut ve Geri Al/İleri Al devre dışı kalır (sağ-tık menüsünde Kes soluk/devre dışı gösterilir; yalnızca Kopyala/Yapıştır/Tümünü Seç kullanılabilir); böylece hiçbir düzenleme belgedeki mevcut karakterleri silemez. İlk taslak yazımında geriye bakmadan ileriye giden yazım için mükemmeldir. Mod, `[YOLO]` başlık rozeti ve araç çubuğu durum etiketi ile görünür tutulur; tema renkleri her iki durumda da değişmez.

### Gerçek Zamanlı İstatistikler

Canlı istatistik çubuğu:
- Kelime sayısı, karakter sayısı (boşluklu/boşluksuz), satır sayısı
- Toplam tuş vuruşu
- Oturum süresi (SS:DD:SS)
- Dakikada kelime (WPM) - aktif yazım süresine dayalı kalıcı hız; molalar değeri düşürmez
- Durum çubuğuna çift tıklandığında boyutu küçük, orta ve büyük arasında döner

### Tam Ekran ve Araç Çubuğu

`F11` tam ekranı açar/kapatır, `Escape` tam ekrandan çıkar (platformun varsayılan çıkış
ipucu gizlenir). Ekranda yalnızca metin kalsın istiyorsanız araç çubuğunu `Ctrl+T` ile
gizleyebilirsiniz.

### Veri Güvenliği ve Kurtarma

- **Atomik ve dayanıklı kayıt + yedek:** her kayıt geçici dosyaya yazılır, fsync edilir ve yeniden adlandırılarak yerine konur; kayıt ortasında çökme ya da elektrik kesintisi belgeyi yarım bırakamaz. Dosya sistemi atomik taşımayı desteklemiyorsa yeniden adlandırma düz taşımaya düşer; baytlar o anda zaten diskte olur. Üzerine yazmalarda dönen yedekler tutulur (`dosya.bak`, `.bak.1`, `.bak.2`) ve belgenin mevcut dosya izinleri korunur
- **Belge farkındalığı:** pencere başlığı açık belgenin adını ve kaydedilmemiş değişiklik varken bir `*` işaretini gösterir (`[YOLO]` rozetinin yanında). `Ctrl+N` boş, adlandırılmamış bir belge açar (YOLO modu açıkken de çalışır); dosya açarken sorulan kaydet / kaydetme / iptal sorusu burada da sorulur. Belge açmak veya yeni belge oluşturmak geri alma geçmişini sıfırlar, böylece `Ctrl+Z` önceki belgenin metnini asla geri getiremez
- **Otomatik kayıt ve kurtarma:** kaydedilmemiş çalışma 30 saniyede bir ve odak kaybında `~/.jtypewriter/recovery/` altına alınır; çökme sonrası taslak bir sonraki açılışta geri yükleme için teklif edilir. İstem geri yükle / sil / sonra sor seçeneklerini sunar — kapatmak taslağı asla silmez ve ertelenen taslak sonraki açılışta yeniden sorulur
- **Kodlama güvenliği:** BOM'lu bir dosya, BOM'unun belirttiği kodlamayla çözülür; BOM'suz bir dosya geçerli UTF-8 olmalıdır, aksi halde sessiz bozulma yerine Windows-1254 mü UTF-8 mi olduğu sorulur. Belgeler aynı kodlamayla geri yazılır; yalnızca metin o kodlamaya sığmadığında (örn. Windows-1254 dosyaya yapıştırılan emoji) sunulan UTF-8'e geçişi kabul ederseniz kodlama değişir — temsil edilemeyen karakterler asla sessizce `?` yapılmaz
- **Hata görünürlüğü:** beklenmeyen hatalar `~/.jtypewriter/logs/` altına yazılır ve sessizce kaybolmak yerine hata diyaloğuyla gösterilir. Fırlatılan istisnalar tam yığın iziyle (neden zinciri dahil) yazılır. Bozuk ayar dosyası sessizce üzerine yazılmaz: `settings.properties.corrupt` olarak kenara alınır ve içeriği tanılama için korunur

### Ayarlar ve Oturum Geri Yükleme

Dil, tema, metin rengi, font boyutu, ses, son belge ve klasörü ile pencere geometrisi
`~/.jtypewriter/settings.properties` dosyasına kaydedilir. Bir sonraki oturum,
öncekinin bittiği yerden başlar: aynı görünüm, aynı belge, aynı pencere. Aradan
geçen sürede silinmiş bir belge sessizce atlanır; bozuk bir ayar dosyası ise
başlangıcı engellemek yerine varsayılanlara döner. YOLO modu bilinçli olarak
*kaydedilmez*: silmeyi engelleyen bir modun bir sonraki açılışta hâlâ etkin olması
istenmez.

### Tek Örnek

Kullanıcı hesabı başına yalnızca bir JTypeWriter örneği çalışır: `~/.jtypewriter/`
altındaki bir kilit dosyası ikinci başlatmayı durdurur; ikinci bir pencere açılmaz,
yerelleştirilmiş bir uyarı penceresi gösterilir ve uygulama kapanır. Çalışan örnek
çökerse kilit işletim sistemi tarafından otomatik bırakılır; çökme sizi dışarıda
bırakmaz.

### Çoklu Tema

Üç yerleşik tema:
- **Koyu** - Varsayılan, göz yormaz
- **Açık** - Temiz ve parlak
- **Sepya** - Sıcak, kağıt hissi

### Metin Renkleri

Altı metin rengi (bir varsayılan + beş vurgu rengi): Varsayılan, Adaçayı, Lavanta, Kum, Çelik, Gül

### Çoklu Dil Desteği

6 dil: İngilizce, Türkçe, Almanca, Fransızca, İtalyanca, İspanyolca. Dil değiştirildiğinde araç çubuğu, diyaloglar ve durum çubuğu anında güncellenir.

---

## Klavye Kısayolları

Kısayollar Windows/Linux'ta `Ctrl`, macOS'ta `⌘` (Cmd) ile çalışır — macOS'ta düz `Ctrl` de kabul edilir. Numpad `+`, `-` ve `0` tuşları ana klavye karşılıklarının eşdeğeri olarak çalışır.

| Kısayol | İşlem |
|---------|-------|
| `Ctrl+T` | Araç çubuğunu aç/kapat |
| `Ctrl+Shift+T` | Üç tema arasında geçiş yap |
| `Ctrl+Shift+Y` | YOLO modunu aç/kapat |
| `Ctrl+Shift+C` | Metin rengini değiştir |
| `Ctrl++` | Font boyutunu artır |
| `Ctrl+-` | Font boyutunu azalt |
| `Ctrl+0` | Font boyutunu 16px'e sıfırla |
| `Ctrl+Z` | Geri al |
| `Ctrl+Shift+Z` | İleri al |
| `Ctrl+Y` | İleri al |
| `Ctrl+N` | Yeni belge |
| `Ctrl+O` | Dosya aç |
| `Ctrl+S` | Dosyayı kaydet |
| `Ctrl+Shift+S` | Farklı kaydet |
| `F11` | Tam ekranı aç/kapat |
| `Escape` | Tam ekrandan çık |

---

## Temalar

CSS dosyaları `src/main/resources/css/` konumundadır:

| Tema | Dosya | Açıklama |
|------|-------|----------|
| Temel | `base.css` | Tüm temalarda paylaşılan stiller |
| Koyu | `dark.css` | Koyu arka plan, açık metin |
| Açık | `light.css` | Açık arka plan, koyu metin |
| Sepya | `sepia.css` | Sıcak sepya tonlu arka plan |

---

## Desteklenen Diller

| Dil | Dosya | Kod |
|-----|-------|-----|
| İngilizce | `messages.properties` | `EN` |
| Türkçe | `messages_tr.properties` | `TR` |
| Almanca | `messages_de.properties` | `DE` |
| Fransızca | `messages_fr.properties` | `FR` |
| İtalyanca | `messages_it.properties` | `IT` |
| İspanyolca | `messages_es.properties` | `ES` |

---

## Testler

### Test Çalıştırma

```bash
# Tüm testler
mvn test

# Tek test sınıfı
mvn test -Dtest=SessionStatsTest

# Tek test metodu
mvn test -Dtest=SessionStatsTest#wordCountIsCorrect
```

### Test Sınıfları

| Test Sınıfı | Kapsam |
|-------------|--------|
| `SessionStatsTest` | Kelime/karakter/satır sayısı, tuş vuruşları, sıfırlama, aktif yazım süreli WPM (durağanlık bozulması regresyonu) |
| `FileServiceTest` | Açık sırasında ve yeni belgede kaydedilmemiş değişiklik akışı (iptal/atla/önce kaydet), atomik yazım, dönen yedekler, kodlama gidiş-dönüşü (temsil edilemeyen karakterler için UTF-8'e geçiş), yeni belgede kodlama sıfırlama, yaşam döngüsü dinleyicileri, kurtarma bağlama |
| `TextCodecTest` | Kodlama tespiti (BOM, sıkı UTF-8), eski kodlama sorusu, kayıpsız kodlama gidiş-dönüşü, sıkı kodlama (temsil edilemeyen karakterler `?` olmak yerine hata verir) |
| `BackupKeeperTest` | Yazımdan önce yedek rotasyonu ve saklama |
| `RecoveryStoreTest` | Çökme kurtarma taslağı yaşam döngüsü (kaydet/geri yükle/sil); oturum başına tek atomik `.jtw` dosyası, en yeni taslağın devralınması, eşzamanlı oturumların izolasyonu, bozuk başlık, hatalı zaman damgası, kullanılamayan yol ve geçersiz UTF-8'e tolerans |
| `AutoSaveServiceTest` | Belge açıldığında veya yeni belge oluşturulduğunda önceki belgenin bayat kurtarma taslağının silinmesi; ertelenen taslağın temiz çıkışta korunması (regresyon) |
| `YoloPolicyTest` | YOLO modu düzenleme politikası (silme engelli, ekleme serbest, güvenli değiştirme) |
| `KeyboardShortcutHandlerTest` | Kısayol çözümleme; shift hassas birleşimler (Ctrl+Shift+Y / Z / C / T / S), yeni belge için Ctrl+N ve numpad eşdeğerleri |
| `MainControllerTest` | Pencere başlığı oluşturma (uygulama + belge adı, kaydedilmemiş `*` işareti, `[YOLO]` rozeti) |
| `MainStageTest` | Ekran dışı pencere konumu koruması (`intersectsAny`: artık bağlı olmayan monitörlerdeki konumlar) |
| `AppSettingsTest` | Tercihlerin yeniden başlatmada korunması, varsayılanlar, bilinmeyen değerlere ve kullanılamayan yollara tolerans, bozuk ayar dosyasının sessizce ezilmek yerine karantinaya alınması |
| `FontSizeManagerTest` | Yazı boyutunun desteklenen aralığa kısıtlanması, varsayılan boyuta kalıcı sıfırlama |
| `AtomicFilesTest` | Dayanıklı atomik yazım: içerik bütünlüğü, artık temp dosyası yok, hata sonrası temizlik |
| `MessagesTest` | 6 dilin tamamında i18n anahtar eşitliği, yerelleştirilmiş tema/renk adları, sistem dilinden bağımsız bundle çözümlemesi |
| `DialogServiceTest` | "Farklı Kaydet" dosya adlandırma kuralları (uzantı işleme) |
| `JTypeWriterAppTests` | Ana metod varlığı |
| `AppLoggingTest` | Log biçimlendiricisinin başlık satırını hata vermeden üretmesi (regresyon) ve fırlatılan istisnalar için neden zinciriyle birlikte girintili yığın izi eklemesi |
| `InstanceLockTest` | Örnek kilidinin alınması, aynı JVM'de ikinci alımın reddedilmesi, bırakma ve yinelenebilir kapatma, kullanılamayan kilit dosyasında fail-open davranışı |

---

## Lisans

Bu proje GNU General Public License v3.0 altında lisanslanmıştır. Detaylı bilgi için [LICENSE](LICENSE) dosyasına bakın.

---

## Geliştirici

**Mesut ORMANLI**

- E-posta: [mesutormanli@gmail.com](mailto:mesutormanli@gmail.com)
- GitHub: [@hyperpostulate](https://github.com/hyperpostulate)

---

## Katkıda Bulunma

1. Depoyu fork edin
2. Bir özellik dalı oluşturun (`git checkout -b feature/yeni-ozellik`)
3. Değişikliklerinizi Commit edin (`git commit -m 'Yeni özellik ekle'`)
4. Dalı push edin (`git push origin feature/yeni-ozellik`)
5. Bir Pull Request oluşturun
