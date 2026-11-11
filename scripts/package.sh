#!/usr/bin/env bash
#
# Builds JTypeWriter and turns it into shippable binaries under
# releases/<platform>/. Everything below releases/ is produced by this script -
# nothing there is hand-made - so a release can always be rebuilt from source:
#
#   ./scripts/package.sh                     # build + application image
#   ./scripts/package.sh --skip-build        # application image from an existing build
#   ./scripts/package.sh --installer         # build + image + platform installer
#   ./scripts/package.sh --skip-build --installer
#
# The application version has a single source of truth: pom.xml. It is read
# back from the artifact that was built, so nothing else can drift, and it is
# validated against jpackage's rules before it is used.
#
# The launcher icon is per platform (PNG on Linux, .ico on Windows), generated
# from src/main/resources/icons/jtypewriter.svg by scripts/generate-icons.py and
# passed to jpackage for images and installers.
#
set -euo pipefail

cd "$(dirname "$0")/.."

APP_NAME="JTypeWriter"
# jpackage's deb bundler lower-cases the package name, so `--name JTypeWriter`
# produces jtypewriter_<version>_amd64.deb, not JTypeWriter_*.deb. This stem is
# the single derivation used by the cleanup below, the "exactly one .deb"
# assertion and verify_deb_license; deriving it again per call site is how the
# cleanup came to match nothing while a stale package survived. The MSI keeps
# APP_NAME because jpackage really writes JTypeWriter-<version>.msi.
DEB_PACKAGE=$(printf '%s' "$APP_NAME" | tr '[:upper:]' '[:lower:]')
DESCRIPTION="A distraction-free text editor"
VENDOR="Mesut ORMANLI"
COPYRIGHT="Copyright (c) 2026 Mesut ORMANLI"
LICENSE_FILE="LICENSE"
NOTICE_FILE="NOTICE"
THIRD_PARTY_FILE="THIRD-PARTY-NOTICES"
LEGAL_DIR="legal"
MAIN_CLASS="org.mesutormanli.jtypewriter.JTypeWriterApp"

# The JDK modules the application and JavaFX actually need, measured with
# `jdeps --print-module-deps` over the thin jar and the JavaFX modules rather
# than copied from a tutorial. Everything else - jdk.compiler with its 10.4 MiB
# of compiler symbol tables, jdk.jshell, jconsole, the httpserver, the whole
# XML/JDBC/JNDI tail jpackage's full-JDK runtime carries - was dead weight the
# user installed and never ran.
#
# java.scripting is not obvious and is the one entry that has already gone
# missing once: javafx.fxml requires it (FXMLLoader evaluates expressions), so
# leaving it out boots to "Module java.scripting not found, required by
# javafx.fxml" and no window. jdk.jfr is a static requirement of javafx.base and
# is deliberately absent - an optional dependency must not cost every user a
# recording engine.
RUNTIME_MODULES="java.base,java.desktop,java.logging,java.scripting,jdk.unsupported"

# The directory that holds the JavaFX modules in the application image. The name
# matters twice: it becomes lib/<name>, and the launcher's --module-path option
# names it through $APPDIR (see the platform case block below - not $ROOTDIR,
# which is empty in an installed package). The script fills it in; jpackage is
# not asked to (see the copy below the image step).
JAVAFX_DIR_NAME="javafx"

# Every artifact must ship the license and the third-party notices. Missing source
# text is a hard error here rather than a silently license-less release.
for required in "$LICENSE_FILE" "$NOTICE_FILE" "$THIRD_PARTY_FILE" \
    "$LEGAL_DIR/GPL-2.0-with-classpath-exception.txt" "$LEGAL_DIR/LGPL-2.1.txt" \
    "$LEGAL_DIR/Bitstream-Vera.txt"; do
    if [[ ! -f "$required" ]]; then
        echo "no $required - refusing to package without it" >&2
        exit 1
    fi
done

