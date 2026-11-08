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

JTypeWriter opens a single plain-text document and then stays out of the way: no accounts, no cloud, no project format. Your text lives in the file you picked, in the encoding that file already had. The packaged download carries its own Java runtime, so nothing has to be installed first; building from source or running the fat JAR needs JDK 25 and Maven 3.8+. It runs on Linux and Windows, and the interface speaks English, Turkish, German, French, Spanish and Italian.

**What you get?**

- **A draft you cannot wreck by accident.** YOLO mode turns off Backspace, Delete, Cut and Undo/Redo so you can only move forward. Every save is atomic and keeps rotating backups, and unsaved work is snapshotted every 30 seconds - after a crash the draft is offered back, and closing that prompt never deletes it.
- **The feel of typing.** A typewriter keystroke when you type, backspace or delete - at a volume you set - plus three themes (dark, light, sepia) and six text colours, all switchable while you write.
- **Numbers as you go.** Words, characters with and without spaces, lines, keystrokes, session time and a sustained words-per-minute figure that ignores your breaks.
- **Only text on screen.** `F11` goes fullscreen, `Ctrl+T` hides the toolbar along with the command menu it hosts, and the window title tells you which document is open and whether it is saved.
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

Authentic typewriter keystroke sound when a key puts text on the page. Caret movement, function keys and shortcuts stay silent, because a typewriter only clacks when it writes or strikes through. The toolbar's Sound dropdown is a quick on/off; `View → Sound` opens the level ladder - Off, 25%, 50%, 75%, 100% - and the choice is remembered across restarts, with 0 completely silent. Each click starts its own voice instead of cutting off the previous one, so fast typing sounds like fast typing rather than a stutter.

### YOLO Mode

"YOLO Mode" (You Only Live Once) prevents accidental deletion. When enabled, Backspace, Delete, Cut and Undo/Redo are disabled (the right-click menu shows Cut greyed out, with only Copy/Paste/Select All usable), so no edit can remove characters that are already in the document. Perfect for first-draft writing where you push forward without looking back. The mode stays visible through the `[YOLO]` title badge and the toolbar's YOLO dropdown; theme colours stay untouched in both states.

### Real-Time Statistics

Live stats bar showing:
- Word count, character count (with/without spaces), line count
- Total keystrokes
- Session duration (HH:MM:SS)
- Words per minute (WPM) - sustained speed based on active typing time, so breaks do not drag it down
- The status bar text is 13 px by default and cycles between small, medium and large when you double-click it; its colour meets WCAG AA contrast (at least 4.5:1) in all three themes
- A successful save stamps the status bar with its time in the UI language's own clock format (e.g. `Saved 12:04 PM` in English, `Kaydedildi 12:04` in Turkish); it clears on the next edit, and it marks a real save rather than the 30-second autosave draft snapshot

### Fullscreen & Toolbar

