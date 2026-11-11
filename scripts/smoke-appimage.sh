#!/usr/bin/env bash
#
# Proves that the packaged application actually starts - the gap between
# "package.sh succeeded" and "a user can run it":
#
#   ./scripts/smoke-appimage.sh                # fresh build + image + installer, then smoke
#   ./scripts/smoke-appimage.sh --skip-build   # smoke what is already built
#   ./scripts/smoke-appimage.sh --skip-build --skip-deb
#   ./scripts/smoke-appimage.sh --skip-build --open-sample
#
# Linux launches the application image under an empty HOME inside Xvfb (or an
# existing display), waits for the main window with xwininfo, stops the process,
# then installs the .deb, launches the installed launcher, stops it, purges the
# package and asserts that dpkg no longer knows it. Windows launches the image
# and requires the jpackage launcher to stay alive: a crashed JVM exits, and a
# process that cannot start at all is exactly what this test must catch.
#
# --open-sample adds one more launch that names a real file on the command
# line. A file association and a typed command both reach the application as
# the same process argument, so this is the only place where the whole chain -
# launcher, argument passing, path resolution, document load, window title -
# is proven against the packaged artifact rather than the sources.
#
# Why this is not part of package.sh or verify-artifacts.sh: those prove the
# *files*. They cannot prove that the bundled runtime finds its libraries, that
# the launcher resolves its app directory, or that the .deb's payload is
# complete enough to start. Those failures only appear when the artifact is
# executed, which is what a user does first.
#
# The smoke runs on its own user.home so the developer's real ~/.jtypewriter
# (settings, drafts, logs, the single-instance lock) is never touched. The
# launch is wrapped in a timeout instead of a fixed sleep: on a slow runner the
# window appears late, and on a broken build it never appears - both are
# answered by polling the process and the window list rather than guessing.
#
# Environment overrides (used by CI and for local debugging):
#   SMOKE_WINDOW_TIMEOUT   seconds to wait for the window (default 60)
#   SMOKE_ALIVE_SECONDS    seconds the Windows launcher must survive (default 10)
#
set -euo pipefail

cd "$(dirname "$0")/.."

APP_NAME="JTypeWriter"
WINDOW_TIMEOUT="${SMOKE_WINDOW_TIMEOUT:-60}"
ALIVE_SECONDS="${SMOKE_ALIVE_SECONDS:-10}"

BUILD=1
SKIP_DEB=0
OPEN_SAMPLE=0
for argument in "$@"; do
    case "$argument" in
        --skip-build)   BUILD=0 ;;
        --skip-deb)     SKIP_DEB=1 ;;
        --open-sample)  OPEN_SAMPLE=1 ;;
        *) echo "unknown option: $argument (expected --skip-build, --skip-deb and/or --open-sample)" >&2; exit 1 ;;
    esac
done

case "$(uname -s)" in
    Linux*)               PLATFORM="linux" ;;
    MINGW*|MSYS*|CYGWIN*) PLATFORM="windows" ;;
    *) echo "unsupported platform: $(uname -s) (supported: Linux, Windows)" >&2; exit 1 ;;
esac

note() { echo "smoke: $*"; }
fail() { echo "smoke: FAIL: $*" >&2; exit 1; }

# P2 of the P3.2 wave: JavaFX must be resolved from named modules. The old
# layout put its classes on the classpath, so every launch logged
# "Unsupported JavaFX configuration: classes were loaded from 'unnamed module'"
# - and the application still started, which is why presence-of-window is not
# evidence. The launcher log is the evidence, and it is checked on every launch
# this script performs rather than on one representative run.
assert_no_unnamed_module() {
    local log="$1" label="$2"
    if grep -q 'unnamed module' "$log" 2>/dev/null; then
        echo "---- $label launcher log ----" >&2
        cat "$log" >&2 || true
        fail "$label logged 'unnamed module': JavaFX is still loaded from the classpath"
    fi
}

# One scratch tree for every home directory and log file this script creates.
WORK="$(mktemp -d)"
APP_PID=""
XVFB_PID=""