BUILD=1
INSTALLER=0
for argument in "$@"; do
    case "$argument" in
        --skip-build) BUILD=0 ;;
        --installer)  INSTALLER=1 ;;
        *) echo "unknown option: $argument (expected --skip-build and/or --installer)" >&2; exit 1 ;;
    esac
done

if [[ "$BUILD" == "1" ]]; then
    mvn -B clean package
fi

# Exactly one application jar - a stale artifact from an older version would
# otherwise silently package the wrong thing. The image carries the *thin* jar:
# JavaFX now reaches the application as modules from lib/javafx, so the fat jar
# (still built, still the standalone `java -jar` convenience artifact) is not
# part of any image any more.
THIN_JAR=""
for candidate in target/jtypewriter-*.jar; do
    case "$candidate" in
        *-jar-with-dependencies.jar|*-sources.jar|*-javadoc.jar) continue ;;
    esac
    if [[ -f "$candidate" ]]; then
        if [[ -n "$THIN_JAR" ]]; then
            echo "more than one application jar under target/ - run 'mvn clean package'" >&2
            exit 1
        fi
        THIN_JAR="$candidate"
    fi
done
if [[ -z "$THIN_JAR" ]]; then
    echo "no application jar found under target/ - run 'mvn package' first" >&2
    exit 1
fi

# The version comes out of the build (pom.xml -> artifact name).
APP_VERSION=$(basename "$THIN_JAR" | sed -e 's/^jtypewriter-//' -e 's/\.jar$//')

# jpackage accepts one to three dot-separated integers whose first component is
# greater than zero, so a -SNAPSHOT version must fail here, with an explanation,
# instead of in the CI logs.
if ! printf '%s' "$APP_VERSION" | grep -Eq '^[1-9][0-9]*(\.[0-9]+){0,2}$'; then
    echo "version '$APP_VERSION' from pom.xml is not a valid jpackage app-version:" >&2
    echo "use one to three dot-separated integers with a non-zero first component (e.g. 1.0.0)" >&2
    exit 1
fi

case "$(uname -s)" in
    Linux*)               PLATFORM="linux" ;;
    MINGW*|MSYS*|CYGWIN*) PLATFORM="windows" ;;
    *)                    echo "unsupported platform: $(uname -s) (supported: Linux, Windows)" >&2; exit 1 ;;
esac

# The openjfx artifacts ship one classifier per platform, and the unclassified
# jar is only a placeholder that points at the classified one. The classifier
# names are the Maven ones (linux/win), not ours (linux/windows).
case "$PLATFORM" in
    linux)   JAVAFX_CLASSIFIER="linux" ;;
    windows) JAVAFX_CLASSIFIER="win" ;;
esac

# jpackage's app-image layout is not the same on the two platforms: Linux puts
# the application and the runtime under lib/, Windows puts them at the image
# root as app/ and runtime/. The JavaFX directory this script writes is
# lib/<javafx> on both, but the assertions below have to be told which spelling
# to look for. Getting this wrong is invisible on the platform you test on and
# fatal on the other, which is what the Windows CI leg found.
#
# The same case block picks how the launcher names that directory in its
# --module-path option, and it deliberately does *not* use $ROOTDIR. jpackage
# expands $ROOTDIR from imageRoot, and the launcher only sets imageRoot while it
# believes it is running from a plain app image. Installed from a .deb it asks
# dpkg instead (dpkg -S for the owning package, dpkg -L for the file list) and
# fills in only the app and runtime directories, so $ROOTDIR expands to the empty
# string and the option becomes /lib/javafx - an absolute path that does not
# exist. The installed launcher then dies with
# "FindException: Module javafx.controls not found" while the very same image
# starts fine, which is exactly what the Ubuntu smoke leg found in CI.
# $APPDIR *is* set in both branches, so the option is spelled relative to it:
# $APPDIR/../javafx is <image>/lib/javafx on Linux (app dir lib/app), and
# $APPDIR/../lib/javafx is <image>/lib/javafx on Windows (app dir app).
case "$PLATFORM" in
    linux)   IMAGE_APP_DIR="lib/app"; IMAGE_RUNTIME_DIR="lib/runtime"
             JFX_MODULE_PATH='$APPDIR/../javafx' ;;
    windows) IMAGE_APP_DIR="app";     IMAGE_RUNTIME_DIR="runtime"
             JFX_MODULE_PATH='$APPDIR/../lib/javafx' ;;
