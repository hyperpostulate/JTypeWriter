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
# The launcher icon is per platform (PNG on Linux, .ico on Windows, .icns on
# macOS), generated from src/main/resources/icons/jtypewriter.svg by
# scripts/generate-icons.py and passed to jpackage for images and installers.
#
set -euo pipefail

cd "$(dirname "$0")/.."

APP_NAME="JTypeWriter"
DESCRIPTION="A distraction-free text editor"
VENDOR="Mesut ORMANLI"
COPYRIGHT="Copyright (c) 2026 Mesut ORMANLI"
LICENSE_FILE="LICENSE"
NOTICE_FILE="NOTICE"
THIRD_PARTY_FILE="THIRD-PARTY-NOTICES"
LEGAL_DIR="legal"
MAIN_CLASS="org.mesutormanli.jtypewriter.JTypeWriterApp"

# Every artifact must ship the license and the third-party notices. Missing source
# text is a hard error here rather than a silently license-less release.
for required in "$LICENSE_FILE" "$NOTICE_FILE" "$THIRD_PARTY_FILE" \
    "$LEGAL_DIR/GPL-2.0-with-classpath-exception.txt" "$LEGAL_DIR/LGPL-2.1.txt"; do
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

# Exactly one runnable fat jar - a stale artifact from an older version would
# otherwise silently package the wrong thing.
FAT_JAR=""
for candidate in target/jtypewriter-*-jar-with-dependencies.jar; do
    if [[ -f "$candidate" ]]; then
        if [[ -n "$FAT_JAR" ]]; then
            echo "more than one jar-with-dependencies under target/ - run 'mvn clean package'" >&2
            exit 1
        fi
        FAT_JAR="$candidate"
    fi
done
if [[ -z "$FAT_JAR" ]]; then
    echo "no jar-with-dependencies found under target/ - run 'mvn package' first" >&2
    exit 1
fi

# The version comes out of the build (pom.xml -> artifact name).
APP_VERSION=$(basename "$FAT_JAR" | sed -e 's/^jtypewriter-//' -e 's/-jar-with-dependencies\.jar$//')

# jpackage accepts one to three dot-separated integers whose first component is
# greater than zero - macOS rejects 0.x bundle versions - so a -SNAPSHOT
# version must fail here, with an explanation, instead of in the CI logs.
if ! printf '%s' "$APP_VERSION" | grep -Eq '^[1-9][0-9]*(\.[0-9]+){0,2}$'; then
    echo "version '$APP_VERSION' from pom.xml is not a valid jpackage app-version:" >&2
    echo "use one to three dot-separated integers with a non-zero first component (e.g. 1.0.0)" >&2
    exit 1
fi

case "$(uname -s)" in
    Linux*)               PLATFORM="linux" ;;
    Darwin*)              PLATFORM="mac" ;;
    MINGW*|MSYS*|CYGWIN*) PLATFORM="windows" ;;
    *)                    echo "unsupported platform: $(uname -s)" >&2; exit 1 ;;
esac

# jpackage takes a different icon container per platform, all rendered from the
# canonical artwork. A missing file is a build error - never guess or omit the
# icon silently.
case "$PLATFORM" in
    linux)   ICON="src/main/resources/icons/jtypewriter-512.png" ;;
    windows) ICON="src/main/resources/icons/jtypewriter.ico" ;;
    mac)     ICON="src/main/resources/icons/jtypewriter.icns" ;;
esac
if [[ ! -f "$ICON" ]]; then
    echo "no $PLATFORM icon at '$ICON'" >&2
    echo "the canonical artwork is src/main/resources/icons/jtypewriter.svg - regenerate the" >&2
    echo "PNG set, the Windows .ico and the macOS .icns with:" >&2
    echo "    ./scripts/generate-icons.py" >&2
    exit 1
fi

DEST="releases/$PLATFORM"
STAGING="target/jpackage-input"

# jpackage refuses to overwrite existing output; this directory is the script's
# own build output, so it is always regenerated from scratch.
rm -rf "$DEST/$APP_NAME" "$DEST/$APP_NAME.app"
rm -f "$DEST/$APP_NAME"*.deb "$DEST/$APP_NAME"*.msi "$DEST/$APP_NAME"*.dmg
# Start from an empty input directory: with --skip-build a previous run's fat jar
# would otherwise stay here and could be picked up instead of the current one.
rm -rf "$STAGING"
mkdir -p "$STAGING" "$DEST"
cp "$FAT_JAR" "$STAGING/"

jpackage \
    --type app-image \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --description "$DESCRIPTION" \
    --vendor "$VENDOR" \
    --copyright "$COPYRIGHT" \
    --input "$STAGING" \
    --main-jar "$(basename "$FAT_JAR")" \
    --main-class "$MAIN_CLASS" \
    --icon "$ICON" \
    --dest "$DEST" \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "--enable-native-access=ALL-UNNAMED"

echo
echo "application image written to $DEST/ (version $APP_VERSION)"

# jpackage's app-image bundler has no license support at all (--license-file is
# fatal with --type app-image), so the license, the attribution notice and the
# third-party notices with their license texts are added to the image by hand
# and then asserted below. On macOS the image is a bundle, so its
# Contents/Resources is the equivalent of the Linux/Windows image root.
case "$PLATFORM" in
    mac) LICENSE_TARGET="$DEST/$APP_NAME.app/Contents/Resources/LICENSE" ;;
    *)   LICENSE_TARGET="$DEST/$APP_NAME/LICENSE" ;;
