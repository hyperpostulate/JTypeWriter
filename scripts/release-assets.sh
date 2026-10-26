#!/usr/bin/env bash
#
# Stages one platform's release artifacts under a normalised, portable name:
#
#   scripts/release-assets.sh <version> <os> <arch> <releases-dir> <out-dir>
#
# package.sh writes the raw jpackage output into releases/<os>/ with names the
# installer backend chose (jtypewriter_1.0.0_amd64.deb on Linux, JTypeWriter-1.0.0.msi
# on Windows, ...). A release with three platforms needs one naming scheme, so
# every asset becomes JTypeWriter-<version>-<os>-<arch>.<ext> and a downloader can
# guess the URL without reading the release page.
#
# Besides the installer, the application image is archived as a .tar.gz holding
# exactly one top-level directory. Which channel becomes the product default is
# still an open decision, so both ship for now; the archive is also the only way
# to reach macOS/Windows without an installer.
#
# This script deliberately does not hash anything: the release job merges all
# platforms into one directory and produces SHA256SUMS there (scripts/checksums.sh),
# so a per-platform checksum would be a second, divergent source of truth.
#
set -euo pipefail

if [[ $# -ne 5 ]]; then
    echo "usage: $0 <version> <os> <arch> <releases-dir> <out-dir>" >&2
    echo "  <os>   linux | windows | mac" >&2
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
    mac)
        IMAGE="JTypeWriter.app"
        INSTALLER_GLOB='*.dmg'
        INSTALLER_EXT="dmg"
        ;;
    *)
        echo "unsupported os '$OS' (expected linux, windows or mac)" >&2
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

# -C keeps exactly one top-level directory in the archive, and the relative name
# (not an absolute path) is what makes the same tarball safe to unpack anywhere.
# GNU tar, bsdtar and the tar Git Bash ships on Windows all accept this form.
tar -czf "$PORTABLE_ASSET" -C "$RELEASES_DIR" -- "$IMAGE"

echo "installer asset:  $INSTALLER_ASSET"
echo "portable archive: $PORTABLE_ASSET"