esac

# jpackage takes a different icon container per platform, all rendered from the
# canonical artwork. A missing file is a build error - never guess or omit the
# icon silently.
case "$PLATFORM" in
    linux)   ICON="src/main/resources/icons/jtypewriter-512.png" ;;
    windows) ICON="src/main/resources/icons/jtypewriter.ico" ;;
esac
if [[ ! -f "$ICON" ]]; then
    echo "no $PLATFORM icon at '$ICON'" >&2
    echo "the canonical artwork is src/main/resources/icons/jtypewriter.svg - regenerate the" >&2
    echo "PNG set and the Windows .ico with:" >&2
    echo "    ./scripts/generate-icons.py" >&2
    exit 1
fi

DEST="releases/$PLATFORM"
STAGING="target/jpackage-input"
JFX_MODULES="target/javafx-modules"
# Where the JavaFX modules end up in the image, filled in below by hand rather
# than by jpackage: bundling (rather than linking them into the runtime) keeps
# the natives usable, because jlink only carries lib/ and bin/ out of a modular
# jar and JavaFX keeps its .so files at the jar root - a jlinked JavaFX would
# start with a missing libglassgtk3.so.
JFX_IMAGE_DIR="$DEST/$APP_NAME/lib/$JAVAFX_DIR_NAME"
JLINK_RUNTIME="target/jlink-runtime"

if [[ ! -d "$JFX_MODULES" ]]; then
    echo "no JavaFX modules under $JFX_MODULES - run 'mvn package' first" >&2
    exit 1
fi

# jpackage refuses to overwrite existing output; these directories are the
# script's own build output, so they are always regenerated from scratch.
rm -rf "$DEST/$APP_NAME"
rm -f "$DEST/$DEB_PACKAGE"*.deb "$DEST/$APP_NAME"*.msi
# Start from an empty input directory: with --skip-build a previous run's jar
# would otherwise stay here and could be picked up instead of the current one.
rm -rf "$STAGING" "$JLINK_RUNTIME"
mkdir -p "$STAGING" "$DEST"
cp "$THIN_JAR" "$STAGING/"

# Only the platform jars: the unclassified openjfx artifact is a 300-byte
# placeholder whose Automatic-Module-Name (javafx.controlsEmpty and friends)
# would otherwise put a second, empty module on the path for no reason.
javafx_jars=0
for candidate in "$JFX_MODULES"/javafx-*-"$JAVAFX_CLASSIFIER".jar; do
    [[ -f "$candidate" ]] && javafx_jars=$((javafx_jars + 1))
done
if [[ "$javafx_jars" -ne 5 ]]; then
    echo "expected 5 JavaFX modules for '$JAVAFX_CLASSIFIER' under $JFX_MODULES, found $javafx_jars" >&2
    echo "(base, graphics, controls, fxml, media) - run 'mvn clean package'" >&2
    exit 1
fi

# A runtime built from the module list above instead of jpackage's "everything
# the host JDK has". Measured, not guessed: see RUNTIME_MODULES.
jlink \
    --add-modules "$RUNTIME_MODULES" \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=zip-6 \
    --output "$JLINK_RUNTIME"
echo "custom runtime written to $JLINK_RUNTIME ($(du -sh "$JLINK_RUNTIME" | cut -f1))"

