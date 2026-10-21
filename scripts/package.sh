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
set -euo pipefail

cd "$(dirname "$0")/.."

APP_NAME="JTypeWriter"
DESCRIPTION="A distraction-free text editor"
VENDOR="Mesut ORMANLI"
COPYRIGHT="Copyright (c) 2026 Mesut ORMANLI"
MAIN_CLASS="org.mesutormanli.jtypewriter.JTypeWriterApp"

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
    --dest "$DEST" \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "--enable-native-access=ALL-UNNAMED"

echo
echo "application image written to $DEST/ (version $APP_VERSION)"

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
        --dest "$DEST" \
        "$@"
    echo "installer written to $DEST/ ($type)"
}

if [[ "$INSTALLER" == "1" ]]; then
    echo
    case "$PLATFORM" in
        linux)
            wrap_installer deb "$DEST/$APP_NAME" \
                --linux-shortcut \
                --linux-app-category "Office" \
                --linux-deb-maintainer "mesutormanli@gmail.com"
            ;;
        windows)
            wrap_installer msi "$DEST/$APP_NAME" \
                --win-menu --win-shortcut --win-dir-chooser
            ;;
        mac)
            wrap_installer dmg "$DEST/$APP_NAME.app" \
                --mac-package-identifier "org.mesutormanli.jtypewriter"
            ;;
    esac
fi