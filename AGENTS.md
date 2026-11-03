# JTypeWriter — agent notes

JavaFX 26 / Java 25 desktop text editor, single Maven module. Import root is
`org.mesutormanli.jtypewriter`.

**Supported platforms: Linux x64 and Windows x64 only.** macOS was removed from the product
(packaging, icons, CI matrix, tests and docs). Do not reintroduce `Darwin*` branches, `.dmg`,
`.icns`, `Contents/Resources` bundle paths or a `macos-latest` runner without an explicit
product decision — `package.sh`, `release-assets.sh` and `verify-artifacts.sh` reject unknown
platforms on purpose.

## Commands

- JDK 25 + Maven 3.8+ are hard requirements (`maven-enforcer-plugin` at `validate`, floor
  derived from `java.version`). There is deliberately no Maven wrapper — don't add one.
- `mvn clean test` — full suite (~365 tests, no toolkit, no display needed).
- `mvn test -Dtest=FileServiceTest` / `-Dtest=FileServiceTest#methodName` — focused run.
- `mvn clean package` — tests + thin JAR + `jar-with-dependencies`. The fat JAR bundles
  JavaFX natives from the build machine, so it runs only on the OS it was built on.
- `mvn clean javafx:run` — development launch.
- `./scripts/package.sh [--skip-build] [--installer]` — `jpackage` app image/installer into
  `releases/<os>/`; host-platform only, needs jpackage (+ fakeroot for `.deb` on Linux).
- `./scripts/verify-artifacts.sh [--skip-build]` — fresh build then a byte-for-byte comparison
  of repo payload ↔ application image ↔ `.deb` payload (jars, legal texts, jar entries).
- `./scripts/smoke-appimage.sh [--skip-build] [--skip-deb]` — starts the packaged app (Xvfb +
  `xwininfo` on Linux, "launcher stays alive" on Windows) and, on Linux with root or
  passwordless sudo, installs, launches and purges the `.deb`. Skips the install half with a
  note when neither is available.
- CI runs `mvn -B package` then `scripts/package.sh --skip-build --installer`, then re-runs
  the suite under `tr-TR` and `de-DE`. `generate-icons.py --check` runs before the build on
  both legs and `verify-artifacts.sh --skip-build` after packaging on the Linux leg; those two
  are pinned by `CiGatesTest`, so moving or dropping a gate fails the suite. A separate `smoke`
  job downloads the uploaded artifact and runs `smoke-appimage.sh --skip-build` on both legs —
  it is pinned there too, including its `needs: build` dependency and `--skip-build`.

Run Maven from the repo root: several tests read `src/`, `docs/` and `.github/` through
relative paths and fail if Surefire's CWD is elsewhere.

There is no lint, formatter or typecheck plugin (no Checkstyle/Spotless, by design). The test
suite is the gate, and it enforces several non-code invariants below.

## Git

- **The agent commits; the user pushes.** Finish a unit of work with a local commit and stop
  there — never `git push`, never add or change a remote, and don't ask whether to push: the
  push is the user's call and happens on their schedule, not yours.
- One roadmap card or one self-contained change per commit, with a body that explains *why*
  and a `Refs: <card> · Kanıt: ...` evidence line where a roadmap card exists.
- Leave the tree clean after committing; `target/`, `releases/` and `scripts/__pycache__/` are
  generated and never belong in a commit.

## Invariants enforced by tests (not by plugins)

- Every `.java` file under `src/` begins with the SPDX block at byte 0, before `package`:
  `/*`, ` * SPDX-License-Identifier: Apache-2.0`, ` * Copyright (c) 2026 Mesut ORMANLI`, ` */`
  (`LicenseHeaderTest`). The canonical icon SVG must declare the same.
- i18n: all six bundles must have the **identical key set**, and every key must be both
  present in the bundles and referenced from `Messages.java` — missing *and* dead keys fail
  (`MessagesTest`). Adding a string means six `messages*.properties` edits plus a `Messages`
  accessor.
- `THIRD-PARTY-NOTICES` must name every bundled component with its real SPDX id, stay LF-only
  within 100 columns, contain no zero-width spaces, and be byte-identical to the copy packed
  into `target/classes/META-INF`. After editing it, re-run tests (a stale build output fails).
