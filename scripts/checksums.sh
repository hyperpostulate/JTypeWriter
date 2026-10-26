#!/usr/bin/env bash
#
# Regenerates <dir>/SHA256SUMS over every regular file in <dir>:
#
#   scripts/checksums.sh <dir>
#
# An artifact nobody can verify is an artifact nobody can trust:
# a release without checksums cannot be distinguished from a tampered or
# truncated download. The release job calls this after merging all platforms
# into one directory; it is also runnable locally on any directory of assets.
#
# The format is what `sha256sum -c SHA256SUMS` expects when run from inside the
# directory: "<hex>  <name>", two spaces, bare filenames, no "./" prefix.
#
set -euo pipefail

# Byte-order of the filename sort must not depend on the runner's locale; a
# locale-dependent order would produce different bytes for the same inputs.
export LC_ALL=C

if [[ $# -ne 1 ]]; then
    echo "usage: $0 <dir>" >&2
    exit 1
fi

DIR="$1"

if [[ ! -d "$DIR" ]]; then
    echo "directory '$DIR' does not exist or is not a directory" >&2
    exit 1
fi

# Plain `*` would skip dotfiles, so hidden files are matched explicitly as well.
shopt -s nullglob
paths=("$DIR"/* "$DIR"/.[!.]* "$DIR"/..?*)
shopt -u nullglob

names=()
for path in "${paths[@]}"; do
    [[ -f "$path" ]] || continue
    name="${path##*/}"
    [[ "$name" == "SHA256SUMS" ]] && continue
    # SHA256SUMS is line-based and the verification loop reads filenames as
    # words; such a name cannot round-trip, so refuse instead of writing a file
    # that silently verifies the wrong thing.
    if [[ "$name" == *$'\n'* ]]; then
        echo "file name contains a newline, cannot represent it in SHA256SUMS: $name" >&2
        exit 1
    fi
    if [[ "$name" == -* ]]; then
        echo "file name starts with '-', which sha256sum would read as an option: $name" >&2
        exit 1
    fi
    names+=("$name")
done

if [[ ${#names[@]} -eq 0 ]]; then
    echo "no files to checksum in '$DIR'" >&2
    exit 1
fi

if command -v sha256sum >/dev/null 2>&1; then
    SUM=(sha256sum)
elif command -v shasum >/dev/null 2>&1; then
    # macOS has no sha256sum; shasum -a 256 emits the same "<hex>  <name>" lines.
    SUM=(shasum -a 256)
else
    echo "neither sha256sum nor shasum is available" >&2
    exit 1
fi

sorted=$(printf '%s\n' "${names[@]}" | sort)

# Removing the stale file first means a deleted asset cannot survive in the new
# one, even if the run is interrupted after the deletion.
rm -f -- "$DIR/SHA256SUMS"

(
    cd "$DIR"
    while IFS= read -r name; do
        [[ -n "$name" ]] || continue
        "${SUM[@]}" -- "$name"
    done <<< "$sorted"
) > "$DIR/SHA256SUMS"

echo "wrote $DIR/SHA256SUMS:"
cat "$DIR/SHA256SUMS"