esac
cp "$LICENSE_FILE" "$LICENSE_TARGET"
cp "$NOTICE_FILE" "$(dirname "$LICENSE_TARGET")/$NOTICE_FILE"
cp "$THIRD_PARTY_FILE" "$(dirname "$LICENSE_TARGET")/$THIRD_PARTY_FILE"
cp -R "$LEGAL_DIR" "$(dirname "$LICENSE_TARGET")/$LEGAL_DIR"

# A release without the license is invalid, in the image and in the jar alike.
for artifact in "$LICENSE_TARGET" \
    "$(dirname "$LICENSE_TARGET")/$NOTICE_FILE" \
    "$(dirname "$LICENSE_TARGET")/$THIRD_PARTY_FILE" \
    "$(dirname "$LICENSE_TARGET")/$LEGAL_DIR/GPL-2.0-with-classpath-exception.txt" \
    "$(dirname "$LICENSE_TARGET")/$LEGAL_DIR/LGPL-2.1.txt"; do
    if [[ ! -f "$artifact" ]]; then
        echo "missing from the application image: $artifact" >&2
        exit 1
    fi
done
for entry in 'META-INF/LICENSE' 'META-INF/NOTICE' \
    'META-INF/THIRD-PARTY-NOTICES' \
    'META-INF/licenses/GPL-2.0-with-classpath-exception.txt' \
    'META-INF/licenses/LGPL-2.1.txt'; do
    if ! jar tf "$FAT_JAR" | grep -x "$entry" >/dev/null; then
        echo "license missing from $FAT_JAR (expected $entry)" >&2
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
        printf '%s\n' 'Files: opt/jtypewriter/lib/runtime/*'
        printf '%s\n' 'Copyright: 2026 Oracle and/or its affiliates, contributors'
        printf '%s\n' 'License: GPL-2.0-with-classpath-exception'
        printf '%s\n' ' The bundled OpenJDK runtime ships under GPLv2 with the Classpath'
        printf '%s\n' ' Exception; its notices, including the licenses of the data'
        printf '%s\n' ' libraries it contains, are reproduced unchanged in'
        printf '%s\n' ' opt/jtypewriter/lib/runtime/legal/.'
        printf '%s\n' ' The JavaFX libraries (GPLv2 with the Classpath Exception) and'
        printf '%s\n' ' GStreamer Lite (LGPL-2.1-or-later) are not part of that runtime;'
        printf '%s\n' ' their components, copyrights, license texts and source offers are'
        printf '%s\n' ' in opt/jtypewriter/THIRD-PARTY-NOTICES and opt/jtypewriter/legal/.'
        printf '\n'
        printf '%s\n' 'Files: opt/jtypewriter/lib/app/*'
        printf '%s\n' 'Copyright: Oracle and/or its affiliates; The GStreamer contributors;'
        printf '%s\n' ' contributors to the components named in THIRD-PARTY-NOTICES'
        printf '%s\n' 'License: GPL-2.0-with-classpath-exception and LGPL-2.1-or-later'
        printf '%s\n' ' The application jar carries the JavaFX classes (GPLv2 with the'
        printf '%s\n' ' Classpath Exception) and GStreamer Lite (LGPL-2.1-or-later).'
        printf '%s\n' ' The components, copyrights, license texts and source offers are'
        printf '%s\n' ' listed in opt/jtypewriter/THIRD-PARTY-NOTICES and reproduced in'
        printf '%s\n' ' opt/jtypewriter/legal/.'
        printf '\n'
        printf '%s\n' 'Files: *'
        printf '%s\n' 'Copyright: APPLICATION_COPYRIGHT'
        printf '%s\n' 'License: Apache-2.0'
        # DEP-5 continuation lines carry exactly one leading space and a blank
        # line inside the stanza is written as " .". The canonical Apache text
        # opens with a blank line, which would otherwise become a stray " ."
        # before the first line of the license.
        sed -e 's/[[:space:]]*$//' -e '/./,$!d' -e 's/^$/./' -e 's/^/ /' "$LICENSE_FILE"
    } > "$resource_dir/copyright"
    printf '%s' "$resource_dir"
}

# The deb's copyright document must carry the real license - not "Unknown" - and
# actually contain the Apache text, since the app image's LICENSE file is not
# part of the deb payload. Only checkable on machines with dpkg-deb.
verify_deb_license() {
    local deb=""
    shopt -s nullglob
    local candidates=("$DEST"/*.deb)
    shopt -u nullglob
    for candidate in "${candidates[@]}"; do
        deb="$candidate" # jpackage lower-cases the package name: jtypewriter_1.0.0_amd64.deb
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
            verify_deb_license
            ;;
        windows)
            # A real EULA page in the installer; deb uses the template instead.
            wrap_installer msi "$DEST/$APP_NAME" \
                --win-menu --win-shortcut --win-dir-chooser \
                --license-file "$LICENSE_FILE"
            ;;
        mac)
            # dmg embeds the text as its EULA.
            wrap_installer dmg "$DEST/$APP_NAME.app" \
                --mac-package-identifier "org.mesutormanli.jtypewriter" \
                --license-file "$LICENSE_FILE"
            ;;
    esac
fi