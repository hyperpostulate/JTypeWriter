<p align="center">
  <img src="src/main/resources/icons/jtypewriter-512.png" alt="JTypeWriter" width="512" height="512">
</p>

<h1 align="center">JTypeWriter</h1>

<p align="center">
  <a href="https://github.com/hyperpostulate/JTypeWriter/blob/main/pom.xml"><img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java 25"></a>
  <img src="https://img.shields.io/badge/Platforms-Linux%20%7C%20Windows-lightgrey.svg" alt="Platforms">
  <img src="https://img.shields.io/badge/Languages-6-blue.svg" alt="6 languages">
  <a href="https://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License: Apache 2.0"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml/badge.svg" alt="Java CI"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhyperpostulate%2FJTypeWriter%2Fgh-pages%2Fbadges%2Ftests.json" alt="Tests"></a>
</p>

<p align="center">A quiet, offline desktop text editor for creative writers.</p>
<p align="center">It sounds like a typewriter, counts what you write, and makes losing that writing very hard.</p>

<p align="center">Türkçe sürüm: <a href="README-TR.md">README-TR.md</a></p>

JTypeWriter opens a single plain-text document and then stays out of the way: no accounts, no cloud, no project format. Your text lives in the file you picked, in the encoding that file already had. The packaged download carries its own Java runtime, so nothing has to be installed first. It runs on Linux and Windows, and the interface speaks English, Turkish, German, French, Spanish and Italian.

## What you get

- **A draft you cannot wreck by accident.** YOLO mode turns off Backspace, Delete, Cut and Undo/Redo so you can only move forward. Every save is atomic, keeps rotating backups, and unsaved work is backed up every 30 seconds.
- **A safety net you can see.** The status bar confirms each automatic backup with its time, and after a crash the draft is offered back. Closing that prompt never deletes it.
- **The feel of typing.** A typewriter keystroke when you type, backspace or delete - at a volume you set - plus three themes and six text colours, all switchable while you write.
- **Numbers as you go.** Words, characters with and without spaces, lines, keystrokes, session time and a sustained words-per-minute figure that ignores your breaks.
- **Only text on screen.** `F11` goes fullscreen, `Ctrl+T` hides the toolbar along with the command menu it hosts, and the window title tells you which document is open and whether it is saved.
- **A session that comes back.** Language, theme, text colour, font size, last document with its caret and scroll position, and window geometry are restored on the next start.
- **Nothing leaves your machine.** No account, no telemetry, no update check, no network request.

---

## Table of Contents

