#!/usr/bin/env bash
#
# Proves that what we ship is what the repository says we ship:
#
#   ./scripts/verify-artifacts.sh              # fresh build, then verify
#   ./scripts/verify-artifacts.sh --skip-build # verify what is already built
#
# One command compares three copies of the same payload byte for byte:
#
#   repository payload  LICENSE, NOTICE, THIRD-PARTY-NOTICES, legal/* and the
#                       fat jar Maven built from this source tree
#   application image   releases/<os>/JTypeWriter, as jpackage wrote it
#   installer payload   the files inside the .deb, unpacked into a temporary
#                       directory with dpkg-deb
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

VERSION=$(basename "$FAT_JAR" | sed -e 's/^jtypewriter-//' -e 's/-jar-with-dependencies\.jar$//')
DEST="releases/$PLATFORM"

IMAGE="$DEST/$APP_NAME"
if [[ ! -d "$IMAGE" ]]; then
    echo "application image '$IMAGE' is missing - run scripts/package.sh first" >&2
    exit 1
fi

# The image keeps the fat jar under lib/app/; find it rather than hard-coding.
IMAGE_JAR=$(find "$IMAGE" -type f -name 'jtypewriter-*-jar-with-dependencies.jar' -print)
if [[ -z "$IMAGE_JAR" || "$(printf '%s\n' "$IMAGE_JAR" | wc -l)" -ne 1 ]]; then
    echo "expected exactly one application jar inside '$IMAGE', found: ${IMAGE_JAR:-none}" >&2
    exit 1
fi

compare "fat jar" "$FAT_JAR" "$IMAGE_JAR"

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
# the packed notice must be the checked-in one - not merely present.
if command -v unzip >/dev/null 2>&1; then
    for entry in META-INF/THIRD-PARTY-NOTICES META-INF/LICENSE META-INF/NOTICE \
        META-INF/licenses/GPL-2.0-with-classpath-exception.txt \
        META-INF/licenses/LGPL-2.1.txt \
        META-INF/licenses/Bitstream-Vera.txt; do
        work=$(mktemp)
        if ! unzip -p "$FAT_JAR" "$entry" > "$work" 2>/dev/null; then
            echo "FAIL $entry: missing from $FAT_JAR" >&2
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
        compare "jar entry $entry" "$source_file" "$work"
        rm -f "$work"
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

    DEB_JAR=$(find "$DEB_PAYLOAD/lib/app" -type f -name 'jtypewriter-*-jar-with-dependencies.jar' -print 2>/dev/null || true)
    if [[ -z "$DEB_JAR" ]]; then
        echo "FAIL no application jar inside the .deb payload" >&2
        FAILURES=$((FAILURES + 1))
    else
        compare "deb fat jar" "$FAT_JAR" "$DEB_JAR"
    fi
fi

echo
if [[ "$FAILURES" -ne 0 ]]; then
    echo "$FAILURES of $CHECKS comparisons failed - the artifacts do not match the repository" >&2
    exit 1
fi
echo "all $CHECKS comparisons passed for version $VERSION on $PLATFORM"