jpackage \
    --type app-image \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --description "$DESCRIPTION" \
    --vendor "$VENDOR" \
    --copyright "$COPYRIGHT" \
    --input "$STAGING" \
    --main-jar "$(basename "$THIN_JAR")" \
    --main-class "$MAIN_CLASS" \
    --runtime-image "$JLINK_RUNTIME" \
    --icon "$ICON" \
    --dest "$DEST" \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "--enable-native-access=ALL-UNNAMED,javafx.graphics,javafx.media" \
    --java-options "--module-path=$JFX_MODULE_PATH" \
    --java-options '--add-modules=javafx.controls,javafx.fxml,javafx.media'

echo
echo "application image written to $DEST/ (version $APP_VERSION)"

# jpackage's app-image bundler has no license support at all (--license-file is
# fatal with --type app-image), so the license, the attribution notice and the
# third-party notices with their license texts are added to the image by hand
# and then asserted below.
LICENSE_TARGET="$DEST/$APP_NAME/LICENSE"
cp "$LICENSE_FILE" "$LICENSE_TARGET"
cp "$NOTICE_FILE" "$(dirname "$LICENSE_TARGET")/$NOTICE_FILE"
cp "$THIRD_PARTY_FILE" "$(dirname "$LICENSE_TARGET")/$THIRD_PARTY_FILE"
cp -R "$LEGAL_DIR" "$(dirname "$LICENSE_TARGET")/$LEGAL_DIR"

# The JavaFX modules are placed here, not handed to jpackage's --app-content.
# On Linux --app-content copied the whole tree including legal/, on Windows the
# same invocation produced an image whose lib/javafx held the jars but no
# legal/ subdirectory - and CI caught it only because the license is asserted.
# The image layout is our contract (the launcher resolves the module path at
# $ROOTDIR/lib/javafx), so the script writes it and then verifies it, instead of
# depending on an option whose recursive-copy behaviour differs per platform.
mkdir -p "$JFX_IMAGE_DIR/$LEGAL_DIR"
javafx_jars=0
for candidate in "$JFX_MODULES"/javafx-*-"$JAVAFX_CLASSIFIER".jar; do
    [[ -f "$candidate" ]] || continue
    cp "$candidate" "$JFX_IMAGE_DIR/"
    javafx_jars=$((javafx_jars + 1))
done
if [[ "$javafx_jars" -ne 5 ]]; then
    echo "expected 5 JavaFX modules for '$JAVAFX_CLASSIFIER', copied $javafx_jars" >&2
    exit 1
fi
# The JavaFX jars carry no license text of their own, and they are no longer
# inside our jar, so the text travels next to them: one source of truth (the
# checked-in legal/) rather than a second copy of the GPL. It has to be in the
# image before the installer step, because both installers are built from it.
cp "$LEGAL_DIR/GPL-2.0-with-classpath-exception.txt" "$JFX_IMAGE_DIR/$LEGAL_DIR/"

# A release without the license is invalid, in the image and in the jar alike.
for artifact in "$LICENSE_TARGET" \
    "$(dirname "$LICENSE_TARGET")/$NOTICE_FILE" \
    "$(dirname "$LICENSE_TARGET")/$THIRD_PARTY_FILE" \
    "$(dirname "$LICENSE_TARGET")/$LEGAL_DIR/GPL-2.0-with-classpath-exception.txt" \
    "$(dirname "$LICENSE_TARGET")/$LEGAL_DIR/LGPL-2.1.txt" \
    "$(dirname "$LICENSE_TARGET")/$LEGAL_DIR/Bitstream-Vera.txt" \
    "$JFX_IMAGE_DIR/$LEGAL_DIR/GPL-2.0-with-classpath-exception.txt"; do
    if [[ ! -f "$artifact" ]]; then
        echo "missing from the application image: $artifact" >&2
        exit 1
    fi
done
# The image layout is now an assertion rather than an assumption: a JavaFX
# module that did not make it into lib/<name> boots to "Module ... not found",
# and a runtime that is the host JDK again would silently undo the jlink gain.
if [[ ! -d "$JFX_IMAGE_DIR" ]]; then
    echo "JavaFX modules missing from the image at lib/$JAVAFX_DIR_NAME" >&2
    exit 1