cleanup() {
    if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
        kill "$APP_PID" 2>/dev/null || true
    fi
    if [[ -n "$XVFB_PID" ]]; then
        kill "$XVFB_PID" 2>/dev/null || true
    fi
    rm -rf "$WORK"
}
trap cleanup EXIT

if [[ "$BUILD" == "1" ]]; then
    mvn -B clean package
    if [[ "$PLATFORM" == "linux" ]]; then
        bash scripts/package.sh --skip-build --installer
    else
        # Windows smoke tests the application image only; the .msi is not
        # installed by this script (an unattended MSI install cannot be undone
        # as reliably as dpkg, and the installer payload is compared by
        # verify-artifacts.sh on the Linux leg).
        bash scripts/package.sh --skip-build
    fi
fi

# --- display ------------------------------------------------------------
# CI has no X server; xvfb-run is avoided on purpose because the script needs
# the child process handle, not an outer wrapper's. An existing DISPLAY is
# reused so a developer on a desktop gets the same run as CI.
ensure_display() {
    if xwininfo -root -tree >/dev/null 2>&1; then
        return
    fi
    command -v Xvfb >/dev/null 2>&1 \
        || fail "no usable DISPLAY and Xvfb is not installed (apt-get install xvfb x11-utils)"
    local display
    for number in $(seq 99 109); do
        if [[ ! -e "/tmp/.X11-unix/X$number" ]]; then
            display=":$number"
            break
        fi
    done
    [[ -n "${display:-}" ]] || fail "could not find a free X display between :99 and :109"
    Xvfb "$display" -screen 0 1280x800x24 >"$WORK/xvfb.log" 2>&1 &
    XVFB_PID=$!
    export DISPLAY="$display"
    local deadline=$((SECONDS + 10))
    while (( SECONDS < deadline )); do
        if xwininfo -root -tree >/dev/null 2>&1; then
            note "started Xvfb on $display"
            return
        fi
        sleep 0.2
    done
    fail "Xvfb on $display did not become reachable (see $WORK/xvfb.log)"
}

# --- process helpers ----------------------------------------------------
# Waits for a titled window and treats an early exit as a failure with the
# launcher's own log, so the CI result is diagnosable without a rerun. The
# pattern is matched literally: a document-title check must not let `.` in
# "sample.txt" match any other character.
wait_for_window() {
    local pid="$1" label="$2" log="$3"
    local pattern="${4:-"\"$APP_NAME - "}"
    local deadline=$((SECONDS + WINDOW_TIMEOUT))
    local started=$SECONDS
    local tree
    while (( SECONDS < deadline )); do
        if ! kill -0 "$pid" 2>/dev/null; then
            local code=0
            wait "$pid" || code=$?
            echo "---- $label launcher log ----" >&2
            cat "$log" >&2 || true
            fail "$label launcher exited with code $code before showing a window"
        fi
        tree=$(xwininfo -root -tree 2>/dev/null || true)
        # The main stage title is composed as "<app name><separator><document>"
        # (see MainController.titleFor). The opening quote plus the separator is
        # what makes this a real assertion: matching just "JTypeWriter" also
        # matches a browser tab or an editor pane whose path contains the
        # project name, so a reused developer display would "find" a window
        # before the app even started. The welcome dialog - "Welcome -
        # JTypeWriter" - deliberately does not match; the main stage is the
        # window a user gets.
        if grep -qF -- "$pattern" <<<"$tree"; then
            note "$label: main window is up after $((SECONDS - started)) s"
            return
        fi
        sleep 0.5
    done
    echo "---- window tree ----" >&2
    xwininfo -root -tree >&2 || true
    echo "---- $label launcher log ----" >&2
    cat "$log" >&2 || true
    fail "$label did not show a window within $WINDOW_TIMEOUT s"
}

stop_process() {
    local pid="$1"
    kill "$pid" 2>/dev/null || true
    local attempt
    for attempt in $(seq 1 40); do
        if ! kill -0 "$pid" 2>/dev/null; then
            wait "$pid" 2>/dev/null || true
            return
        fi
        sleep 0.25
    done
    kill -9 "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
}

