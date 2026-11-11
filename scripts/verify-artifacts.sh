#!/usr/bin/env bash
#
# Proves that what we ship is what the repository says we ship:
#
#   ./scripts/verify-artifacts.sh              # fresh build, then verify
#   ./scripts/verify-artifacts.sh --skip-build # verify what is already built
#
# One command compares three copies of the same payload byte for byte:
#
#   repository payload  LICENSE, NOTICE, THIRD-PARTY-NOTICES, legal/*, the thin
#                       jar Maven built from this source tree, and the JavaFX
#                       module jars the image ships
#   application image   releases/<os>/JTypeWriter, as jpackage wrote it
#   installer payload   the files inside the .deb, unpacked into a temporary
#                       directory with dpkg-deb
#
# The image layout itself is checked as well, because "the application starts"
# is not evidence of it: the runtime must be the jlink one rather than the host
# JDK, and the JavaFX modules must be files in lib/javafx rather than classes
# buried in a jar the JVM loads through the unnamed module.
#
# A release is only reproducible if those three agree. package.sh asserts that
# the files exist and that the legal texts are where they should be; it says
# nothing about whether they are the *same bytes*. A stale jar, a hand-edited
# notice or an installer built from an older image all pass package.sh and fail
# here, which is exactly the drift a release cannot detect after the fact.
#
# The .deb is the only installer this script unpacks. That is deliberate: Linux
# x64 is the platform this project releases first, and the rubber-stamp that
# matters for it is the one that can be reproduced on any Debian/Ubuntu machine.
# A Windows .msi is compared only as part of the application image.
#
# The jar entries are compared too: the notice inside the jar must equal the
# checked-in file, not just be present (PackagingNoticesTest asserts the same
# for target/classes; this asserts it for the artifact that ships).
#
set -euo pipefail

cd "$(dirname "$0")/.."

BUILD=1
for argument in "$@"; do
    case "$argument" in
        --skip-build) BUILD=0 ;;
        *) echo "unknown option: $argument (expected --skip-build)" >&2; exit 1 ;;
    esac
done

APP_NAME="JTypeWriter"
# Same derivation as package.sh: jpackage lower-cases the deb package name, so
# the artifact is jtypewriter_<version>_amd64.deb rather than JTypeWriter_*.deb.
DEB_PACKAGE=$(printf '%s' "$APP_NAME" | tr '[:upper:]' '[:lower:]')

case "$(uname -s)" in
    Linux*)               PLATFORM="linux" ;;
    MINGW*|MSYS*|CYGWIN*) PLATFORM="windows" ;;
    *)                    echo "unsupported platform: $(uname -s) (supported: Linux, Windows)" >&2; exit 1 ;;
esac

if [[ "$BUILD" == "1" ]]; then
    # A fresh build is the point of the default mode: the comparison below only
    # means something when the jar was produced from the source tree we are in.
    mvn -B clean package
    bash scripts/package.sh --skip-build --installer
fi

FAILURES=0
CHECKS=0

# compare <label> <expected> <actual>...
#
# Every copy past <expected> must be byte-identical to it. All mismatches are
# reported before the script exits, so one run lists every drifted file instead
# of stopping at the first.
compare() {
    local label="$1" expected="$2"
    shift 2
    CHECKS=$((CHECKS + 1))
    if [[ ! -f "$expected" ]]; then
        echo "FAIL $label: missing $expected" >&2
        FAILURES=$((FAILURES + 1))
        return
    fi
    local actual
    for actual in "$@"; do
        if [[ ! -f "$actual" ]]; then
            echo "FAIL $label: missing $actual" >&2
            FAILURES=$((FAILURES + 1))
            continue
        fi
        if cmp -s -- "$expected" "$actual"; then
            echo "ok   $label ($(basename "$actual"))"
        else
            echo "FAIL $label: $actual differs from $expected" >&2
            echo "     expected $(wc -c < "$expected") bytes, got $(wc -c < "$actual")" >&2
            cmp -- "$expected" "$actual" 2>&1 | head -n 1 | sed 's/^/     /' >&2 || true
            FAILURES=$((FAILURES + 1))
        fi
    done
}

FAT_JAR=""
for candidate in target/jtypewriter-*-jar-with-dependencies.jar; do
    if [[ -f "$candidate" ]]; then
        if [[ -n "$FAT_JAR" ]]; then
            echo "more than one jar-with-dependencies under target/ - run this without --skip-build" >&2
            exit 1
        fi
        FAT_JAR="$candidate"
    fi
done
if [[ -z "$FAT_JAR" ]]; then
    echo "no jar-with-dependencies under target/ - run 'mvn package' or drop --skip-build" >&2
    exit 1
fi