- `docs/release-notes-template.md` must stay bilingual (English section before Türkçe), use
  `@VERSION@`/`@TAG@`, contain no literal semver, and repeat the same verification commands in
  both halves. `release.yml` fails the release if either half is gone.
- Every `actions/upload-artifact` step in the workflows needs `retention-days` (max 7).
- No test may start a JavaFX toolkit. Keep new UI logic testable without one.

## Architecture

- Manual dependency injection, no framework. `AppGraph` is the composition root: public final
  fields where **declaration order is dependency order**; cross-component listeners are wired
  in its constructor. Don't reorder or construct these components elsewhere.
- `JTypeWriterApp` is deliberately *not* an `Application` subclass (keeps the JDK launcher
  happy with JavaFX on the classpath). `FxApplication` bridges the JavaFX lifecycle.
- `fxml/main.fxml` is only an empty `BorderPane`; every widget is built in Java. `MainController`
  is supplied through a `controllerFactory`.
- Strong house convention: pull decisions, geometry, parsing and state out of JavaFX classes
  into pure/headless static methods or model objects (`YoloPolicy`, `StatsBarModel`,
  `ToolbarFit`, `ExitDisposition`, `AutosaveHealth`, `DialogSizing`, `ShortcutRows`,
  `TextCodec`), then unit-test those. Follow it instead of testing nodes.
- `service/` is JavaFX-free; user-facing `FileChooser`/`Alert` work is isolated in
  `DialogService`, which tests mock. `FileService` also owns unsaved-change flow.
- Concurrency/state where it matters: `FileService` listeners live in a `CopyOnWriteArrayList`;
  enum-based state models (`ExitDisposition`, `AutosaveHealth.Problem`) encode exit/autosave
  rules. Read a class Javadoc before changing its policy — data-safety semantics (atomic
  saves, rotating backups, encoding-stable round trips, no lossy `?` replacement) are deliberate.
- Javadoc style is dense and rationale-driven (HTML `<p>`/`<ul>`, `{@link}`, "why not X"), no
  `@author`. Match it rather than adding one-line comments.

## Localization

- Locales: EN (base), TR, DE, FR, IT, ES. Bundle lookup deliberately disables system-locale
  fallback and uses `Locale.ROOT` (also avoids the Turkish dotless-i trap).
- Never rely on the default locale or locale-dependent formatting in app code or tests; tests
  save/restore `Locale.setDefault` and CI proves it under TR and DE system locales.
- Native language names are Java constants in `Language`, not bundle keys.

## Testing conventions

- JUnit Jupiter 6, package-private test classes/methods, no `@DisplayName`, no `@Nested` or
  `@ParameterizedTest`. Names are long lowerCamelCase sentences describing the invariant.
- `@TempDir`, `Assumptions` for POSIX/root-only cases, Mockito only for isolating
  `Messages`/`DialogService`. Tests must never touch the real `~/.jtypewriter`; pass explicit
  paths or `-Duser.home=<temp>`.
- `ProcessCrashRecoveryTest` is Linux-only: it spawns child JVMs via `CrashHarness` and
  `destroyForcibly()`. Run it alone when diagnosing; it is the slowest test.

## Housekeeping

- Version has one source: `pom.xml`. Release tag must be `v<version>`; `package.sh` rejects
  versions jpackage cannot use (`0.x`, `-SNAPSHOT`).
- Icons have one source: `src/main/resources/icons/jtypewriter.svg`.
  `scripts/generate-icons.py` regenerates the PNG/ICO set plus `icons.manifest` (needs
  ImageMagick 7 `magick` or 6 `convert`). `--check` compares hashes against that manifest and
  needs no ImageMagick — it never re-rasterises, because the bytes a renderer produces are not
  portable across builds. `--render` is the machine-local pixel comparison; run it after
  regenerating. A hand-edited asset, an edited SVG or an extra PNG in that directory fails
  `--check`.
- `releases/` and `target/` are generated — never hand-edit or commit them.
- Docs are bilingual: update `README.md` and `README-TR.md` (and release notes) together.
- `.gitattributes` forces LF for text (CRLF only for `.bat/.cmd/.ps1`); keep new text files LF.