# --- Linux --------------------------------------------------------------
smoke_linux_image() {
    local image="releases/linux/$APP_NAME"
    [[ -x "$image/bin/$APP_NAME" ]] \
        || fail "no application image launcher at $image/bin/$APP_NAME - run scripts/package.sh first"

    ensure_display
    local home="$WORK/image-home"
    mkdir -p "$home"
    local log="$WORK/image.log"

    note "launching the application image"
    # The bundled JVM reads user.home from the passwd entry, not $HOME, so an
    # empty sandbox is injected through JAVA_TOOL_OPTIONS - the only hook the
    # jpackage launcher honours without rewriting its .cfg.
    JAVA_TOOL_OPTIONS="-Duser.home=$home" "$image/bin/$APP_NAME" >"$log" 2>&1 &
    APP_PID=$!
    wait_for_window "$APP_PID" "image" "$log"
    stop_process "$APP_PID"
    APP_PID=""
    assert_no_unnamed_module "$log" "image"
    note "image started and stopped cleanly"
}

smoke_linux_open_sample() {
    local image="releases/linux/$APP_NAME"
    [[ -x "$image/bin/$APP_NAME" ]] \
        || fail "no application image launcher at $image/bin/$APP_NAME - run scripts/package.sh first"

    ensure_display
    local home="$WORK/sample-home"
    mkdir -p "$home"
    local sample="$WORK/cli-sample.txt"
    echo "This document was named on the command line." > "$sample"
    local log="$WORK/sample.log"

    note "launching the application image with a document argument"
    JAVA_TOOL_OPTIONS="-Duser.home=$home" "$image/bin/$APP_NAME" "$sample" >"$log" 2>&1 &
    APP_PID=$!
    # The title names the document, so a run that ignored the argument (and
    # showed "Untitled" or the welcome card) fails instead of passing on any
    # window at all.
    wait_for_window "$APP_PID" "sample document" "$log" \
        "\"$APP_NAME - $(basename "$sample")\""
    stop_process "$APP_PID"
    APP_PID=""
    assert_no_unnamed_module "$log" "sample document"
    note "the sample document opened from the process argument"
}

