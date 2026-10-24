#!/usr/bin/env bash
#
# Publishes the current test count as a shields.io endpoint so the README badge
# follows the suite instead of drifting. The count is read from the surefire
# reports that "mvn test" has just written - no second source of truth - and
# only CI on main calls this with a token. Without GITHUB_TOKEN it is a safe
# local dry run that just prints the JSON:
#
#   ./scripts/publish-test-badge.sh                      # print the badge JSON
#   GITHUB_TOKEN=... ./scripts/publish-test-badge.sh     # publish to gh-pages
#
# The badge file lives on the gh-pages branch (badges/tests.json), which the
# README consumes via https://img.shields.io/endpoint. Publishing never touches
# the working tree: a throwaway clone in a temp dir is pushed and discarded.
#
set -euo pipefail

cd "$(dirname "$0")/.."

REPORTS_DIR="target/surefire-reports"
REMOTE="https://x-access-token:${GITHUB_TOKEN:-}@github.com/hyperpostulate/JTypeWriter.git"
BADGE_PATH="badges/tests.json"

# --- read the test totals ----------------------------------------------
# Each TEST-*.xml starts with a <testsuite ...> element carrying the counts for
# that class; the totals are their sum. grep/sed only, so CI needs nothing
# beyond the shell.
shopt -s nullglob
reports=("$REPORTS_DIR"/TEST-*.xml)
if (( ${#reports[@]} == 0 )); then
    echo "no surefire reports under $REPORTS_DIR - run 'mvn test' first" >&2
    exit 1
fi

total_tests=0
total_failed=0
for report in "${reports[@]}"; do
    line=$(grep -m1 -o '<testsuite [^>]*>' "$report")
    tests=$(printf '%s' "$line" | sed -n 's/.* tests="\([0-9]*\)".*/\1/p')
    failures=$(printf '%s' "$line" | sed -n 's/.* failures="\([0-9]*\)".*/\1/p')
    errors=$(printf '%s' "$line" | sed -n 's/.* errors="\([0-9]*\)".*/\1/p')
    total_tests=$((total_tests + ${tests:-0}))
    total_failed=$((total_failed + ${failures:-0} + ${errors:-0}))
done

# --- build the shields endpoint JSON -----------------------------------
# Green only for a clean, non-empty run; anything else is red so a broken build
# is visible on the README rather than hidden behind a stale count.
if (( total_tests > 0 && total_failed == 0 )); then
    message="$total_tests passing"
    color="brightgreen"
else
    message="$total_tests tests"
    color="red"
fi
json=$(printf '{"schemaVersion": 1, "label": "tests", "message": "%s", "color": "%s"}' \
    "$message" "$color")

# --- local dry run -----------------------------------------------------
# No token means we cannot (and must not) push; print what would be published.
if [[ -z "${GITHUB_TOKEN:-}" ]]; then
    printf '%s\n' "$json"
    echo "GITHUB_TOKEN is not set - nothing was pushed" >&2
    exit 0
fi

# --- publish to gh-pages -----------------------------------------------
WORK_DIR=$(mktemp -d "${TMPDIR:-/tmp}/jtypewriter-badge.XXXXXX")
trap 'rm -rf "$WORK_DIR"' EXIT

JSON_FILE="$WORK_DIR/tests.json"
printf '%s\n' "$json" > "$JSON_FILE"
REPO_DIR="$WORK_DIR/repo"

# Clone the badge branch; if it does not exist yet (first ever publication),
# start an orphan branch whose only content is .nojekyll plus the badge.
prepare_repository() {
    rm -rf "$REPO_DIR"
    if git clone --quiet --depth 1 --branch gh-pages "$REMOTE" "$REPO_DIR" 2>/dev/null; then
        return 0
    fi
    if ! git clone --quiet --depth 1 "$REMOTE" "$REPO_DIR" 2>/dev/null; then
        return 1
    fi
    git -C "$REPO_DIR" checkout --quiet --orphan gh-pages
    git -C "$REPO_DIR" rm -rf --quiet . 2>/dev/null || true
    : > "$REPO_DIR/.nojekyll"
}

# A concurrent run can advance gh-pages between clone and push, so the whole
# fetch/rebase/push cycle is retried a few times before giving up.
attempt=1
while (( attempt <= 3 )); do
    if ! prepare_repository; then
        echo "could not clone the repository (attempt $attempt of 3)" >&2
        attempt=$((attempt + 1))
        sleep 2
        continue
    fi

    mkdir -p "$REPO_DIR/badges"
    cp "$JSON_FILE" "$REPO_DIR/$BADGE_PATH"

    git -C "$REPO_DIR" config user.name "github-actions[bot]"
    git -C "$REPO_DIR" config user.email "github-actions[bot]@users.noreply.github.com"
    git -C "$REPO_DIR" add --all

    if git -C "$REPO_DIR" diff --cached --quiet; then
        echo "test count unchanged - badge already up to date"
        exit 0
    fi

    git -C "$REPO_DIR" commit --quiet -m "chore: update test count badge"
    # Rebase onto a newer gh-pages (if any) so the push is a fast-forward.
    git -C "$REPO_DIR" fetch --quiet origin gh-pages 2>/dev/null || true
    git -C "$REPO_DIR" rebase --quiet origin/gh-pages 2>/dev/null || true

    if git -C "$REPO_DIR" push --quiet origin HEAD:gh-pages 2>/dev/null; then
        echo "published $BADGE_PATH to gh-pages"
        exit 0
    fi

    echo "push rejected or failed (attempt $attempt of 3), retrying" >&2
    attempt=$((attempt + 1))
    sleep 2
done

# Publishing is a convenience, not part of the build contract: a fork without
# permission to push, or a transient network failure, must never turn a green
# build red. Warn and succeed.
echo "warning: could not publish the test count badge; the build is unaffected" >&2
exit 0