# The image ships the thin jar now: JavaFX arrives as modules from lib/javafx,
# so this - not the fat jar - is the application payload whose bytes must match
# the repository.
THIN_JAR=""
for candidate in target/jtypewriter-*.jar; do
    case "$candidate" in
        *-jar-with-dependencies.jar|*-sources.jar|*-javadoc.jar) continue ;;
    esac
    if [[ -f "$candidate" ]]; then
        if [[ -n "$THIN_JAR" ]]; then
            echo "more than one application jar under target/ - run this without --skip-build" >&2
            exit 1
        fi
        THIN_JAR="$candidate"
    fi
done
if [[ -z "$THIN_JAR" ]]; then
    echo "no application jar under target/ - run 'mvn package' or drop --skip-build" >&2
    exit 1
fi

VERSION=$(basename "$THIN_JAR" | sed -e 's/^jtypewriter-//' -e 's/\.jar$//')
DEST="releases/$PLATFORM"

case "$PLATFORM" in
    linux)   JAVAFX_CLASSIFIER="linux" ;;
    windows) JAVAFX_CLASSIFIER="win" ;;
esac

# jpackage's app-image layout differs per platform: Linux keeps the application
# and the runtime under lib/, Windows puts them at the image root as app/ and
# runtime/. The JavaFX directory is the script's own contract and stays
# lib/javafx on both, because $ROOTDIR points at the image root either way.
case "$PLATFORM" in
    linux)   IMAGE_APP_DIR="lib/app"; IMAGE_RUNTIME_DIR="lib/runtime" ;;
    windows) IMAGE_APP_DIR="app";     IMAGE_RUNTIME_DIR="runtime" ;;
esac

IMAGE="$DEST/$APP_NAME"
if [[ ! -d "$IMAGE" ]]; then
    echo "application image '$IMAGE' is missing - run scripts/package.sh first" >&2
    exit 1
fi

# The image keeps the application jar under the platform's app directory; find
# it rather than hard-coding the file name, and insist there is exactly one so
# a stale jar cannot hide.
IMAGE_JAR=$(find "$IMAGE/$IMAGE_APP_DIR" -type f -name 'jtypewriter-*.jar' -print)
if [[ -z "$IMAGE_JAR" || "$(printf '%s\n' "$IMAGE_JAR" | wc -l)" -ne 1 ]]; then
    echo "expected exactly one application jar inside '$IMAGE/$IMAGE_APP_DIR', found: ${IMAGE_JAR:-none}" >&2
    exit 1
fi

compare "application jar" "$THIN_JAR" "$IMAGE_JAR"

# JavaFX is shipped as modular jars, not linked into the runtime, so each one is
# a file whose bytes must match the build like any other payload.
JFX_IMAGE_DIR="$IMAGE/lib/javafx"
for module_jar in target/javafx-modules/javafx-*-"$JAVAFX_CLASSIFIER".jar; do
    [[ -f "$module_jar" ]] || continue
    compare "javafx $(basename "$module_jar")" "$module_jar" "$JFX_IMAGE_DIR/$(basename "$module_jar")"
done

# The JavaFX license text travels beside the modules (the jars carry none of
# their own), and it is the same checked-in file as everywhere else.
compare "javafx license" "legal/GPL-2.0-with-classpath-exception.txt" \
    "$JFX_IMAGE_DIR/legal/GPL-2.0-with-classpath-exception.txt"

# The runtime is the jlink one: a runtime that silently reverted to the host JDK
# would still start the application, so the difference is asserted rather than
# felt in the download size.
if [[ ! -s "$IMAGE/$IMAGE_RUNTIME_DIR/release" ]]; then
    echo "FAIL the image has no custom runtime ($IMAGE_RUNTIME_DIR/release)" >&2
    FAILURES=$((FAILURES + 1))
else
    CHECKS=$((CHECKS + 1))
    runtime_modules=$(sed -n 's/^MODULES="\(.*\)"$/\1/p' "$IMAGE/$IMAGE_RUNTIME_DIR/release")
    for required in java.base java.desktop java.logging java.scripting; do
        if [[ " $runtime_modules " != *" $required "* ]]; then
            echo "FAIL the image runtime is missing $required" >&2
            FAILURES=$((FAILURES + 1))
        fi
    done
    for dead in jdk.compiler jdk.jshell jdk.jconsole; do
        if [[ " $runtime_modules " == *" $dead "* ]]; then
            echo "FAIL the image runtime still ships $dead (jlink gain lost)" >&2
            FAILURES=$((FAILURES + 1))
        fi
    done
    if [[ ! -d "$IMAGE/$IMAGE_RUNTIME_DIR/legal" ]]; then
        echo "FAIL the image runtime carries no legal/ tree" >&2
        FAILURES=$((FAILURES + 1))
    fi
fi

# The legal payload sits next to the launcher in the image root. package.sh
# wrote it there, so the location is known; the comparison is about the bytes at
# those paths.
IMAGE_ROOT="$IMAGE"