smoke_linux_deb() {
    local deb=""
    shopt -s nullglob
    local candidates=(releases/linux/*.deb)
    shopt -u nullglob
    if [[ ${#candidates[@]} -ne 1 ]]; then
        fail "expected exactly one .deb under releases/linux/, found ${#candidates[@]} - run scripts/package.sh --installer"
    fi
    deb="${candidates[0]}"

    local SUDO=""
    if [[ "$(id -u)" != "0" ]]; then
        if sudo -n true 2>/dev/null; then
            SUDO="sudo"
        else
            note "no root and no passwordless sudo - checking the .deb payload instead of installing it"
            smoke_linux_deb_payload "$deb"
            return
        fi
    fi

    note "installing $deb"
    local install_log="$WORK/dpkg-install.log"
    if ! $SUDO dpkg -i "$deb" >"$install_log" 2>&1; then
        # A failed install leaves dpkg's database holding a half-configured
        # package, and every later `dpkg -i` then refuses to proceed. Undo that
        # before reporting, so one red run does not poison the machine (or a
        # reused CI runner) for the next one.
        $SUDO dpkg --purge --force-all jtypewriter >/dev/null 2>&1 || true
        echo "---- dpkg -i output ----" >&2
        cat "$install_log" >&2
        fail "installing $deb failed"
    fi
    local status
    status=$(dpkg-query -W -f='${Status}' jtypewriter 2>/dev/null || true)
    [[ "$status" == "install ok installed" ]] \
        || fail "the .deb install reported success but dpkg has no installed jtypewriter ($status)"

    local home="$WORK/deb-home"
    mkdir -p "$home"
    local log="$WORK/deb.log"
    note "launching the installed application"
    JAVA_TOOL_OPTIONS="-Duser.home=$home" /opt/jtypewriter/bin/"$APP_NAME" >"$log" 2>&1 &
    APP_PID=$!
    wait_for_window "$APP_PID" "installed package" "$log"
    stop_process "$APP_PID"
    APP_PID=""
    assert_no_unnamed_module "$log" "installed package"
    note "installed application started and stopped cleanly"

    note "purging the package"
    local purge_log="$WORK/dpkg-purge.log"
    if ! $SUDO dpkg --purge jtypewriter >"$purge_log" 2>&1; then
        echo "---- dpkg --purge output ----" >&2
        cat "$purge_log" >&2
        fail "purging jtypewriter failed"
    fi
    if dpkg -l 2>/dev/null | grep -q 'jtypewriter'; then
        fail "jtypewriter still appears in 'dpkg -l' after purge"
    fi
    note "package purged; dpkg no longer knows jtypewriter"
}

# The .deb branch that needs no root: unpack the payload and launch its launcher.
#
# This is not a weaker version of the install check, it is the part that matters.
# A jpackage launcher behaves differently once its binary belongs to a package:
# it stops deriving its directories from its own path and asks dpkg instead -
# `dpkg -S <launcher>` for the owning package, `dpkg -L <package>` for the file
# list - and the app and runtime directories are then whatever those lines end
# in. Two things went wrong in CI behind exactly that switch, and neither was
# visible to the app-image launch:
#
#   * the JavaFX option was --module-path=$ROOTDIR/lib/javafx, and $ROOTDIR is
#     expanded from imageRoot, which the installed branch never sets. The option
#     became /lib/javafx and the launcher died with "Module javafx.controls not
#     found" while the image next to it started fine;
#   * libapplauncher.so is located the same way, so a payload missing it fails
#     before the JVM even starts.
#
# Standing in for dpkg with the unpacked tree answers both questions with the
# paths an install would produce, so the packaged branch runs here too - on a
# developer's machine, not only on a CI runner that happens to have root.
smoke_linux_deb_payload() {
    local deb="$1"
    local payload="$WORK/deb-payload"
    mkdir -p "$payload"
    if ! dpkg-deb -x "$deb" "$payload" >"$WORK/dpkg-deb.log" 2>&1; then
        echo "---- dpkg-deb -x output ----" >&2
        cat "$WORK/dpkg-deb.log" >&2
        fail "could not unpack $deb"
    fi
    local root="$payload/opt/jtypewriter"
    [[ -x "$root/bin/$APP_NAME" ]] \
        || fail "the .deb payload has no launcher at opt/jtypewriter/bin/$APP_NAME"
    [[ -f "$root/lib/libapplauncher.so" ]] \
        || fail "the .deb payload has no lib/libapplauncher.so - the launcher finds its own library through dpkg, so it would exit before starting the JVM"

    local stub="$WORK/dpkg-stub"
    mkdir -p "$stub"
    cat >"$stub/dpkg" <<EOF
#!/bin/sh
# Answers only the two queries a jpackage launcher makes about its owning
# package, pointing them at the unpacked payload instead of a real install.
case "\$1" in
    -S) echo "jtypewriter: \$2" ;;
    -L) printf '%s\n' "$root" "$root/bin" "$root/lib" "$root/lib/app" \\
            "$root/lib/runtime" "$root/lib/libapplauncher.so" ;;
esac
EOF
    chmod +x "$stub/dpkg"

    local home="$WORK/deb-payload-home" log="$WORK/deb-payload.log"
    mkdir -p "$home"
    note "launching the unpacked .deb payload's launcher"
    PATH="$stub:$PATH" JAVA_TOOL_OPTIONS="-Duser.home=$home" "$root/bin/$APP_NAME" >"$log" 2>&1 &
    APP_PID=$!
    wait_for_window "$APP_PID" ".deb payload" "$log"
    stop_process "$APP_PID"
    APP_PID=""
    assert_no_unnamed_module "$log" ".deb payload"
    note "the .deb payload's launcher started and stopped cleanly (not installed; run as root for the install check)"
}

# --- Windows ------------------------------------------------------------
smoke_windows_image() {
    local image="releases/windows/$APP_NAME"
    [[ -x "$image/$APP_NAME.exe" ]] \
        || fail "no application image launcher at $image/$APP_NAME.exe - run scripts/package.sh first"

    local home="$WORK/image-home"
    mkdir -p "$home"
    local log="$WORK/image.log"

    # The MSYS shell's /tmp is not the native Windows path the JVM sees, so a
    # unix-style -Duser.home would resolve against the current drive.
    local home_native="$home"
    if command -v cygpath >/dev/null 2>&1; then
        home_native=$(cygpath -w "$home")
    fi

    note "launching the application image"
    JAVA_TOOL_OPTIONS="-Duser.home=$home_native" "$image/$APP_NAME.exe" >"$log" 2>&1 &
    APP_PID=$!

    # A launcher that dies early is the failure this seam exists to catch; a
    # window query is deliberately not used because the Windows runners have no
    # reliable interactive desktop.
    local attempt
    for attempt in $(seq 1 $((ALIVE_SECONDS * 2))); do
        if ! kill -0 "$APP_PID" 2>/dev/null; then
            local code=0
            wait "$APP_PID" || code=$?
            echo "---- image launcher log ----" >&2
            cat "$log" >&2 || true
            fail "the launcher exited with code $code after $((attempt / 2)) s"
        fi
        sleep 0.5
    done
    assert_no_unnamed_module "$log" "image"
    note "image launcher stayed alive for $ALIVE_SECONDS s"

    taskkill //F //PID "$APP_PID" >/dev/null 2>&1 \
        || taskkill //F //IM "$APP_NAME.exe" >/dev/null 2>&1 || true
    wait "$APP_PID" 2>/dev/null || true
    APP_PID=""
}

smoke_windows_open_sample() {
    local image="releases/windows/$APP_NAME"
    [[ -x "$image/$APP_NAME.exe" ]] \
        || fail "no application image launcher at $image/$APP_NAME.exe - run scripts/package.sh first"

    local home="$WORK/sample-home"
    mkdir -p "$home"
    local home_native="$home"
    local sample="$WORK/cli-sample.txt"
    local sample_native="$sample"
    echo "This document was named on the command line." > "$sample"
    if command -v cygpath >/dev/null 2>&1; then
        home_native=$(cygpath -w "$home")
        sample_native=$(cygpath -w "$sample")
    fi
    local log="$WORK/sample.log"

    note "launching the application image with a document argument"
    JAVA_TOOL_OPTIONS="-Duser.home=$home_native" "$image/$APP_NAME.exe" "$sample_native" >"$log" 2>&1 &
    APP_PID=$!

    # There is no reliable interactive desktop on the Windows runners, so this
    # leg proves what it can: the launcher accepts an argument and keeps
    # running instead of dying on it. The document title is asserted on Linux.
    local attempt
    for attempt in $(seq 1 $((ALIVE_SECONDS * 2))); do
        if ! kill -0 "$APP_PID" 2>/dev/null; then
            local code=0
            wait "$APP_PID" || code=$?
            echo "---- image launcher log ----" >&2
            cat "$log" >&2 || true
            fail "the launcher exited with code $code after $((attempt / 2)) s with a document argument"
        fi
        sleep 0.5
    done
    assert_no_unnamed_module "$log" "image with a document argument"
    note "image launcher with a document argument stayed alive for $ALIVE_SECONDS s"

    taskkill //F //PID "$APP_PID" >/dev/null 2>&1 \
        || taskkill //F //IM "$APP_NAME.exe" >/dev/null 2>&1 || true
    wait "$APP_PID" 2>/dev/null || true
    APP_PID=""
}

case "$PLATFORM" in
    linux)
        smoke_linux_image
        if [[ "$OPEN_SAMPLE" == "1" ]]; then
            smoke_linux_open_sample
        fi
        if [[ "$SKIP_DEB" == "1" ]]; then
            note "skipping the .deb check (--skip-deb)"
        else
            smoke_linux_deb
        fi
        ;;
    windows)
        smoke_windows_image
        if [[ "$OPEN_SAMPLE" == "1" ]]; then
            smoke_windows_open_sample
        fi
        ;;
esac

note "PASS ($PLATFORM)"