fi
javafx_jars=0
for candidate in "$JFX_IMAGE_DIR"/javafx-*-"$JAVAFX_CLASSIFIER".jar; do
    [[ -f "$candidate" ]] && javafx_jars=$((javafx_jars + 1))
done
if [[ "$javafx_jars" -ne 5 ]]; then
    echo "the image carries $javafx_jars JavaFX modules, expected 5 in lib/$JAVAFX_DIR_NAME" >&2
    exit 1
fi
if [[ ! -s "$DEST/$APP_NAME/$IMAGE_RUNTIME_DIR/release" ]]; then
    echo "the image has no custom runtime ($IMAGE_RUNTIME_DIR/release is missing)" >&2
    exit 1
fi
if grep -q 'jdk.compiler' "$DEST/$APP_NAME/$IMAGE_RUNTIME_DIR/release"; then
    echo "the image runtime still carries jdk.compiler - jlink did not run or was ignored" >&2
    exit 1
fi
for entry in 'META-INF/LICENSE' 'META-INF/NOTICE' \
    'META-INF/THIRD-PARTY-NOTICES' \
    'META-INF/licenses/GPL-2.0-with-classpath-exception.txt' \
    'META-INF/licenses/LGPL-2.1.txt' \
    'META-INF/licenses/Bitstream-Vera.txt'; do
    if ! jar tf "$THIN_JAR" | grep -x "$entry" >/dev/null; then
        echo "license missing from $THIN_JAR (expected $entry)" >&2
        exit 1
    fi
done

# --- platform installer -------------------------------------------------
# Installer types and their options are platform specific, so they live here
# next to the version they are built from.

wrap_installer() {
    local type="$1"
    local image="$2"
    shift 2
    jpackage \
        --type "$type" \
        --app-image "$image" \
        --name "$APP_NAME" \
        --app-version "$APP_VERSION" \
        --description "$DESCRIPTION" \
        --vendor "$VENDOR" \
        --icon "$ICON" \
        --dest "$DEST" \
        "$@"
    echo "installer written to $DEST/ ($type)"
}

# jpackage's deb bundler registers the menu entry from the maintainer scripts,
# and it does so unguarded: postinst runs `xdg-desktop-menu install` under
# `set -e`, so on any machine without a writable XDG menu directory - a CI
# runner, a server, a container, a headless box - the call exits 3 and aborts
# the whole dpkg transaction, leaving jtypewriter in a half-configured state
# that later `dpkg -i` runs refuse to clean up. prerm has the same call behind
# an ownership test, which does not make it any more tolerant.
#
# Registering a desktop menu entry is a desktop nicety, not a condition of the
# package being installed, so the calls are made best-effort here. This is the
# only edit made to jpackage's own maintainer scripts and it deliberately adds
# `|| true` rather than deleting the calls: on a real desktop the entry is
# still installed and removed exactly as before.
relax_desktop_menu_registration() {
    local work="$1"
    local script
    for script in "$work/DEBIAN/postinst" "$work/DEBIAN/prerm"; do
        [[ -f "$script" ]] || continue
        # Comment lines are skipped so the explanation above cannot be rewritten
        # into a command; `|| true` is only appended once, so a rebuilt package
        # that is patched twice cannot grow into `|| true || true`.
        sed -i -E '/^[[:space:]]*#/!{/xdg-desktop-menu/{/\|\| true$/!s/[[:space:]]*$/ || true/}}' "$script"
    done
}

