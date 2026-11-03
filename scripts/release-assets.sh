#!/usr/bin/env bash
#
# Stages one platform's release artifacts under a normalised, portable name:
#
#   scripts/release-assets.sh <version> <os> <arch> <releases-dir> <out-dir>
#
# package.sh writes the raw jpackage output into releases/<os>/ with names the
# installer backend chose (jtypewriter_1.0.0_amd64.deb on Linux, JTypeWriter-1.0.0.msi
# on Windows). A release with several platforms needs one naming scheme, so
# every asset becomes JTypeWriter-<version>-<os>-<arch>.<ext> and a downloader can
# guess the URL without reading the release page.
#
# Besides the installer, the application image is archived as a .tar.gz holding
# exactly one top-level directory. Which channel becomes the product default is
# still an open decision, so both ship for now; the archive is also the only way
# to reach Windows without an installer.
#
# This script deliberately does not hash anything: the release job merges all
# platforms into one directory and produces SHA256SUMS there (scripts/checksums.sh),
# so a per-platform checksum would be a second, divergent source of truth.
#
set -euo pipefail

if [[ $# -ne 5 ]]; then
    echo "usage: $0 <version> <os> <arch> <releases-dir> <out-dir>" >&2
    echo "  <os>   linux | windows" >&2
    echo "  <arch> x64 | arm64" >&2
    exit 1
fi

VERSION="$1"
OS="$2"
ARCH="$3"
RELEASES_DIR="$4"
OUT_DIR="$5"

if ! printf '%s' "$VERSION" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
    echo "version '$VERSION' is not of the form <major>.<minor>.<patch> (e.g. 1.0.0)" >&2
    exit 1
fi

# The app image and installer live under a per-platform directory that matches
# runner.os; anything else means the caller passed the wrong thing and the
# resulting asset would be mislabelled.
case "$OS" in
    linux)
        IMAGE="JTypeWriter"
        INSTALLER_GLOB='*.deb'
        INSTALLER_EXT="deb"
        ;;
    windows)
        IMAGE="JTypeWriter"
        INSTALLER_GLOB='*.msi'
        INSTALLER_EXT="msi"
        ;;
    *)
        echo "unsupported os '$OS' (expected linux or windows)" >&2
        exit 1
        ;;
esac

case "$ARCH" in
    x64|arm64) ;;
    *)
        echo "unsupported arch '$ARCH' (expected x64 or arm64)" >&2
        exit 1
        ;;
esac

if [[ ! -d "$RELEASES_DIR" ]]; then
    echo "releases directory '$RELEASES_DIR' does not exist - run scripts/package.sh first" >&2
    exit 1
fi
if [[ ! -d "$RELEASES_DIR/$IMAGE" ]]; then
    echo "application image '$RELEASES_DIR/$IMAGE' is missing - run scripts/package.sh first" >&2
    exit 1
fi

# Exactly one installer, the same rule package.sh applies to the fat jar: a
# left-over installer from an older version would otherwise be published under
# the new version's name.
shopt -s nullglob
installers=("$RELEASES_DIR"/$INSTALLER_GLOB)
shopt -u nullglob

if [[ ${#installers[@]} -eq 0 ]]; then
    echo "no $INSTALLER_EXT installer under '$RELEASES_DIR' - run scripts/package.sh --installer first" >&2
    exit 1
fi
if [[ ${#installers[@]} -gt 1 ]]; then
    echo "more than one $INSTALLER_EXT installer under '$RELEASES_DIR' - remove the stale ones:" >&2
    printf '  %s\n' "${installers[@]}" >&2
    exit 1
fi

mkdir -p "$OUT_DIR"

INSTALLER_ASSET="$OUT_DIR/JTypeWriter-$VERSION-$OS-$ARCH.$INSTALLER_EXT"
PORTABLE_ASSET="$OUT_DIR/JTypeWriter-$VERSION-$OS-$ARCH.tar.gz"

cp -- "${installers[0]}" "$INSTALLER_ASSET"

# GNU tar reads a colon in an argument as "remote host:path", and a Windows
# drive letter is precisely that shape: `D:\a\...` is taken for host `D`, so on
# the Windows runner this line used to die with "Cannot connect to D" before a
# single byte was written. Every path handed to tar therefore goes through
# cygpath, which turns a drive path into the POSIX form MSYS tar understands
# (`/d/a/...`, no colon left to misread).
#
# Only paths that actually contain a colon are converted, and only when cygpath
# exists - on Linux there is nothing to convert, so the same line runs unchanged
# and the script stays one implementation rather than two.
to_tar_path() {
    local path="$1"
    if [[ "$path" == *:* ]] && command -v cygpath >/dev/null 2>&1; then
        cygpath -u "$path"
    else
        printf '%s' "$path"
    fi
}

TAR_SOURCE=$(to_tar_path "$RELEASES_DIR")
TAR_ARCHIVE=$(to_tar_path "$PORTABLE_ASSET")

# -C keeps exactly one top-level directory in the archive, and the relative name
# (not an absolute path) is what makes the same tarball safe to unpack anywhere.
# GNU tar, bsdtar and the tar Git Bash ships on Windows all accept this form.
tar -czf "$TAR_ARCHIVE" -C "$TAR_SOURCE" -- "$IMAGE"

echo "installer asset:  $INSTALLER_ASSET"
echo "portable archive: $PORTABLE_ASSET"