LEGAL_FILES=(
    "LICENSE"
    "NOTICE"
    "THIRD-PARTY-NOTICES"
    "legal/GPL-2.0-with-classpath-exception.txt"
    "legal/LGPL-2.1.txt"
    "legal/Bitstream-Vera.txt"
)
for relative in "${LEGAL_FILES[@]}"; do
    compare "$relative" "$relative" "$IMAGE_ROOT/$relative"
done

# The jar's own entries are what every runtime unpacking the artifact reads, so
# the packed notice must be the checked-in one - not merely present. Both jars
# are checked: the thin one ships in the image, the fat one is still the
# standalone `java -jar` artifact and makes the same promise.
if command -v unzip >/dev/null 2>&1; then
    for packed_jar in "$THIN_JAR" "$FAT_JAR"; do
        for entry in META-INF/THIRD-PARTY-NOTICES META-INF/LICENSE META-INF/NOTICE \
            META-INF/licenses/GPL-2.0-with-classpath-exception.txt \
            META-INF/licenses/LGPL-2.1.txt \
            META-INF/licenses/Bitstream-Vera.txt; do
            work=$(mktemp)
            if ! unzip -p "$packed_jar" "$entry" > "$work" 2>/dev/null; then
                echo "FAIL $entry: missing from $packed_jar" >&2
                FAILURES=$((FAILURES + 1))
                rm -f "$work"
                continue
            fi
            case "$entry" in
                META-INF/THIRD-PARTY-NOTICES) source_file="THIRD-PARTY-NOTICES" ;;
                META-INF/LICENSE)             source_file="LICENSE" ;;
                META-INF/NOTICE)              source_file="NOTICE" ;;
                META-INF/licenses/*)          source_file="legal/${entry#META-INF/licenses/}" ;;
            esac
            compare "jar entry $entry ($(basename "$packed_jar"))" "$source_file" "$work"
            rm -f "$work"
        done
    done
else
    echo "note: unzip not available - skipping the jar-entry comparisons"
fi

# --- installer payload --------------------------------------------------
# dpkg-deb unpacking is only meaningful on Linux; on Windows the application
# image comparison above is all this script can honestly claim.
if [[ "$PLATFORM" != "linux" ]]; then
    echo "note: installer payload comparison is implemented for the .deb only - skipping"
elif ! command -v dpkg-deb >/dev/null 2>&1; then
    echo "FAIL no dpkg-deb available to unpack the .deb" >&2
    FAILURES=$((FAILURES + 1))
else
    shopt -s nullglob
    DEBS=("$DEST/$DEB_PACKAGE"*.deb)
    shopt -u nullglob
    if [[ ${#DEBS[@]} -ne 1 ]]; then
        echo "expected exactly one .deb under '$DEST/', found ${#DEBS[@]} - run scripts/package.sh --installer" >&2
        exit 1
    fi

    DEB_ROOT=$(mktemp -d)
    trap 'rm -rf "$DEB_ROOT"' EXIT
    if ! dpkg-deb -x "${DEBS[0]}" "$DEB_ROOT" 2>/dev/null; then
        echo "could not unpack ${DEBS[0]}" >&2
        exit 1
    fi

    # jpackage installs under /opt/jtypewriter by default; a different prefix
    # means the installer was built by something other than scripts/package.sh.
    DEB_PAYLOAD="$DEB_ROOT/opt/jtypewriter"
    if [[ ! -d "$DEB_PAYLOAD" ]]; then
        echo "unexpected .deb layout: no payload at opt/jtypewriter" >&2
        exit 1
    fi

    for relative in "${LEGAL_FILES[@]}"; do
        compare "deb $relative" "$relative" "$DEB_PAYLOAD/$relative"
    done

    DEB_JAR=$(find "$DEB_PAYLOAD/lib/app" -type f -name 'jtypewriter-*.jar' -print 2>/dev/null || true)
    if [[ -z "$DEB_JAR" ]]; then
        echo "FAIL no application jar inside the .deb payload" >&2
        FAILURES=$((FAILURES + 1))
    else
        compare "deb application jar" "$THIN_JAR" "$DEB_JAR"
    fi

    # The JavaFX modules and their license text must survive the installer the
    # same way they survive the image; jpackage copies lib/ wholesale, so a
    # missing file here would mean the payload was completed from somewhere else.
    for module_jar in target/javafx-modules/javafx-*-"$JAVAFX_CLASSIFIER".jar; do
        [[ -f "$module_jar" ]] || continue
        compare "deb javafx $(basename "$module_jar")" "$module_jar" \
            "$DEB_PAYLOAD/lib/javafx/$(basename "$module_jar")"
    done
    compare "deb javafx license" "legal/GPL-2.0-with-classpath-exception.txt" \
        "$DEB_PAYLOAD/lib/javafx/legal/GPL-2.0-with-classpath-exception.txt"
fi

echo
if [[ "$FAILURES" -ne 0 ]]; then
    echo "$FAILURES of $CHECKS comparisons failed - the artifacts do not match the repository" >&2
    exit 1
fi
echo "all $CHECKS comparisons passed for version $VERSION on $PLATFORM"