# jpackage's deb bundler copies bin/, lib/ and its own share/doc/ into the
# payload, but silently drops every file the app-image has at its root - so
# LICENSE, NOTICE, THIRD-PARTY-NOTICES and legal/ never reach the installer even
# though the image carries them. The release notes promise every asset carries
# them, and Apache-2.0 requires NOTICE to travel with the binary, so the payload
# is completed here rather than left to the image alone.
#
# The deb is unpacked with dpkg-deb -R (which keeps DEBIAN/ intact), the legal
# files are copied in, the menu registration is made headless-safe, md5sums and
# Installed-Size are regenerated because the payload grew, and the package is
# rebuilt. --root-owner-group keeps the files root:root without needing
# fakeroot. dpkg-deb is already required to verify the copyright document
# below, so this adds no new dependency.
complete_deb_payload() {
    local deb="$1"
    local work out
    work=$(mktemp -d)
    out=$(mktemp -d)
    local rebuilt="$out/$(basename "$deb")"

    if ! dpkg-deb -R "$deb" "$work" 2>/dev/null; then
        rm -rf "$work" "$out"
        echo "could not unpack $deb to complete its payload" >&2
        exit 1
    fi

    local payload="$work/opt/jtypewriter"
    if [[ ! -d "$payload" ]]; then
        rm -rf "$work" "$out"
        echo "unexpected deb layout: no payload at opt/jtypewriter" >&2
        exit 1
    fi

    cp "$LICENSE_FILE" "$NOTICE_FILE" "$THIRD_PARTY_FILE" "$payload/"
    rm -rf "$payload/$LEGAL_DIR"
    cp -R "$LEGAL_DIR" "$payload/$LEGAL_DIR"
    # The image's copies carry the build user's umask (664); a package payload
    # should be world-readable, not group-writable.
    chmod 644 "$payload/$LICENSE_FILE" "$payload/$NOTICE_FILE" "$payload/$THIRD_PARTY_FILE"
    chmod 644 "$payload/$LEGAL_DIR"/*

    relax_desktop_menu_registration "$work"

    # dpkg-deb trusts an existing md5sums file and would otherwise install files
    # the package database knows nothing about.
    rm -f "$work/DEBIAN/md5sums"
    (
        cd "$work"
        find . -path ./DEBIAN -prune -o -type f -printf '%P\0' \
            | LC_ALL=C sort -z \
            | xargs -0 md5sum > "$work/DEBIAN/md5sums"
    )

    # Installed-Size is in KiB and the control file is line oriented: the field
    # is replaced, never appended, so a rebuild cannot grow the stanza.
    local installed_size
    installed_size=$(du -sk --exclude=DEBIAN "$work" | cut -f1)
    sed -i -e "s/^Installed-Size:.*/Installed-Size: $installed_size/" "$work/DEBIAN/control"

    if ! dpkg-deb --build --root-owner-group "$work" "$rebuilt" >/dev/null 2>&1; then
        rm -rf "$work" "$out"
        echo "could not rebuild $deb with its legal payload" >&2
        exit 1
    fi
    mv -f "$rebuilt" "$deb"
    rm -rf "$work" "$out"
    echo "legal payload added to $(basename "$deb")"
}

