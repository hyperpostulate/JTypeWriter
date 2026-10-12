#!/usr/bin/env bash
#
# Builds JTypeWriter and wraps it into a native application image under
# releases/<platform>/. Everything below releases/ is produced by this script -
# nothing there is hand-made - so a release can always be rebuilt from source:
#
#   ./scripts/package.sh              # build + test + package
#   ./scripts/package.sh --skip-build # package the existing build only
#
# Platform installers (deb / msi / dmg) are built the same way by the CI
# workflow, which wraps the application image produced here.
#
set -euo pipefail

cd "$(dirname "$0")/.."

APP_NAME="JTypeWriter"
APP_VERSION="0.0.1"
MAIN_CLASS="org.mesutormanli.jtypewriter.JTypeWriterApp"

if [[ "${1:-}" != "--skip-build" ]]; then
    mvn -B clean package
fi

FAT_JAR=""
for candidate in target/jtypewriter-*-jar-with-dependencies.jar; do
    [[ -f "$candidate" ]] && FAT_JAR="$candidate"
done
if [[ -z "$FAT_JAR" ]]; then
    echo "no jar-with-dependencies found under target/ - run 'mvn package' first" >&2
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

# jpackage refuses to overwrite an existing image; this directory is the
# script's own build output, so it is always regenerated from scratch.
rm -rf "$DEST/$APP_NAME" "$DEST/$APP_NAME.app"
mkdir -p "$STAGING" "$DEST"
cp "$FAT_JAR" "$STAGING/"

jpackage \
    --type app-image \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --description "A distraction-free text editor" \
    --vendor "Mesut ORMANLI" \
    --copyright "Copyright (c) 2026 Mesut ORMANLI" \
    --input "$STAGING" \
    --main-jar "$(basename "$FAT_JAR")" \
    --main-class "$MAIN_CLASS" \
    --dest "$DEST" \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "--enable-native-access=ALL-UNNAMED"

echo
echo "application image written to $DEST/"