- **`F11` takes over the screen, `Escape` gives it back:** `F11` toggles fullscreen and `Escape` leaves it (the platform's default exit hint is suppressed)
- **The toolbar can disappear completely:** `Ctrl+T` removes it when you want nothing but text on screen
- **The toolbar never wraps to a second row:** at every width it stays one horizontal row. At the tested window sizes the six dropdowns show a caption with their value ("Theme: Dark", "Size: 16", "Language: Deutsch") and nothing overflows - fully captioned at 1280 px, captions collapsed to the bare value ("Dark", "16", "Deutsch") at 900 px in all six languages, measured from the actual localised labels. The window cannot be narrowed below the widest collapsed row plus the window decoration (889 px, from the Spanish labels), so the row never has to clip: the minimum width is measured from the six bundles, not guessed, and a translation that grows a label fails the test that pins it. A window size remembered from an older build that is narrower than that comes back at the minimum, and the stored value repairs itself at the end of that session. The command-menu button and the six plain buttons (find, open, save, undo, redo, about) never collapse and always stay visible
  - **Every control is reachable and explained.** `Tab` follows the pinned visual order: Menu, Find, Open, Save, Undo, Redo, Theme, YOLO, Sound, Color, Size, About, Language. The order is set with `viewOrder`; JavaFX documents that property for rendering and picking, so the focus order is an application convention rather than a toolkit guarantee. Each control keeps a visible focus ring and a tooltip from the UI language that includes the shortcut where one exists, such as "Save (Ctrl+S)"
- **Your layout is remembered:** the toolbar's shown or hidden state, fullscreen and the status bar's size all survive a restart, and a choice is saved the moment you toggle it rather than only on exit
  - **Fullscreen never fights the window geometry.** A window left in fullscreen comes back in fullscreen; fullscreen wins at startup over the remembered position and size, and leaving it later lands on the remembered position

### Command Menu

The leftmost toolbar control is a `☰` button holding four menus - File, Edit and View with the editor commands, Help with About. It is a menu button rather than a permanent menu strip: a second chrome row would follow you into fullscreen and undermine `Ctrl+T`, so the menu hides with the toolbar it lives in. Every editor command the platform does not handle itself is generated from the same `EditorAction` inventory as the shortcuts and toolbar buttons, so `New` (`Ctrl+N`) and `Save As` (`Ctrl+Shift+S`) are now discoverable instead of keyboard-only. View is not a list of cycling commands: Theme, YOLO, Text Color and Font Size each open a second-level submenu listing the same values as the toolbar dropdowns, with the current one checked - `View → Theme → Dark` means Dark, not "the next theme". Sound opens the level ladder (Off, 25%, 50%, 75%, 100%); the toolbar's On/Off dropdown is a shortcut over that same value rather than a second setting. They are generated from the same option lists and write through the same apply paths as the dropdowns, so the two entry points can never disagree. Labels and accelerators follow the UI language (the accelerators are descriptive - the key filter performs them), the open popup is painted from the same theme tokens as the toolbar dropdowns so it follows Dark, Light and Sepia, and Help → About opens the About card. Because the menus are generated rather than hand-written, a command can no longer exist in one entry point and be missing from another.

### Find & Replace

`Ctrl+F` opens a compact find/replace card in the window's top-right corner, over the text; `Ctrl+H` opens the same card with the replace field focused. It is modelled on Notepad++'s dialog: one "Find what" field, one "Replace with" field, a "Match case" checkbox, Find Next / Find Previous / Replace / Replace All, and a live status line ("3 of 12", "12 matches", "No matches"). The card is a panel rather than a modal window because finding is a mode: the document stays visible while you type a query and press `F3` repeatedly, and closing the card with `Escape` or its Close button keeps the query for the next `F3`.

- **The same commands exist everywhere:** the toolbar's `⚲` button, the Edit menu (`Find`, `Find Next`, `Find Previous`, `Replace`) and the four shortcuts all run one `EditorAction`, so they cannot drift.
- **F3 and Shift+F3 walk the matches** and wrap in both directions, and they keep working after the card is closed. With no query yet, `F3` opens the card instead of doing nothing.
- **Search is literal text, never a regex:** `a.*b` means those five characters and `$1` is four characters, so there is no invalid-pattern mode and no second error path.
- **A 10 MB document never freezes the window.** The scan runs on its own worker and is installed back on the FX thread only if it still describes the current text, query and case mode; a result that arrives after you typed another character is dropped. Measured with the real model on a 10 MiB document: 26-53 ms for a whole-document scan (0 matches, 197 845 matches, and 1 187 069 matches for a single-letter query), none of it on the FX thread.
- **Replace All is one undo step:** the new text is computed from the original matches and applied as a single edit, so `Ctrl+Z` takes the document back in one move. Replacing does not search its own output, so replacing `a` with `aa` terminates.
- **YOLO mode asks first.** The mode promises that no edit removes characters; a replacement removes the matched range, so with YOLO on the card asks once per operation and cancelling writes nothing. The change is then a normal, undoable edit.
- **The card follows the theme:** surface, border, fields, checkbox, buttons and status line resolve through the same tokens as the toolbar, so it is native in Dark, Sepia and Light - including the input backgrounds, which Modena would otherwise paint white in the dark theme.

### Welcome, Recent Files and About Cards

The welcome view, the recent-files view and the About view are cards in the same top-right corner as the find panel, not windows of their own. They share that corner with it: opening one closes whatever card was there, so what you see is always exactly the card you last asked for.

- **The document stays usable.** The card is not modal, so a shortcut can be copied out of it while it is open, and closing it puts the caret back in the text.
- **`Escape` and `Enter` dismiss it**, exactly as they did when it was a dialog, and so does the `Close` button in the card's top-right corner - a word, like the find panel's. That button is the card's only one: the content is there to be read, not to be dismissed through a second call-to-action at the bottom. The close button takes focus when the card opens, so either key works immediately; `Tab` still walks the card's controls.
- **It is bounded by the window, not by the screen.** A card taller than a short window shrinks to it and scrolls, instead of running past the edge, and it is painted from the same theme tokens as the toolbar, so it is native in Dark, Sepia and Light.
- **It speaks the language it is opened in.** The content is rebuilt from the bundle on every opening and on a language switch, so a card that is up cannot keep the previous language after you change it.
- **The greeting can stop greeting you.** The "don't show this again" checkbox on the welcome card is a stored preference, so the card no longer opens at launch once it is ticked. `File → Show Welcome Card` is the way back: it clears the preference and opens the card immediately, so the opt-out is never a door you cannot reopen.

**The recent list is a card of its own**, opened from `File → Recent Files`. It used to sit on the welcome card, which tied the list to a greeting the user might switch off: someone who does not want to be greeted still wants their files. Keeping the two apart means the greeting can be dismissed for good without losing the list, and the list can be reopened at any time.

- **Ten documents, newest first.** Opening the same document again moves it to the top instead of adding a second row, and the same file reached through a relative path or `..` is recognised as the same document. When the list is full, the oldest entry falls off - never the newest.
- **A row is named by its file name**, so the list stays readable and does not put your home directory on screen; the full path is the row's tooltip. A card with nothing in it says so rather than showing an empty panel.
- **It opens through the same gate as `Open`.** Cancel, a failing save and the encoding question all behave exactly as they do from the menu or a drop - the card names the next document, it is not a second way to discard the one in the editor.
- **A document deleted outside the app leaves the list silently** instead of becoming a row that fails when clicked.

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
  - **Some paths log only.** A failing keystroke sound and a failed undo-history reset are logged without a dialog; a failed settings write raises one warning that names the file, because otherwise every preference changed from then on would be lost silently. A failed draft delete raises a status-bar warning, not a dialog.
  - **Bad state is set aside, not overwritten.** A corrupt settings file is moved aside as `settings.properties.corrupt` and its content kept for diagnostics.
  - **Logging cannot block startup.** If the log file cannot be created - a home directory the JVM denies or a read-only log location - the app starts with console logging only.

### Settings & Session Restore

- **What is remembered:** language, theme, text colour, font size, sound level, the last document and its folder, and the window geometry are persisted to `~/.jtypewriter/settings.properties`, so the next session starts where the last one ended - same look, same document, same window
  - **Nothing blocks startup.** A document that has disappeared in the meantime is skipped silently, and a broken settings file falls back to the defaults instead of blocking startup
  - **The file carries its own schema version.** `schema.version` lets a file written by an older build open in a newer one without losing values, while a file from a newer build is read as far as this build understands it and is never downgraded. Migration happens in memory; the file is rewritten only by the next real change
  - **A settings file that cannot be written is reported once.** The window keeps working and your documents are untouched; the warning names the file and says that language, theme, font size and window position will not be remembered after this session
  - **YOLO mode is deliberately *not* persisted.** A mode that blocks deletions should not still be armed at the next start

### Opening Documents from Outside the App

`JTypeWriter notes.txt` opens that document directly, and a file association or double-click hands the same path through the same code, so both entry points behave alike. Only one file can be named: a second argument is refused with a dialog that names it, rather than silently picking the first. A path that does not exist, is not a regular file, or cannot be parsed as a path is reported in the UI language - startup never silently falls back to an empty editor, which would look exactly like a lost document. With no argument the welcome card appears unless you turned it off with its own checkbox (the File menu reopens it). A recovered draft still wins over the command-line document: it is the only copy of that text, while the named file is still on disk. A symbolic link keeps its own name; the save path already follows the link to its target. Starting a second instance while one is running is still refused by the single-instance lock - there is no hand-off, so the already-running window is not asked to open the file.

**Dropping a file works too, and means the same thing.** Drag a `.txt` or `.md` onto the window - the text, the status bar, anywhere - and it opens through the same code as Open: a dirty document is asked about first, and Cancel or a cancelled save leaves the editor exactly as it was, so a drop is never a shortcut past the save prompt. The whole window is the target because that is where the pointer happens to be, and the gesture is intercepted before the text area can treat it as dragged text. Plain-text drags inside the editor still work as before. Zero files, several files at once, a folder, a PDF or a file that vanished between the drag and the drop are each refused with a dialog naming what was dropped - a drop that silently did nothing would look like a broken window.

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

6 languages: English, Turkish, German, French, Italian, Spanish. Toolbar, dialogs, the window title and the status bar update immediately when the language is switched, and every date and time is rendered in that language's own format rather than a fixed pattern.

---

## Your Data

Everything JTypeWriter keeps stays on your machine. There is no account, no telemetry, no update check and no network request: the application itself sends nothing, and the only outbound action is the e-mail link in the About card, which hands a `mailto:` address to your own mail client.

- **Where it lives:** `~/.jtypewriter/` - `settings.properties` (language, theme, text colour, font size, sound, toolbar visibility, status-bar size, window geometry, and the last document and folder as absolute paths), `recovery/` (crash-recovery drafts), `logs/`, and `instance.lock`, the advisory single-instance lock that carries no document content.
- **Drafts contain your text.** Each `recovery/draft-<uuid>.jtw` holds the unsaved document verbatim; a postponed draft is parked as `pending-<uuid>.jtw` and survives a clean exit. A draft also records the absolute path the text came from, Base64-encoded so separators and newlines survive - that is encoding, not encryption - so a draft can outlive the document it came from. The scan only reads plain files: a symlink named like a draft is ignored rather than followed, and a file above 64 MiB is skipped instead of being loaded at startup. Parked drafts are housekeeping - they expire after 30 days, and the set is capped at 10 files / 64 MiB - while an active `draft-<uuid>.jtw` is never expired, because it may belong to another running instance.
- **Next to your documents:** an overwrite keeps `<document>.bak`, `.bak.1` and `.bak.2`, and a save stages a `jtw-<name>*.tmp` file beside the document before renaming it into place. Both contain document text. That is why a document occupies four times its size on disk, and briefly up to six times while a save is being written; the first save says so once.
- **Logs:** `logs/jtypewriter-0.log` to `jtypewriter-2.log` - three rotating generations of up to one megabyte each. They hold file paths, error messages and full stack traces.
- **Deleting all of it:** quit the application first, otherwise the log handler and `instance.lock` are recreated, then delete `~/.jtypewriter` and any `<document>.bak*` or leftover `jtw-*.tmp` next to your documents. There is no in-app "delete all data" command, and uninstalling the application does not remove this data - removing an installed app image or package leaves `~/.jtypewriter` in place.

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
| `Ctrl+F` | Find and replace card |
| `F3` | Find next (wraps) |
| `Shift+F3` | Find previous (wraps) |
| `Ctrl+H` | Find and replace card, replace field focused |
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

Download the installer (`.deb` on Linux, `.msi` on Windows) or the portable
`.tar.gz` from
[the GitHub Releases page](https://github.com/hyperpostulate/JTypeWriter/releases),
together with the `SHA256SUMS` file that lets you
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
./scripts/package.sh --installer     # Same, plus the platform installer (.deb / .msi)
./scripts/verify-artifacts.sh        # Fresh build, then compare every shipped copy byte for byte
./scripts/smoke-appimage.sh --skip-build  # Start the built image; install, launch and remove the .deb
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
platform installer (`.deb`, `.msi`) for Linux and Windows - the same
command the CI workflow runs. Every release publishes both channels for each
platform, the installer and a portable `.tar.gz`; which one becomes the product
default is still an open decision, so `--installer` is one supported path, not
the decided one. The application version has a single source of truth:
`pom.xml`. The script reads it back from the built artifact and validates it
against jpackage's rules.

`scripts/verify-artifacts.sh` closes the other half of that loop: it compares the
checked-in payload (`LICENSE`, `NOTICE`, `THIRD-PARTY-NOTICES`, `legal/`, the fat
JAR and the jar's own `META-INF` entries) against what actually ships in the
native image and in the installer, byte for byte, in one command. The `.deb`
payload is unpacked with `dpkg-deb` and compared the same way, because
`jpackage`'s deb bundler drops the image-root files and `scripts/package.sh`
completes the payload afterwards.

`scripts/smoke-appimage.sh` proves what the file comparison cannot: that the
artifacts actually run. On Linux it starts the application image under a
throwaway `user.home` (`Xvfb` when no display is available), waits for the main
window with `xwininfo` instead of sleeping, then installs the `.deb`, starts the
installed launcher and purges the package again; on Windows it starts the image
and requires the jpackage launcher to stay alive. The CI `smoke` job runs it on
both legs against the artifact the build job produced, so "packaged" is no
longer shipped untested.

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

---

## CI/CD

GitHub Actions workflows: build and test on every push and pull request to `main` (`.github/workflows/maven.yml`), release on a version tag (`.github/workflows/release.yml`).

- **Trigger**: Runs on every push and pull request to `main`
- **Environment**: Ubuntu and Windows (build matrix)
- **JDK**: Temurin 25
- **Command**: `mvn -B package` (build + tests), then `scripts/package.sh --skip-build --installer`
- **Gates**: `scripts/generate-icons.py --check` runs before the build on both OS legs: it compares every icon's hash with `icons.manifest`, so a committed asset that no longer matches the canonical artwork fails in a minute rather than shipping. It verifies hashes rather than re-rasterising on purpose - ImageMagick encodes the same pixels to different bytes on different builds, so a byte or pixel comparison in CI would compare toolchains, not artwork. `scripts/verify-artifacts.sh --skip-build` runs after packaging (Linux leg, because it unpacks the `.deb` with `dpkg-deb`) and re-proves the byte-for-byte comparison between the repository payload, the application image and the installer payload. A separate `smoke` job then consumes the packaged artifact and runs `scripts/smoke-appimage.sh --skip-build` on both OS legs: Linux starts the image under Xvfb and installs, launches and purges the `.deb`; Windows requires the launcher to stay alive
- **Locales**: the test suite also runs under Turkish and German system locales, so i18n regressions (e.g. a language that silently resolves to the system language) cannot slip through
- **Artifacts**: native application image plus a platform installer (`.deb` / `.msi`) per OS
- **Releases**: pushing a `v*` tag that matches the `pom.xml` version builds both platforms and publishes them to a GitHub Release together with a `SHA256SUMS` file, so the binaries no longer live in Actions artifacts that expire after a few days. The same workflow can be started by hand as a **dry run**: `prepare`, both OS builds, the asset/source/checksum assembly, the five-asset assertion and the release-note rendering all run, but publication is replaced by an artifact holding exactly the assembled set. A manual run cannot publish - a release from a branch tip could not be reproduced from its tag - so `dry_run` defaults to true and asking for false is refused in `prepare`. A published release is marked **pre-release** (`gh release create --prerelease`) while the project says so in its own note: the first releases are early access and can change between versions, and the mark is removed deliberately in a later card rather than by omission

---

## Architecture

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
│   ├── MainMenu.java                     # File/Edit/View/Help menu button generated from EditorAction
│   ├── MainStage.java                    # Stage configuration (fullscreen, close)
│   ├── AppIcons.java                     # Window/taskbar/dock icon set from /icons/
│   ├── AppFonts.java                     # Bundled UI font + headless measurement face
│   ├── WelcomeCard.java                  # Welcome card: shortcuts + greeting opt-out
│   ├── RecentFilesCard.java              # Recent documents card (opened from the File menu)
│   ├── AboutCard.java                    # About card with author info
│   ├── KeyboardShortcutHandler.java      # Centralized keyboard shortcut handling
│   ├── CrashHandler.java                 # Global crash net: log + error dialog
│   ├── DropHandler.java                  # Scene-level file drop glue (resolve, open, report)
│   ├── DroppedFiles.java                 # Drop decision: one readable .txt/.md (pure)
│   ├── FindModel.java                    # Find/replace scan, cursor and replace decisions (pure)
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
├── AppVersion.java                       # App version, filtered from pom.xml at build time
├── AtomicFiles.java                      # Durable atomic writes (fsync + rename)
├── InstanceLock.java                     # Per-user instance lock (single instance)
├── RecentFiles.java                      # Welcome card's recent-document list (pure)
├── StartupArguments.java                 # Resolves the CLI/file-association argument (pure)
└── FxApplication.java                    # JavaFX application lifecycle bridge
```

### Core Components

| Component | Description |
|-----------|-------------|
| `EditorArea` | TextArea subclass that routes every edit through the YOLO policy; loads documents through `setDocument` |
| `UndoHistoryReset` | Clears the TextArea undo/redo history on every document load (reflective, JavaFX has no API) |
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
| `DroppedFilesTest` | The drop decision: `null`/empty file list, one file, `.md`, an upper-case extension, several files (refused with the count), a PDF and an extension-less file, a path that vanished before the drop, a directory named `*.txt`, a symlink that keeps its name, and the suffix check rejecting names that only end with the same letters |
| `FindModelTest` | The find/replace model: literal query and replacement, non-overlapping scan, case sensitivity under a Turkish default, wrapping in both directions, a search restarting before the first match, a background scan that is stale or oversize being dropped, replace-all not rescanning its own output, replace-one selecting the next match past the inserted text, and a range past the end being refused |
| `DropContractTest` | Source-level contract for the drop: `MainStage` installs it, it filters `DRAG_OVER`/`DRAG_DROPPED` on the whole scene, it opens only through `FileService.openPathAsync` (no second save prompt, no direct save), it hands the loaded text to the editor, and no source outside `FileService` asks the unsaved-changes question |
| `StartupArgumentsTest` | The `JTypeWriter <file>` decision: no/blank argument, an existing regular file, a relative path, a missing path, a directory, two arguments, a symlink that keeps its name, and text the platform cannot parse |
| `AutosaveHealthTest` | Sticky failure state (`DRAFT_WRITE` / `DRAFT_PARK` / `DRAFT_DELETE`) that survives repeated failures, clears on a successful write and notifies once per real transition |
| `ProcessCrashRecoveryTest` | Linux-only end-to-end kill test: `CrashHarness` (a helper, not a test class) builds the real service graph in a child JVM without starting JavaFX and snapshots a dirty document through the production autosave path; the parent then kills it with `SIGKILL` (`Process.destroyForcibly`) and a second JVM must recover the exact non-ASCII text. A paired case proves an answered close still drops the draft, and `-Duser.home` keeps logging inside a temp home. Synchronisation is the stdout marker plus `waitFor`, never `Thread.sleep` |
| `YoloPolicyTest` | YOLO mode edit policy (deletions blocked, insertions allowed, safe replacement) |
| `FindReplacePolicyTest` | When a replacement needs the user's consent: YOLO on requires the prompt, YOLO off asks nothing |
| `FindScanServiceTest` | The scan runs off the FX thread and its result is delivered only through the injected FX dispatcher (`fxThreadScans()` stays zero) |
| `KeyboardShortcutHandlerTest` | Shortcut decoding incl. shift-sensitive combos (Ctrl+Shift+Y / Z / C / T / S), Ctrl+N for a new document, the find chords (Ctrl+F / F3 / Shift+F3 / Ctrl+H, where F3 and Shift+F3 must stay distinct) and numpad equivalents |
| `MainControllerTest` | Window title composition (application + document name, unsaved `*` marker, `[YOLO]` badge), the Edit menu's find/replace placement, and the command-menu contract: `MainMenu.layout()` carries every platform-independent `EditorAction` exactly once across the four pages (six settings commands are represented by their value submenu instead), the View submenus list and check the same values the toolbar dropdowns offer, and every page title and row label is non-empty in all six languages |
| `MainStageTest` | Off-screen window position guard (`intersectsAny`: restored positions on detached monitors) |
| `AppIconsTest` | The icon set on the classpath: every PNG decodes to the declared size, and the `.ico` file starts with its expected magic bytes (pure byte checks, no toolkit) |
| `AppSettingsTest` | Preference persistence across restarts, restored toolbar/fullscreen/status-bar-size state and their corrupt-value fallbacks, defaults, tolerance for unknown values and unusable paths, quarantine of a corrupt settings file instead of silently overwriting it, the settings schema version, the sound level (including the legacy on/off key read as a level), and the recent-document list plus the welcome opt-out |
| `StartupGreetingTest` | When the welcome card may arrive on its own: a fresh install is greeted, a stored opt-out keeps it away, and a document named on the command line is never covered by it |
| `RecentFilesCardTest` | How a recent-document row is named: the file name, falling back to the whole path when a path has no last element |
| `RecentFilesTest` | The recent-files card's document list as pure list operations: newest first, one row per document by absolute normalised path, the ten-entry cap dropping the oldest entry, vanished documents filtered out, and empty/null handled |
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
| `ToolbarStateTest` | The sound level's single setter: persistence across a restart, the audio engine follows, zero silences both, On restores the last audible rung, repeats are no-ops |
| `ToolbarFitTest` | Toolbar overflow policy: how the captioned row is measured (buttons, dropdowns, spacer, chrome) with injected metrics, the FULL→COMPACT switch and its 40 px hysteresis, plus acceptance tests proving German and French are not clipped at 1280 px and that the collapsed row fits 900 px in all six languages, and that the window's minimum width is the widest collapsed row measured across the six bundles |
| `AppFontsTest` | The bundled UI font is a release asset: both faces are on the classpath under the family `base.css` names, they render every toolbar label in all six languages (including the `☰` menu glyph), and a pinned string measures to a pinned width, so a fallback to a system font fails the build |
| `PackagingNoticesTest` | The packaging contract: `META-INF/THIRD-PARTY-NOTICES` and all three license texts are on the classpath, the notice names every bundled component with its real SPDX id, states that FFmpeg is not distributed, stays LF-only within 100 columns, and is byte-identical to the checked-in file so a stale build output cannot pass |
| `ContrastRatioTest` | WCAG contrast maths (black-on-white 21:1, identical colours 1:1, the 4.5 boundary) and CSS-parsing guards that fail the build if status text, the autosave warning, the toolbar text, a plain popup row or the default editor text drops below AA or 13 px (or a theme declares a token `base.css` never reads), the focus ring drops below 3:1, highlighted popup-row text drops below AA, the popup cell font size drifts from the closed combo's, the focus colour becomes transparent again, or a non-existent JavaFX CSS property (`-fx-letter-spacing`, `-fx-transition`) returns |
| `CiGatesTest` | The CI gate contract: the workflow really invokes the icon freshness check and the artifact verification, the icon check runs before the build and on both OS legs without re-rasterising, the artifact check runs after packaging, and that artifact gate stays on the Linux leg (it unpacks the `.deb`) |
| `EditorActionTest` | The command inventory: every constant performs exactly its own effect through a recording `Context` (no toolkit), every constant carries a label and at least one chord, only `EXIT_FULLSCREEN` is left to the platform, the toolbar buttons run the shared actions, and a source scan fails if a chord literal is redeclared outside `EditorAction` |
| `ShortcutRowsTest` | The shortcut card is generated from the inventory instead of being copied: two-way coverage with `EditorAction`, the published row order, a localised label and description per row, and no chord twice on the card, in all six languages |
| `StatsPerfHarnessTest` | The ceiling behind the incremental counters: one real `StatsBarModel.refresh` (formatting included) on a 10 MiB document stays under 50 ms in CI. The whole-text version measured ~164 ms once a second, so an O(n) refresh coming back fails the build |
| `FxThreadIoTest` | The FX-thread invariant without starting a toolkit: opening, saving and autosaving reach the disk through `IoExecutor` only (`fxThreadTasks()` stays zero), a clean autosave tick never copies the document text, and text typed while a save is in flight stays dirty afterwards |
| `RecoveryTargetPathTest` | The guard between a recovery draft's header and the next save: the path must be absolute, canonical, under an allowed root and outside the app's own data directory. A relative path, a traversal escape, a dangling symlink or a path without allowed roots is dropped (fail closed) while the recovered text is still offered |

---

## License

JTypeWriter is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for the full text and [NOTICE](NOTICE) for attribution notices. [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) lists every bundled component with its license id, copyright line and source, and the full texts sit in [legal/](legal); both ship inside the fat JAR as `META-INF/THIRD-PARTY-NOTICES` and `META-INF/licenses/`, and at the root of the native image. The bundled JavaFX libraries stay under GPLv2 with the Classpath Exception and GStreamer Lite under LGPL-2.1-or-later; the OpenJDK runtime carries its own notices under `lib/runtime/legal/`. The bundled keystroke sound (`src/main/resources/audio/keystroke.wav`) is the author's own work, contains no third-party sample, and is covered by the same Apache-2.0 licence; its provenance and SHA-256 are recorded in [src/main/resources/audio/PROVENANCE.md](src/main/resources/audio/PROVENANCE.md). The UI chrome font is DejaVu Sans Condensed (regular and bold), unmodified, under the Bitstream Vera licence; its provenance and file hashes are in [src/main/resources/fonts/PROVENANCE.md](src/main/resources/fonts/PROVENANCE.md).

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