# The one supported way to put our own text into the deb's copyright document:
# --resource-dir overrides the template jpackage fills in. Two options are not
# open to us - --license-file would dump the text into the DEP-5 "License:"
# field without the leading-space continuations the format requires, and
# --app-content is rejected for type deb (it is app-image only), so the file
# cannot simply be copied into the payload.
#
# The document is therefore generated from LICENSE itself: one source of truth,
# the whole Apache-2.0 text inside the artifact, and "License: Apache-2.0"
# instead of the "Unknown" jpackage writes when no license is supplied.
#
# The deb also carries the bundled runtime (OpenJDK and JavaFX, GPLv2 with the
# Classpath Exception) plus ICU/CLDR/Unicode data. DEP-5 takes the last matching
# stanza, so the runtime gets its own stanza first and our own files follow.
build_deb_resource_dir() {
    local resource_dir="target/jpackage-resource-dir"
    rm -rf "$resource_dir"
    mkdir -p "$resource_dir"
    {
        printf '%s\n' 'Format: https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/'
        printf '%s\n' 'Upstream-Name: JTypeWriter'
        printf '%s\n' 'Source: https://github.com/hyperpostulate/JTypeWriter'
        printf '\n'
        # DEP-5 resolves a file against the *last* stanza whose Files field
        # matches, so the catch-all has to come first: with it last, every
        # runtime file would be relicensed as Apache-2.0 and the stanzas below
        # would be decoration. The stanzas that follow therefore start broad
        # (our own code) and end narrow (the components we only redistribute).
        printf '%s\n' 'Files: *'
        printf '%s\n' 'Copyright: APPLICATION_COPYRIGHT'
        printf '%s\n' 'License: Apache-2.0'
        # DEP-5 continuation lines carry exactly one leading space and a blank
        # line inside the stanza is written as " .". The canonical Apache text
        # opens with a blank line, which would otherwise become a stray " ."
        # before the first line of the license.
        sed -e 's/[[:space:]]*$//' -e '/./,$!d' -e 's/^$/./' -e 's/^/ /' "$LICENSE_FILE"
        printf '\n'
        printf '%s\n' 'Files: opt/jtypewriter/lib/runtime/*'
        printf '%s\n' 'Copyright: 2026 Oracle and/or its affiliates, contributors'
        printf '%s\n' 'License: GPL-2.0-with-classpath-exception'
        printf '%s\n' ' The bundled OpenJDK runtime ships under GPLv2 with the Classpath'
        printf '%s\n' ' Exception; its notices, including the licenses of the data'
        printf '%s\n' ' libraries it contains, are reproduced unchanged in'
        printf '%s\n' ' opt/jtypewriter/lib/runtime/legal/.'
        printf '\n'
        # JavaFX used to sit inside the application jar; it is now its own
        # directory of modular jars, which is also where its license text lives.
        printf '%s\n' 'Files: opt/jtypewriter/lib/javafx/*'
        printf '%s\n' 'Copyright: Oracle and/or its affiliates; The GStreamer contributors'
        printf '%s\n' 'License: GPL-2.0-with-classpath-exception and LGPL-2.1-or-later'
        printf '%s\n' ' The JavaFX modules (GPLv2 with the Classpath Exception) and the'
        printf '%s\n' ' GStreamer Lite binary inside javafx.media (LGPL-2.1-or-later) are'
        printf '%s\n' ' reproduced here unmodified. The GPL text is in'
        printf '%s\n' ' opt/jtypewriter/lib/javafx/legal/, the full component list,'
        printf '%s\n' ' copyrights and source offers in opt/jtypewriter/THIRD-PARTY-NOTICES'
        printf '%s\n' ' and opt/jtypewriter/legal/.'
        printf '\n'
        printf '%s\n' 'Files: opt/jtypewriter/lib/app/*'
        printf '%s\n' 'Copyright: Mesut ORMANLI and contributors'
        printf '%s\n' 'License: Apache-2.0'
        printf '%s\n' ' The application jar is JTypeWriter itself; see the catch-all stanza'
        printf '%s\n' ' above for the full license text. It carries no third-party classes'
        printf '%s\n' ' any more - JavaFX is in opt/jtypewriter/lib/javafx/.'
    } > "$resource_dir/copyright"
    printf '%s' "$resource_dir"
}