- [What you get](#what-you-get)
- [Features](#features)
- [Your Data](#your-data)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Themes](#themes)
- [Supported Languages](#supported-languages)
- [Installation](#installation)
- [Technical Reference](#technical-reference)
- [License](#license)
- [Developer](#developer)
- [Contributing](#contributing)

---

## Features

### Typewriter Sound Effects

- Typing a character clicks; moving the caret, function keys and shortcuts stay silent.
- Volume from the toolbar's Sound dropdown or `View → Sound`: Off, 25%, 50%, 75%, 100%. The choice is remembered across restarts.
- Every click starts its own voice instead of cutting off the previous one, so fast typing sounds like fast typing.

### YOLO Mode

- `Ctrl+Shift+Y` disables Backspace, Delete, Cut and Undo/Redo, so no edit can remove characters that are already in the document.
- Copy, Paste and Select All keep working; Cut is greyed out in the right-click menu.
- The `[YOLO]` title badge and the toolbar's YOLO dropdown show that the mode is on.
- Deliberately not remembered: a mode that blocks deletions should not still be armed at the next start.

### Real-Time Statistics

The bottom bar shows, live:

- Word count, character count with and without spaces, and line count.
- Total keystrokes and session duration (`HH:MM:SS`).
- Words per minute based on active typing time only, so breaks do not drag it down.
- A save confirmation after `Ctrl+S` (`Saved 12:04`), and a separate one after every automatic backup (`Backup saved: 12:04`), so a real save is never confused with a recovery copy.
- Text size of 13 px by default; double-click the bar to cycle small, medium and large, and the choice is remembered.

The save confirmation disappears with the next edit; the backup confirmation stays until a real save replaces it or a new document is opened.

### Printing and PDF Export

- `Ctrl+P` prints the document and `Ctrl+Shift+E` exports a PDF; both are also in the File menu.
- Both use the **saved `.txt` file**, never the editor buffer: unsaved changes - or a document that was never saved - trigger a Save / Cancel prompt first, and Cancel means nothing is printed or exported.
- The page is fixed: bundled DejaVu Sans Mono at 12 pt, black on white, A4 portrait with 20 mm margins. Theme, text colour and editor font size do not apply.
- The PDF is written by the application itself and embeds the same font, so its text stays selectable and copyable.
- Print and PDF share one layout, so the sheet and the file break lines at the same place.
- Failures open an alert; a completed print or a written file says nothing.

### Fullscreen & Toolbar

- `F11` toggles fullscreen and `Escape` leaves it.
- `Ctrl+T` hides the toolbar - and with it the command menu that lives in it - so nothing but text is on screen.
- The toolbar stays one row at every width: dropdown captions collapse to their bare values on narrow windows and no control is clipped or wrapped.
- Every control is reachable with `Tab` in a fixed order and carries a tooltip in the UI language, including its shortcut where one exists ("Save (Ctrl+S)").
- Shown or hidden state, fullscreen and the status bar size are remembered, and a change is saved the moment you make it.
- A window left in fullscreen comes back in fullscreen; leaving it later lands on the remembered position.

### Command Menu

- The `☰` button on the left of the toolbar holds File, Edit, View and Help.
- It is a menu button, not a permanent menu strip, so it hides with the toolbar instead of following you into fullscreen.
- Every command the platform does not handle itself is listed, including `New` (`Ctrl+N`) and `Save As` (`Ctrl+Shift+S`).
- `View → Theme`, `YOLO`, `Text Color` and `Font Size` open value submenus with the current value checked; `Sound` opens the volume ladder; `Help → About` opens the About card.
- Menu entries, shortcuts and toolbar buttons all come from one command list, so they cannot drift apart.

### Find & Replace

- `Ctrl+F` opens a find/replace card in the window's top-right corner over the text; `Ctrl+H` opens the same card with the replace field focused.
- One "Find what" field, one "Replace with" field, a "Match case" checkbox, Find Next / Find Previous / Replace / Replace All, and a live status line ("3 of 12", "No matches").
- The card is a panel, not a modal window: the document stays visible and editable while you search.
- `F3` and `Shift+F3` walk the matches in both directions and wrap; they keep working after the card is closed, and with no query yet `F3` opens the card.
- Closing the card with `Escape` or its Close button keeps the query for the next `F3`.
- Search is literal text, never a regex.
- `Replace All` is a single undo step, and replacing does not search its own output.
- With YOLO on, a replacement asks once per operation and cancelling writes nothing.
- Large documents do not freeze the window: the scan runs in the background.

### Welcome, Recent Files and About Cards

- All three open as cards in the same top-right corner as the find panel, over the text; opening one closes whatever card was there.
- They are not modal: the document stays usable, and `Escape`, `Enter` or the `Close` button dismisses a card and puts the caret back in the text.
- A card that is taller than a short window shrinks to it and scrolls instead of running past the edge.
- Content is rebuilt on every opening and on a language switch, so an open card cannot stay in the previous language.
- The welcome card's "don't show this again" checkbox is remembered; `File → Show Welcome Card` is the way back.
- `File → Recent Files` lists the last ten documents, newest first. Rows show the file name, with the full path as their tooltip, and a document deleted outside the app drops off silently.
- A recent row opens through the same unsaved-changes gate as Open - it is not a second way to discard the editor.

### Data Safety & Crash Recovery

- **Atomic saves.** Every save is written to a temp file, flushed and renamed into place, so a crash mid-save or a power cut cannot leave a truncated document.
- **Rotating backups.** An overwrite keeps `<document>.bak`, `.bak.1` and `.bak.2` and preserves the document's file permissions.
- **Automatic backups.** Unsaved work is snapshotted every 30 seconds and whenever the window loses focus; the status bar confirms each one with its time.
- **Recovery after a crash.** A surviving draft is offered at the next start: restore, discard, or later. Closing that prompt never deletes anything.
- **Deleting is the exception.** A draft is deleted only when the close prompt was answered - the document was saved, or you explicitly chose to discard it. Any other exit keeps it for the next start.
- **A backup cannot roll a document back.** If the file on disk is newer than the draft, the text is still restored, but without its original path, so saving forces Save As.
- **Failures are visible.** A backup that cannot be written shows a status bar warning until a write succeeds again and explains itself in one dialog. A failed draft delete gets its own brief warning and no dialog.
- **Document awareness.** The title shows the document name, a `*` while there are unsaved changes and the `[YOLO]` badge while the mode is on.
- **A new document is a real decision.** `Ctrl+N` asks the same save / discard / cancel question, and undo history stops at the document boundary.

### Encoding Safety

- A document is saved back in the encoding it was opened with.
- A BOM-marked file is decoded with the encoding of its BOM.
- A BOM-less file must be valid UTF-8 or follow the UTF-16 byte pattern; otherwise you are asked whether it is Windows-1254 or UTF-8 instead of getting silent corruption.
- A BOM that contradicts the bytes is a question, not a guess: the app names the declared encoding and the file and asks before opening.
- Characters the encoding cannot represent are never silently replaced with `?`; you are offered a switch to UTF-8.

### Settings & Session Restore

- Remembered: language, theme, text colour, font size, sound level, last document, its caret and scroll position, its folder, window geometry, toolbar visibility, status bar size and fullscreen.
- Nothing blocks startup: a document that has disappeared is skipped, and a broken settings file falls back to the defaults.
- A settings file that cannot be written warns once, names the file and says what will not be remembered.
- A file written by an older build opens without losing values; a file from a newer build is read as far as this build understands it and never downgraded.
- YOLO mode is deliberately **not** restored.

### Opening Documents from Outside the App

- `JTypeWriter notes.txt` opens that document, and a file association or double-click hands the same path through the same code.
- Only one file can be named: a second argument is refused with a dialog that names it.
- A missing path, a folder or an unparsable argument is reported in the UI language instead of falling back to an empty editor.
- With no argument the welcome card appears, unless you turned it off with its own checkbox.
- A recovered draft wins over the command-line document: it is the only copy of that text, while the named file is still on disk.
- **Drag and drop works the same way.** Drop a `.txt` or `.md` anywhere on the window and it opens through the same gate as Open, including the save prompt.
- Zero files, several files, a folder, a PDF or a file that vanished before the drop are each refused with a dialog naming what was dropped.

### Single Instance

- One instance per user account, guarded by a lock file in `~/.jtypewriter/`.
- A second start opens no window of its own: it shows a localised warning, waits to be acknowledged and exits.
- The operating system releases the lock if the running instance crashes, so a crash never locks you out.

### Themes

- **Dark** - the default; low-glare, easy on the eyes.
- **Light** - clean and bright.
- **Sepia** - warm, paper-like.

`Ctrl+Shift+T` cycles Dark → Sepia → Light; the toolbar's Theme dropdown and `View → Theme` pick any of them directly. Themes can be changed while you write, and the status bar text meets WCAG AA contrast (at least 4.5:1) in all three.

### Text Colors

Six colours - Default, Sage, Lavender, Sand, Steel, Rose. `Ctrl+Shift+C` cycles; the toolbar and `View → Text Color` pick directly.

### Supported Languages

Six languages: English, Turkish, German, French, Italian and Spanish. Toolbar, menus, dialogs, cards, the window title and the status bar update immediately when the language is switched, and every date and time is rendered in that language's own format.

---

## Your Data

Everything JTypeWriter keeps stays on your machine. There is no account, no telemetry, no update check and no network request: the application itself sends nothing, and the only outbound action is the e-mail link in the About card, which hands a `mailto:` address to your own mail client.

- **Where it lives:** `~/.jtypewriter/` - preferences, recovery drafts, logs and the single-instance lock.
- **Drafts contain your text.** Each recovery draft holds the unsaved document verbatim; a draft you postponed survives a clean exit and is offered again on the next start.
- **Backups sit next to your documents.** An overwrite keeps `<document>.bak`, `.bak.1` and `.bak.2`, and a save stages a temporary `jtw-*.tmp` file beside the document before renaming it into place. Both contain document text, which is why a document can take four times its size on disk - and briefly up to six times while it is being saved. The first save says so once.
- **Logs** rotate through three files of up to one megabyte each and hold file paths, error messages and stack traces.
- **Recovery drafts are cleaned up.** Postponed drafts expire after 30 days and the parked set is capped at 10 files / 64 MiB, while an active draft is never expired - it may belong to another running instance.
- **Deleting all of it:** quit the application first - otherwise the log handler and the lock are recreated - then delete `~/.jtypewriter` and any `<document>.bak*` or leftover `jtw-*.tmp` next to your documents. There is no in-app "delete all data" command, and uninstalling the application does not remove this data.

Exact file names, encodings and limits are in [Data on disk](#data-on-disk).

---

## Keyboard Shortcuts

Shortcuts use `Ctrl` on Windows and Linux. Numpad `+`, `-` and `0` are accepted as equivalents of their main-keyboard neighbours.

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
| `Ctrl+P` | Print the saved document |
| `Ctrl+Shift+E` | Export the saved document as PDF |
| `Ctrl+F` | Find and replace card |
| `F3` | Find next (wraps) |
| `Shift+F3` | Find previous (wraps) |
| `Ctrl+H` | Find and replace card, replace field focused |
| `F11` | Toggle fullscreen |
| `Escape` | Exit fullscreen |

---

## Installation

Download the installer (`.deb` on Linux, `.msi` on Windows) or the portable `.tar.gz` from
[the GitHub Releases page](https://github.com/hyperpostulate/JTypeWriter/releases), together
with the `SHA256SUMS` file that lets you verify it.

- Both channels carry a bundled runtime, so neither needs Java installed.
- The `.tar.gz` is unpack-and-run: no installation step and no root rights.
- To build from source instead, see [Build and run](#build-and-run).

---

## Technical Reference

### Requirements

| Requirement | Version |
|-------------|---------|
| Packaged application | No Java required (bundled JDK runtime) |
| Building from source / fat JAR | Java 25+, Maven 3.8+ |

### Build and run

```bash
git clone https://github.com/hyperpostulate/JTypeWriter.git
cd JTypeWriter
mvn clean install
```

Everyday commands:

```bash
mvn clean javafx:run                 # Build and launch the application
mvn clean test                       # Run all tests
mvn clean package                    # Build + tests + thin JAR + fat JAR
./scripts/package.sh                 # Build + wrap into a native app image (releases/<os>/)
./scripts/package.sh --skip-build    # Reuse the last build, only re-wrap it
./scripts/package.sh --installer     # Same, plus the platform installer (.deb / .msi)
./scripts/verify-artifacts.sh        # Fresh build, then compare every shipped copy byte for byte
./scripts/smoke-appimage.sh --skip-build  # Start the built image; install, launch and remove the .deb
mvn test -Dtest=SessionStatsTest     # Run a single test class
mvn test -Dtest=FileServiceTest      # Run a single test class
```

Packaging produces two JARs: a thin one (application classes only) and a
`jar-with-dependencies` that bundles JavaFX together with its native libraries.
Because those natives come from the build machine, the fat JAR runs only on the
platform it was built on - it is a convenience artifact for `java -jar`, not a
cross-platform distribution and not what the native image ships.
`scripts/package.sh` builds a native image, and because it selects the host
platform it has to be run on each target platform - the release workflow runs it
once per OS. `mvn javafx:run` is the development entry point.

The native image is assembled from three parts, none of which is a fat JAR:

- the thin JAR, which is the application;
- a runtime built by `jlink` from a measured module list
  (`java.base`, `java.desktop`, `java.logging`, `java.scripting`,
  `jdk.unsupported`), so the image no longer carries a compiler, a JShell, a
  console or the whole JDBC/XML tail of the host JDK;
- the JavaFX module jars, placed in `lib/javafx` and resolved with
  `--module-path`/`--add-modules`, so JavaFX is a set of named modules instead
  of classes loaded from the unnamed module. The jars are bundled rather than
  linked into the runtime on purpose: `jlink` only carries `lib/` and `bin/` out
  of a modular jar, and JavaFX keeps its `.so` files at the jar root, so a
  linked JavaFX would start with a missing `libglassgtk3.so`. Their GPL text
  travels beside them in `lib/javafx/legal/`. The script writes that directory
  itself rather than passing it to jpackage's `--app-content`: on Linux that
  option copied the whole tree, on Windows the same invocation produced an
  image whose `lib/javafx` held the jars but not `legal/`, so the license
  silently disappeared from the Windows installer. The launcher option names it
  relative to `$APPDIR` (`$APPDIR/../javafx` on Linux, `$APPDIR/../lib/javafx`
  on Windows, both landing on `<image>/lib/javafx`), **not** through
  `$ROOTDIR`: that macro is expanded from `imageRoot`, which the launcher only
  sets while it runs from a plain app image, so in an installed `.deb` it
  expands to nothing and the module path became `/lib/javafx`.

The effect is measured, not asserted: the application image went from 147 MiB to
73 MiB, the `.deb` from 48 MiB to 44 MiB, and the launch log's
`Unsupported JavaFX configuration: classes were loaded from 'unnamed module'`
warning is gone.

`--installer` wraps that image into the platform installer (`.deb`, `.msi`) for
Linux and Windows - the same command the CI workflow runs. Every release
publishes both channels for each platform, the installer and a portable
`.tar.gz`; which one becomes the product default is still an open decision, so
`--installer` is one supported path, not the decided one. The application version
has a single source of truth: `pom.xml`. The script reads it back from the built
artifact and validates it against jpackage's rules.

`scripts/verify-artifacts.sh` closes the other half of that loop: it compares the
checked-in payload (`LICENSE`, `NOTICE`, `THIRD-PARTY-NOTICES`, `legal/`, the thin
JAR, the JavaFX module jars and each jar's own `META-INF` entries) against what
actually ships in the native image and in the installer, byte for byte, in one
command. It also asserts the image layout, because "the application starts" is
not evidence of it: the runtime must be the `jlink` one rather than the host JDK,
and `lib/javafx` must exist. The `.deb` payload is unpacked with `dpkg-deb` and
compared the same way, because `jpackage`'s deb bundler drops the image-root
files and `scripts/package.sh` completes the payload afterwards.

`scripts/smoke-appimage.sh` proves what the file comparison cannot: that the
artifacts actually run. On Linux it starts the application image under a
throwaway `user.home` (`Xvfb` when no display is available), waits for the main
window with `xwininfo` instead of sleeping, then installs the `.deb`, starts the
installed launcher and purges the package again; without root (or passwordless
`sudo`) it unpacks the `.deb` and launches the payload's launcher with a `dpkg`
stub, because that is what makes jpackage's launcher switch to its
installed-package code path; on Windows it starts the image
and requires the jpackage launcher to stay alive. Every launch it performs also
checks the log for `unnamed module`, which is the one symptom of JavaFX being
back on the classpath. The CI `smoke` job runs it on both legs against the
artifact the build job produced, so "packaged" is no longer shipped untested.

A wrong toolchain stops the build before it starts: `maven-enforcer-plugin` runs
at the `validate` phase, requires a JDK at least as new as `java.version` in
`pom.xml` (25) and Maven 3.8 or newer, and names the missing piece in one line
instead of failing later with a compiler error about `release 25`.

The launcher, window and taskbar icon is generated from the single source of the
artwork, `src/main/resources/icons/jtypewriter.svg`: `scripts/generate-icons.py`
regenerates the PNG set plus the Windows `.ico` together with
`icons.manifest`, and `scripts/package.sh` picks the right format per platform.
`--check` compares every asset's hash against the manifest, which catches the
one failure that matters - the artwork was edited and nobody re-ran the
script - on any machine, because it needs no rasteriser. `--render` is the
stronger, machine-local proof: it re-rasterises the SVG and compares pixels with
the committed assets (a perfect match is 0.00% difference, and it still fails on
a deliberate palette tweak at 0.33%). The script accepts either ImageMagick 7's
`magick` driver or the legacy `convert` of version 6, so regenerating works on a
developer machine and on a runner whose distribution still packages ImageMagick
6.

### Theme and language resources

CSS files are located in `src/main/resources/css/`:

| Theme | File | Description |
|-------|------|-------------|
| Base | `base.css` | Shared styles across all themes |
| Dark | `dark.css` | Dark background, light text |
| Light | `light.css` | Light background, dark text |
| Sepia | `sepia.css` | Warm sepia-toned background |

Localised text lives in one bundle per language, under `src/main/resources/`:

| Language | File | Code |
|----------|------|------|
| English | `messages.properties` | `EN` |
| Turkish | `messages_tr.properties` | `TR` |
| German | `messages_de.properties` | `DE` |
| French | `messages_fr.properties` | `FR` |
| Italian | `messages_it.properties` | `IT` |
| Spanish | `messages_es.properties` | `ES` |

### Data on disk

JTypeWriter's own data lives under `~/.jtypewriter/`; backups and temporaries sit next to the document itself:

| Path | Contents |
|------|----------|
| `settings.properties` | language, theme, text colour, font size, sound level, toolbar visibility, status bar size, window geometry, the last document and folder (absolute paths), the last document's caret and scroll position, and `schema.version` |
| `recovery/draft-<uuid>.jtw` | the live session's unsaved draft |
| `recovery/pending-<uuid>.jtw` | a draft the user postponed with "later", kept for the next start |
| `logs/jtypewriter-0.log` .. `-2.log` | three rotating generations of up to one megabyte each |
| `instance.lock` | the advisory single-instance lock; it carries no document content |

- A draft is a small header plus the text. The header records the absolute path the text came from, Base64-encoded so separators and newlines survive - encoding, not encryption - so a draft can outlive the document it came from.
- The recovery scan reads plain files only: a symlink named like a draft is ignored rather than followed, and a file above 64 MiB is skipped instead of being loaded at startup.
- Parked drafts expire after 30 days and the parked set is capped at 10 files / 64 MiB; an active `draft-<uuid>.jtw` is never expired, because it may belong to another running instance.
- A settings file that cannot be parsed is moved aside as `settings.properties.corrupt` and kept for diagnostics.

### Architecture

```
org.mesutormanli.jtypewriter
├── audio/
│   ├── TypewriterSound.java              # Typewriter keystroke audio playback
│   ├── SoundLevel.java                   # Volume ladder behind View > Sound
│   └── SoundPolicy.java                  # Which keystrokes click (pure)
├── locale/
│   ├── Language.java                     # Supported languages enum
│   ├── LocaleManager.java                # Language switching + listener support
│   └── Messages.java                     # ResourceBundle-based i18n accessor
├── print/
│   ├── DocumentLayout.java               # One pagination model for print and PDF (pure)
│   ├── PageGeometry.java                 # Page size and margins in points (pure value)
│   ├── PdfExport.java                    # Dependency-free PDF writer with the embedded mono face
│   ├── PdfWriter.java                    # PDF objects, streams and the xref table
│   ├── Printing.java                     # PrinterJob output, black 12 pt on an unstyled white page
│   └── TrueTypeFont.java                 # Minimal TTF reader: metrics, cmap, glyph ids
├── service/
│   ├── AutoSaveService.java              # Periodic draft snapshots + recovery offer
│   ├── AutosaveHealth.java               # Sticky autosave failure state (DRAFT_WRITE / DRAFT_PARK / DRAFT_DELETE)
│   ├── BackupKeeper.java                 # Rotating .bak backups before overwrites
│   ├── DialogService.java                # FileChooser & Alert dialog management
│   ├── ExitDisposition.java              # Exit-path rule for the recovery draft (CLEAR / PARK / KEEP)
│   ├── ExportService.java                # Save gate + PDF write for print/export
│   ├── FileService.java                  # Atomic file I/O (encoding-stable)
│   ├── RecoveryChoice.java               # Answer to the "recover draft" prompt (restore / discard / later)
│   ├── RecoveryRetention.java            # Age/count/size policy for parked drafts (pure)
│   ├── RecoveryStore.java                # Crash-recovery draft storage
│   ├── RecoveryTargetPath.java           # Validation of a draft's claimed original path (pure)
│   ├── StorageFootprint.java             # The disk cost of one saved document (pure)
│   └── TextCodec.java                    # Encoding detection & lossless round trips
├── stats/
│   ├── SessionStats.java                 # Writing session statistics (WPM, counts)
│   └── TextChange.java                   # One edit as the editor saw it (pure value)
├── ui/
│   ├── MainController.java               # Root layout controller
│   ├── MainMenu.java                     # File/Edit/View/Help menu button generated from EditorAction
│   ├── MainStage.java                    # Stage configuration (fullscreen, close)
│   ├── AppIcons.java                     # Window/taskbar icon set from /icons/
│   ├── AppFonts.java                     # Bundled UI font + headless measurement face
│   ├── WelcomeCard.java                  # Welcome card: shortcuts + greeting opt-out
│   ├── RecentFilesCard.java              # Recent documents card (opened from the File menu)
│   ├── AboutCard.java                    # About card with author info
│   ├── EditorAction.java                 # The command inventory: label, chord and effect per command
│   ├── KeyboardShortcutHandler.java      # Centralized keyboard shortcut handling
│   ├── CrashHandler.java                 # Global crash net: log + error dialog
│   ├── DropHandler.java                  # Scene-level file drop glue (resolve, open, report)
│   ├── DroppedFiles.java                 # Drop decision: one readable .txt/.md (pure)
│   ├── FindCommands.java                 # The find/replace port the command inventory exposes
│   ├── FindModel.java                    # Find/replace scan, cursor and replace decisions (pure)
│   ├── FindPanel.java                    # Floating find/replace card over the editor
│   ├── FindReplacePolicy.java            # Whether a replacement needs YOLO consent (pure)
│   ├── FindScanService.java              # Background find scan on its own daemon worker
│   ├── OverlayCard.java                  # Base class of the cards that float over the text
│   ├── OverlaySlot.java                  # At most one floating card visible at a time
│   ├── ShortcutRows.java                 # Shortcut-card rows generated from EditorAction
│   ├── ShortcutTable.java                # The two-column shortcut grid shared by cards
│   ├── SingleInstanceWarning.java        # "Already running" warning window
│   ├── DialogButtons.java                # Custom buttons for the application's Alerts
│   ├── DialogSizing.java                 # Alert sizing maths (pure)
│   ├── DialogStyling.java                # Theme stylesheets on an Alert's dialog pane
│   ├── component/
│   │   ├── EditorArea.java               # TextArea enforcing the YOLO policy
│   │   ├── YoloPolicy.java               # YOLO mode edit policy (pure logic)
│   │   ├── EditorKeyHandler.java         # Keystroke sound + stats tracking
│   │   ├── StatsBar.java                 # Bottom status bar (clock + stats)
│   │   ├── StatsBarModel.java            # Status-bar text composition (JavaFX-free)
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
├── AppVersion.java                       # App version, filtered from pom.xml at build time
├── AtomicFiles.java                      # Durable atomic writes (fsync + rename)
├── InstanceLock.java                     # Per-user instance lock (single instance)
├── IoExecutor.java                       # The one seam for blocking file I/O
├── RecentFiles.java                      # Welcome card's recent-document list (pure)
├── StartupArguments.java                 # Resolves the CLI/file-association argument (pure)
└── FxApplication.java                    # JavaFX application lifecycle bridge
```

#### Core Components

| Component | Description |
|-----------|-------------|
| `EditorArea` | TextArea subclass that routes every edit through the YOLO policy; loads documents through `setDocument` |
| `YoloPolicy` | YOLO mode edit policy (blocks deletions, allows insertions) |
| `DroppedFiles` | Pure decision behind a file drop: exactly one readable `.txt`/`.md` becomes the document to open; no file, several files, another type, a folder or a vanished path becomes a localized refusal |
| `DropHandler` | Scene-level drag glue: accepts the gesture as an event filter (so the text area cannot treat it as dragged text), resolves it through `DroppedFiles` and opens the document through the same unsaved-changes gate as Open |
| `FindModel` | Pure find/replace model: literal (non-regex) non-overlapping matches, case-sensitive or not without depending on the default locale, wrapping next/previous, replace-one and replace-all that re-scan after an edit, and a range that no longer fits the document is refused instead of applied |
| `StartupArguments` | Pure resolution of the process argument behind `JTypeWriter <file>`: a regular file becomes the document to open, while a missing path, a directory, an unparsable path or more than one argument becomes a localized refusal instead of a silent empty editor; a symbolic link keeps its own name |
| `EditorKeyHandler` | Handles keystroke sound effects and session stats tracking |
| `StatsBar` | Renders the clock and the *saved at ...* confirmation, both in the UI language's date/time formats, and live session statistics in the bottom bar; shows a persistent warning naming the autosave failure, with separate text for a stale draft, and its text size cycles on double-click |
| `KeyboardShortcutHandler` | Centralized keyboard shortcut routing |
| `MainMenu` | File/Edit/View/Help command menu in the toolbar; entries and accelerators are generated from `EditorAction` |
| `ToolbarView` | Top toolbar with all editor controls |
| `ToolbarFit` | Toolbar overflow policy: pure, JavaFX-free measurement that collapses the dropdown captions when the captioned row would not fit |
| `ToolbarState` | Manages YOLO mode and sound on/off state |
| `StatsBarState` | Persisted status-bar size; non-Node state over a neutral `AppSettings.StatsBarSize` |
| `AppIcons` | Window and taskbar icon set loaded from `/icons/`; a missing asset never breaks startup |
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
| `AppVersion` | The application version from `pom.xml`, delivered as a filtered resource so About, the settings schema version and the release tag are one number |
| `AtomicFiles` | Durable atomic file writes (fsync, rename, directory flush) |
| `ThemeManager` | CSS theme loading and switching |
| `FontSizeManager` | Editor font size state (10-48px) |
| `TextColorManager` | Text color cycling state |
| `SessionStats` | Real-time writing statistics (WPM, word/char/line count) |
| `TypewriterSound` | Typewriter keystroke audio playback |
| `SoundLevel` | The `View > Sound` volume ladder; `OFF` is the persisted silence |
| `SoundPolicy` | Pure decision of which keystrokes click, shared by the key handler and its tests |
| `LocaleManager` | Multi-language support with change listeners |
| `Messages` | Resource bundle-based message accessor |

#### Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| JavaFX Controls | 26.0.1 | UI components |
| JavaFX FXML | 26.0.1 | FXML loading |
| JavaFX Media | 26.0.1 | Audio playback |
| JUnit Jupiter | 6.0.3 | Test framework |
| Mockito | 5.23.0 | Test mocking |

JavaFX Graphics and Base come in transitively with Controls. Dependency injection is framework-free: `AppGraph` wires the object graph manually.

### Testing

#### Running Tests

```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=SessionStatsTest

# Single test method
mvn test -Dtest=SessionStatsTest#wordCountIsCorrect
```

#### Test Classes

| Test Class | Coverage |
|------------|----------|
| `SessionStatsTest` | Word count, char count, line count, keystrokes, reset, WPM from active typing time (idle decay regression) |
| `FileServiceTest` | Unsaved-change handling on open and on a new document (cancel/discard/save-first), atomic writes, rotated backups, encoding round trips incl. the UTF-8 upgrade for unrepresentable characters, encoding reset for a new document, lifecycle listeners, recovery attachment, and the print/export gate: the text handed back is read from the file on disk (never the editor buffer), a cancelled save returns nothing, a clean document reads without a prompt, and a read failure reports instead of printing |
| `ExportServiceTest` | The user-facing half of print and export: an empty gate writes nothing and opens no chooser, a chosen target receives exactly the renderer's bytes, a renderer failure reports through the alert and leaves no half-written file behind, and the PDF dialog is never reached before the save gate has produced a file |
| `TextCodecTest` | Encoding detection (BOMs, strict UTF-8), legacy prompts, lossless encoding round trips, strict encode (unrepresentable characters fail instead of turning into `?`) |
| `EncodingMatrixTest` | The open→save and open→edit→save byte matrix per encoding (BOM-less UTF-16LE/BE, Windows-1254 via the legacy prompt, mixed UTF-8 with CRLF, tab and emoji), pinning that a valid-UTF-8 file with NULs on both parities stays UTF-8 and that BOM-less UTF-16 never reaches the legacy prompt |
| `BackupKeeperTest` | Pre-overwrite backup rotation and retention |
| `RecoveryStoreTest` | Crash-recovery draft lifecycle (save/restore/drop) in one atomic per-session `.jtw` file, newest-draft adoption, isolation between concurrent sessions, parking a postponed draft so the live slot stays free, tolerance for corrupt headers, bad timestamps, unusable paths and invalid UTF-8; a read-only directory makes `clear()` report failure and log a `WARNING` |
| `RecoveryRetentionTest` | The parked-draft housekeeping rule as a pure function: age, count and size budgets, the newest entry surviving a budget it alone exceeds, and an oversized entry not discarding a smaller one behind it |
| `AutoSaveServiceTest` | Opening or creating a document drops the previous document's stale recovery draft; a postponed draft survives an exit and no longer silences autosave of the new document (regression); an answered exit clears the draft, an unasked exit parks it, a frozen slot keeps the file untouched, and failed snapshots or parked drafts report health; a failed delete keeps the warning without a dialog and a later successful delete withdraws it; a draft older than the document on disk is restored without its path |
| `ExitDispositionTest` | The four-way exit matrix: answered vs unasked exit against a live vs frozen slot, with a frozen slot winning over every other signal |
| `DroppedFilesTest` | The drop decision: `null`/empty file list, one file, `.md`, an upper-case extension, several files (refused with the count), a PDF and an extension-less file, a path that vanished before the drop, a directory named `*.txt`, a symlink that keeps its name, and the suffix check rejecting names that only end with the same letters |
| `FindModelTest` | The find/replace model: literal query and replacement, non-overlapping scan, case sensitivity under a Turkish default, wrapping in both directions, a search restarting before the first match, a background scan that is stale or oversize being dropped, replace-all not rescanning its own output, replace-one selecting the next match past the inserted text, and a range past the end being refused |
| `DropContractTest` | Source-level contract for the drop: `MainStage` installs it, it filters `DRAG_OVER`/`DRAG_DROPPED` on the whole scene, it opens only through `FileService.openPathAsync` (no second save prompt, no direct save), it hands the loaded text to the editor, and no source outside `FileService` asks the unsaved-changes question |
| `StartupArgumentsTest` | The `JTypeWriter <file>` decision: no/blank argument, an existing regular file, a relative path, a missing path, a directory, two arguments, a symlink that keeps its name, and text the platform cannot parse |
| `AutosaveHealthTest` | Sticky failure state (`DRAFT_WRITE` / `DRAFT_PARK` / `DRAFT_DELETE`) that survives repeated failures, clears on a successful write and notifies once per real transition |
| `ProcessCrashRecoveryTest` | Linux-only end-to-end kill test: `CrashHarness` (a helper, not a test class) builds the real service graph in a child JVM without starting JavaFX and snapshots a dirty document through the production autosave path; the parent then kills it with `SIGKILL` (`Process.destroyForcibly`) and a second JVM must recover the exact non-ASCII text. A paired case proves an answered close still drops the draft, and `-Duser.home` keeps logging inside a temp home. Synchronisation is the stdout marker plus `waitFor`, never `Thread.sleep` |
| `YoloPolicyTest` | YOLO mode edit policy (deletions blocked, insertions allowed, safe replacement) |
| `FindReplacePolicyTest` | When a replacement needs the user's consent: YOLO on requires the prompt, YOLO off asks nothing |
| `FindScanServiceTest` | The scan runs off the FX thread and its result is delivered only through the injected FX dispatcher (`fxThreadScans()` stays zero) |
| `FindPanelContractTest` | Source contract of the find card's `reveal`: a match is selected with `selectRange` and never followed by `positionCaret`, which JavaFX implements as `selectRange(pos, pos)` and would collapse the selection |
| `KeyboardShortcutHandlerTest` | Shortcut decoding incl. shift-sensitive combos (Ctrl+Shift+Y / Z / C / T / S), Ctrl+N for a new document, the find chords (Ctrl+F / F3 / Shift+F3 / Ctrl+H, where F3 and Shift+F3 must stay distinct) and numpad equivalents |
| `MainControllerTest` | Window title composition (application + document name, unsaved `*` marker, `[YOLO]` badge), the Edit menu's find/replace placement, and the command-menu contract: `MainMenu.layout()` carries every platform-independent `EditorAction` exactly once across the four pages (six settings commands are represented by their value submenu instead), the View submenus list and check the same values the toolbar dropdowns offer, and every page title and row label is non-empty in all six languages |
| `MainStageTest` | Off-screen window position guard (`intersectsAny`: restored positions on detached monitors) |
| `AppIconsTest` | The icon set on the classpath: every PNG decodes to the declared size, and the `.ico` file starts with its expected magic bytes (pure byte checks, no toolkit) |
| `AppVersionTest` | The version is one number: the filtered resource is read and trimmed, a missing or blank value is empty rather than throwing, `get()` is never blank, and every reader (About, the settings schema) agrees with `pom.xml` |
| `AppSettingsTest` | Preference persistence across restarts, restored toolbar/fullscreen/status-bar-size state and their corrupt-value fallbacks, defaults, tolerance for unknown values and unusable paths, quarantine of a corrupt settings file instead of silently overwriting it, the settings schema version, the sound level (including the legacy on/off key read as a level), the last document's caret/scroll position and its file scoping, and the recent-document list plus the welcome opt-out |
| `AppDirsTest` | The data-directory layout: an unset or blank `user.home` falls back to an absolute temp-based data directory instead of throwing, and a real home yields `<home>/.jtypewriter` with `recovery/` and `logs/` under it |
| `StartupGreetingTest` | When the welcome card may arrive on its own: a fresh install is greeted, a stored opt-out keeps it away, and a document named on the command line is never covered by it |
| `RecentFilesCardTest` | How a recent-document row is named: the file name, falling back to the whole path when a path has no last element |
| `RecentFilesTest` | The recent-files card's document list as pure list operations: newest first, one row per document by absolute normalised path, the ten-entry cap dropping the oldest entry, vanished documents filtered out, and empty/null handled |
| `StatsBarStateTest` | Persisted status-bar size: starts from the setting, a fresh install starts at medium, cycling persists each step, a corrupt value degrades to medium, the mapping covers both enums both ways |
| `StatsBarModelTest` | The status bar's text logic, headless: populated immediately after construction, refresh picks up new statistics, a language switch refreshes before notifying and re-renders the clock and the warning, and each autosave failure kind shows its own warning |
| `FontSizeManagerTest` | The font-size ladder, off-ladder sizes inserted without changing the setting, ladder stepping for Ctrl+±, clamping to the supported range and the persisted reset to the default size |
| `ToolbarOptionListsTest` | The six toolbar dropdowns: option order, no duplicates, and every dropdown able to display its own current value |
| `AtomicFilesTest` | Durable atomic writes: content integrity, no temp leftovers, cleanup after failures |
| `MessagesTest` | i18n bundle parity across all 6 languages, localised theme/colour names, locale-independent bundle resolution |
| `DialogServiceTest` | Save-as file naming rules (default extension handling) |
| `DialogButtonsTest` | The shared alert buttons: primary is OK/Done, cancel is Cancel/Close, the alternative is the third action, label text is preserved verbatim, exactly one default and one cancel, and the recovery dialog puts Cancel on Later rather than Discard |
| `DialogSizingTest` | Alert sizing maths: the preferred size is returned unchanged when it fits, clamped into a 768 px screen, floored on an absurdly small screen, with symmetric margins, and the shared content column leaves room for the dialog chrome |
| `DialogStylingTest` | Every Alert is themed: stylesheet order per theme (base then light/dark/sepia), every listed stylesheet resolves on the classpath, `base.css` themes the dialog pane, and each theme defines the tokens dialog panes read |
| `JTypeWriterAppTests` | Main method presence |
| `AppLoggingTest` | Log formatter builds its header without throwing (regression) and appends an indented stack trace with the cause chain for thrown exceptions; a log-file handler in a temp directory, an unusable log directory and a denied home falling back to console only, and repeated `init()` not stacking handlers |
| `InstanceLockTest` | Instance-lock acquisition, rejection of a second acquire in the same JVM, release and idempotent close, fail-open behaviour for an unusable lock file, and a read-only data directory reporting `UNAVAILABLE` instead of blocking startup |
| `LanguageTest` | Native language names are stable code constants under every system locale (no `getDisplayLanguage` trap) |
| `ThemeCycleTest` | The theme cycle: enum order matches the toolbar, Dark is the default, `next` wraps back to the start, and every bundle documents the cycle starting at the default |
| `ToolbarStateTest` | The sound level's single setter: persistence across a restart, the audio engine follows, zero silences both, On restores the last audible rung, repeats are no-ops |
| `ToolbarFitTest` | Toolbar overflow policy: how the captioned row is measured (buttons, dropdowns, spacer, chrome) with injected metrics, the FULL→COMPACT switch and its 40 px hysteresis, plus acceptance tests proving German and French are not clipped at 1280 px and that the collapsed row fits 900 px in all six languages, and that the window's minimum width is the widest collapsed row measured across the six bundles |
| `AppFontsTest` | The bundled fonts are release assets: the chrome faces and the monospace face are on the classpath under the families `base.css` names (`.root` and `.editor-area` respectively), they render every toolbar label in all six languages (including the `☰` menu glyph), the monospace face really is monospaced, and a pinned string measures to a pinned width, so a fallback to a system font fails the build |
| `DocumentLayoutTest` | The one pagination model print and PDF share: greedy wrapping at the measured width, explicit and trailing newlines, tabs expanded to four-cell stops, a run wider than the text block broken at code-point boundaries with surrogate pairs intact, an exact-fit line kept whole, the baseline arithmetic and its reset on every page, overflow onto a new page, an empty document laid out as one empty page, and `PageGeometry`'s margin maths including geometry that leaves no room |
| `PdfExportTest` | The PDF writer against the file it emits: header and trailer, every xref offset landing on its `<n> 0 obj`, every stream's `/Length` matching its bytes (Flate streams inflated), an A4 `/MediaBox`, one page per layout page, the embedded `/FontFile2` inflating to the bundled monospace TTF byte for byte, `/ToUnicode` mapping glyphs back to code points (Turkish characters included), the white sheet and black text painted in every content stream, and two renders of the same text being byte-identical |
| `TrueTypeFontTest` | The minimal TTF reader behind export and print: `unitsPerEm`, a positive glyph count, 'A' and 'ş' mapping to real glyphs, equal advances for 'i' and 'W' proving the face is monospaced, a positive ascent, and non-font bytes rejected instead of parsed as garbage |
| `PackagingNoticesTest` | The packaging contract: `META-INF/THIRD-PARTY-NOTICES` and all three license texts are on the classpath, the notice names every bundled component with its real SPDX id, states that FFmpeg is not distributed, stays LF-only within 100 columns, and is byte-identical to the checked-in file so a stale build output cannot pass |
| `ContrastRatioTest` | WCAG contrast maths (black-on-white 21:1, identical colours 1:1, the 4.5 boundary) and CSS-parsing guards that fail the build if status text, the autosave warning, the toolbar text, a plain popup row or the default editor text drops below AA or 13 px (or a theme declares a token `base.css` never reads), the focus ring drops below 3:1, highlighted popup-row text drops below AA, the popup cell font size drifts from the closed combo's, the focus colour becomes transparent again, or a non-existent JavaFX CSS property (`-fx-letter-spacing`, `-fx-transition`) returns |
| `CiGatesTest` | The CI gate contract: the shared workflow really invokes the icon freshness check and the artifact verification, the icon check runs before the build and on both OS legs without re-rasterising, the artifact check runs after packaging, that artifact gate stays on the Linux leg (it unpacks the `.deb`), both `maven.yml` and `release.yml` call the shared workflow rather than keeping their own step lists, and the release downloads only the normalised `dist/` assets |
| `ReadmeTestTableTest` | The test-class table is derived, not remembered: every `*Test.java` under `src/test` has a row in both READMEs, the Turkish table is as complete as the English one, and no row names a class that no longer exists |
| `ReleaseNotesTest` | The release contract: every artifact upload carries a short retention, the release body comes from the checked-in template, the template stays bilingual with no literal version and the same verification commands in both halves, the note measures download sizes, and the workflow can dry-run while a manual run cannot publish |
| `ReleaseAssetsScriptTest` | The portable-archive script: a drive-lettered path is not read as a `tar` host name, and plain paths still produce an archive with a single top-level directory |
| `LicenseHeaderTest` | The SPDX header sits at byte 0 of every `.java` file under `src/`, before `package`, and the canonical icon SVG declares the same licence |
| `EditorActionTest` | The command inventory: every constant performs exactly its own effect through a recording `Context` (no toolkit), every constant carries a label and at least one chord, only `EXIT_FULLSCREEN` is left to the platform, the toolbar buttons run the shared actions, and a source scan fails if a chord literal is redeclared outside `EditorAction` |
| `ShortcutRowsTest` | The shortcut card is generated from the inventory instead of being copied: two-way coverage with `EditorAction`, the published row order, a localised label and description per row, and no chord twice on the card, in all six languages |
| `EditorUndoContractTest` | The undo/redo contract without a toolkit: no production source reaches into JavaFX's private undo-manager fields, and loading a document still replaces the text in a single programmatic call |
| `OverlayCardTest` | The floating cards' dismissal contract: Escape and Enter dismiss a card, any other key is left to the card |
| `OverlaySlotTest` | At most one floating card at a time: showing one hides the others without destroying them, registering the same card twice hides it once, and an unregistered owner hides every member |
| `AudioAssetTest` | The bundled keystroke sample is a release asset: on the classpath, its format and SHA-256 match `PROVENANCE.md`, it is an audible transient rather than silence, and the provenance states authorship and licence |
| `SoundLevelTest` | The volume ladder: rungs ascend from silence to full, only `OFF` is silent, and an off-ladder persisted volume resolves to its nearest rung |
| `SoundPolicyTest` | Which keystrokes click: typing a character clicks, an undefined character is silent, only the deleting keys click on press, and a shortcut chord is silent whatever its key |
| `StatsPerfHarnessTest` | The ceiling behind the incremental counters: one real `StatsBarModel.refresh` (formatting included) on a 10 MiB document stays under 50 ms in CI. The whole-text version measured ~164 ms once a second, so an O(n) refresh coming back fails the build |
| `FxThreadIoTest` | The FX-thread invariant without starting a toolkit: opening, saving and autosaving reach the disk through `IoExecutor` only (`fxThreadTasks()` stays zero), a clean autosave tick never copies the document text, and text typed while a save is in flight stays dirty afterwards |
| `RecoveryTargetPathTest` | The guard between a recovery draft's header and the next save: the path must be absolute, canonical, under an allowed root and outside the app's own data directory. A relative path, a traversal escape, a dangling symlink or a path without allowed roots is dropped (fail closed) while the recovered text is still offered |

### CI/CD

GitHub Actions workflows: build and test on every push and pull request to `main` (`.github/workflows/maven.yml`), release on a version tag (`.github/workflows/release.yml`). Neither defines the gates itself: both call the shared `.github/workflows/verify-build.yml`, so a tag runs the same icon check, locale runs, byte comparison and smoke launch a branch does.

- **Trigger**: Runs on every push and pull request to `main`
- **Environment**: Ubuntu and Windows (build matrix)
- **JDK**: Temurin 25
- **Command**: `mvn -B package` (build + tests), then `scripts/package.sh --skip-build --installer`
- **Gates**: `scripts/generate-icons.py --check` runs before the build on both OS legs: it compares every icon's hash with `icons.manifest`, so a committed asset that no longer matches the canonical artwork fails in a minute rather than shipping. It verifies hashes rather than re-rasterising on purpose - ImageMagick encodes the same pixels to different bytes on different builds, so a byte or pixel comparison in CI would compare toolchains, not artwork. `scripts/verify-artifacts.sh --skip-build` runs after packaging (Linux leg, because it unpacks the `.deb` with `dpkg-deb`) and re-proves the byte-for-byte comparison between the repository payload, the application image and the installer payload. A separate `smoke` job then consumes the packaged artifact and runs `scripts/smoke-appimage.sh --skip-build` on both OS legs: Linux starts the image under Xvfb and installs, launches and purges the `.deb`; Windows requires the launcher to stay alive
- **Locales**: the test suite also runs under Turkish and German system locales, so i18n regressions (e.g. a language that silently resolves to the system language) cannot slip through
- **Artifacts**: native application image plus a platform installer (`.deb` / `.msi`) per OS
- **Releases**: pushing a `v*` tag that matches the `pom.xml` version builds both platforms and publishes them to a GitHub Release together with a `SHA256SUMS` file, so the binaries no longer live in Actions artifacts that expire after a few days. The same workflow can be started by hand as a **dry run**: `prepare`, both OS builds, the asset/source/checksum assembly, the five-asset assertion and the release-note rendering all run, but publication is replaced by an artifact holding exactly the assembled set. A manual run cannot publish - a release from a branch tip could not be reproduced from its tag - so `dry_run` defaults to true and asking for false is refused in `prepare`. A published release is a normal release, not a pre-release: `v1.1.0` dropped `--prerelease` together with the early-access sentence in the note, because the project now publishes a version it calls ready to use. The flag and the sentence are two claims about the same thing, so `ReleaseNotesTest` pins them as a pair - no flag in the publish step and no early-build wording in either half of the note - and one cannot come back without the other

---

## License

JTypeWriter is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for the full text and [NOTICE](NOTICE) for attribution notices. [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) lists every bundled component with its license id, copyright line and source, and the full texts sit in [legal/](legal); both ship inside the fat JAR as `META-INF/THIRD-PARTY-NOTICES` and `META-INF/licenses/`, and at the root of the native image. The bundled JavaFX libraries stay under GPLv2 with the Classpath Exception and GStreamer Lite under LGPL-2.1-or-later; the OpenJDK runtime carries its own notices under `lib/runtime/legal/`. The bundled keystroke sound (`src/main/resources/audio/keystroke.wav`) is the author's own work, contains no third-party sample, and is covered by the same Apache-2.0 licence; its provenance and SHA-256 are recorded in [src/main/resources/audio/PROVENANCE.md](src/main/resources/audio/PROVENANCE.md). The bundled fonts are DejaVu Sans Condensed (regular and bold, the UI chrome) and DejaVu Sans Mono (the editor, printing and PDF export), unmodified, under the Bitstream Vera licence; the monospace file is also embedded in every exported PDF, which its installable-embedding permission allows. Their provenance and file hashes are in [src/main/resources/fonts/PROVENANCE.md](src/main/resources/fonts/PROVENANCE.md).

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