# The deb's copyright document must carry the real license - not "Unknown" - and
# actually contain the Apache text, since the app image's LICENSE file is not
# part of the deb payload. Only checkable on machines with dpkg-deb.
verify_deb_license() {
    local deb=""
    shopt -s nullglob
    local candidates=("$DEST/$DEB_PACKAGE"*.deb)
    shopt -u nullglob
    for candidate in "${candidates[@]}"; do
        deb="$candidate"
    done
    if [[ -z "$deb" ]]; then
        echo "installer reported success but no .deb was found under $DEST/" >&2
        exit 1
    fi
    if ! command -v dpkg-deb >/dev/null 2>&1; then
        echo "note: dpkg-deb not available - skipping deb license verification"
        return
    fi

    local root
    root=$(mktemp -d)
    if ! dpkg-deb -x "$deb" "$root" 2>/dev/null; then
        rm -rf "$root"
        echo "could not extract $deb to verify its license metadata" >&2
        exit 1
    fi
    # jpackage puts the document under the install dir; a /usr install dir is the
    # only case where Debian's conventional path is used.
    local copyright=""
    for candidate in \
        "$root/usr/share/doc/jtypewriter/copyright" \
        "$root/opt/jtypewriter/share/doc/copyright"; do
        if [[ -f "$candidate" ]]; then
            copyright="$candidate"
            break
        fi
    done
    if [[ -z "$copyright" ]]; then
        rm -rf "$root"
        echo "no copyright document inside $deb" >&2
        exit 1
    fi
    local body
    body=$(cat "$copyright")
    rm -rf "$root"

    if [[ "$body" == *Unknown* || "$body" != *"License: Apache-2.0"* ]]; then
        echo "license metadata in $deb is missing or still 'Unknown'" >&2
        echo "check --copyright and build_deb_resource_dir in this script" >&2
        exit 1
    fi
    if [[ "$body" != *"Apache License"* || "$body" != *"Version 2.0, January 2004"* ]]; then
        echo "the Apache-2.0 text is missing from the copyright document in $deb" >&2
        exit 1
    fi
    # The bundled runtime is not ours to relicense, so its stanza must survive.
    if [[ "$body" != *"License: GPL-2.0-with-classpath-exception"* ]]; then
        echo "the bundled-runtime stanza is missing from the copyright document in $deb" >&2
        exit 1
    fi
    # ...and it must actually apply. DEP-5 uses the last matching stanza, so a
    # component stanza placed before the catch-all is silently overwritten by
    # it: the file would carry the Apache text while looking correctly licensed.
    if [[ "$body" != *"Files: opt/jtypewriter/lib/javafx/*"* ]]; then
        echo "the JavaFX stanza is missing from the copyright document in $deb" >&2
        exit 1
    fi
    local wildcard_at runtime_at javafx_at
    wildcard_at=$(awk '/^Files: \*$/{print NR; exit}' <<<"$body")
    runtime_at=$(awk '/^Files: opt\/jtypewriter\/lib\/runtime\/\*$/{print NR; exit}' <<<"$body")
    javafx_at=$(awk '/^Files: opt\/jtypewriter\/lib\/javafx\/\*$/{print NR; exit}' <<<"$body")
    if [[ -z "$wildcard_at" || -z "$runtime_at" || -z "$javafx_at" ]] \
        || (( wildcard_at > runtime_at || wildcard_at > javafx_at )); then
        echo "the copyright stanzas are in the wrong order in $deb: DEP-5 applies the" >&2
        echo "last matching stanza, so 'Files: *' must come before the component ones" >&2
        exit 1
    fi
    echo "deb license document verified ($deb)"
}

if [[ "$INSTALLER" == "1" ]]; then
    echo
    case "$PLATFORM" in
        linux)
            wrap_installer deb "$DEST/$APP_NAME" \
                --linux-shortcut \
                --linux-app-category "Office" \
                --linux-deb-maintainer "mesutormanli@gmail.com" \
                --copyright "$COPYRIGHT" \
                --resource-dir "$(build_deb_resource_dir)"
            shopt -s nullglob
            built_debs=("$DEST/$DEB_PACKAGE"*.deb)
            shopt -u nullglob
            if [[ ${#built_debs[@]} -ne 1 ]]; then
                echo "expected exactly one .deb under $DEST/, found ${#built_debs[@]}" >&2
                exit 1
            fi
            complete_deb_payload "${built_debs[0]}"
            verify_deb_license
            ;;
        windows)
            # A real EULA page in the installer; deb uses the template instead.
            wrap_installer msi "$DEST/$APP_NAME" \
                --win-menu --win-shortcut --win-dir-chooser \
                --license-file "$LICENSE_FILE"
            ;;
    esac
fi