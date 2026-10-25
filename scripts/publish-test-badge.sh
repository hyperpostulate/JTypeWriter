#!/usr/bin/env bash
#
# Publishes the current test count as a shields.io endpoint so the README badge
# follows the suite instead of drifting. The count is read from the surefire
# reports that the CI runs have just written - no second source of truth - and
# only CI on main calls this with a token. Without GITHUB_TOKEN it is a safe
# local dry run that just prints the JSON:
#
#   ./scripts/publish-test-badge.sh                      # print the badge JSON
#   GITHUB_TOKEN=... ./scripts/publish-test-badge.sh     # publish to gh-pages
#
# CI re-runs the suite under several system locales and keeps each run's reports
# in its own directory (target/surefire-reports, target/surefire-reports-tr,
# ...). The badge sums all of them and says how many runs that was, because the
# count is the sum of every run, not the last locale to finish: a suite of 272
# tests run under three locales publishes "816 passing (3 locales)".
#
# The badge file lives on the gh-pages branch (badges/tests.json), which the
# README consumes via https://img.shields.io/endpoint. Publishing never touches
# the working tree: a throwaway clone in a temp dir is pushed and discarded.
#
# A failed publication is a failure: if the badge cannot be cloned, committed or
# pushed after the retries below, the script exits non-zero so the job goes red.
# A badge that silently goes stale is worse than a red run. Only two paths are
# tolerant: the no-token dry run (prints the JSON, exits 0) and "the badge
# already carries this count" (nothing to publish, exits 0).
#
set -euo pipefail

cd "$(dirname "$0")/.."

# Reports of the main run, plus one sibling directory per additional locale run.
REPORTS_BASE="target/surefire-reports"
BADGE_PATH="badges/tests.json"
MAX_ATTEMPTS=3

# --- resolve owner/repo ------------------------------------------------
# The slug used to live inside the clone URL, together with the token. Fork it
# into one variable instead, so a fork or a renamed repository needs no edit.
# Precedence: GITHUB_REPOSITORY (GitHub sets owner/repo for every run), then the
# origin remote of this checkout, then a hard error - guessing a repository to
# push a badge to is exactly how a fork ends up writing somewhere it should not.
repo_slug_from_url() {
    local url=${1%/}   # trailing slash, as some clones leave behind
    case $url in
        *github.com[/:]*) ;;
        *) return 1 ;;
    esac
    url=${url#*github.com}
    url=${url#[/:]}    # https://github.com/o/r or git@github.com:o/r
    url=${url%.git}
    # Exactly owner/repo, nothing deeper and nothing missing.
    [[ $url =~ ^[^/]+/[^/]+$ ]] || return 1
    printf '%s\n' "$url"
}

if [[ -n "${GITHUB_REPOSITORY:-}" ]]; then
    REPO_SLUG=$GITHUB_REPOSITORY
elif origin_url=$(git remote get-url origin 2>/dev/null) &&
     REPO_SLUG=$(repo_slug_from_url "$origin_url"); then
    :
else
    echo "cannot determine the repository: set GITHUB_REPOSITORY=owner/repo or add an 'origin' remote pointing at github.com/owner/repo" >&2
    exit 1
fi

# Credentials travel out of band, never in the URL: a token in the URL ends up
# in argv (visible in the process list), in git's error messages and in the
# clone's .git/config. An http.extraheader passed with -c is used for one
# command only and is never written to the fetched repository. Do not add
# "set -x": that would print the header value into the CI log.
REMOTE="https://github.com/${REPO_SLUG}.git"

# --- read the test totals ----------------------------------------------
# Each TEST-*.xml starts with a <testsuite ...> element carrying the counts for
# that class; the totals are their sum. grep/sed/coreutils only, so CI needs
# nothing beyond the shell. Every report directory is read: a later run must
# never hide an earlier one, and the badge must reflect all locale runs.
shopt -s nullglob
report_dirs=()
for dir in "$REPORTS_BASE" "$REPORTS_BASE"-*; do
    if [[ -d $dir ]]; then
        report_dirs+=("$dir")
    fi
done

total_tests=0
total_failed=0
read_dirs=0
for dir in "${report_dirs[@]}"; do
    reports=("$dir"/TEST-*.xml)
    (( ${#reports[@]} > 0 )) || continue

    dir_tests=0
    dir_failed=0
    for report in "${reports[@]}"; do
        line=$(grep -m1 -o '<testsuite [^>]*>' "$report")
        tests=$(printf '%s' "$line" | sed -n 's/.* tests="\([0-9]*\)".*/\1/p')
        failures=$(printf '%s' "$line" | sed -n 's/.* failures="\([0-9]*\)".*/\1/p')
        errors=$(printf '%s' "$line" | sed -n 's/.* errors="\([0-9]*\)".*/\1/p')
        dir_tests=$((dir_tests + ${tests:-0}))
        dir_failed=$((dir_failed + ${failures:-0} + ${errors:-0}))
    done

    # Per-directory breakdown on stderr, so a CI log shows where the total came
    # from without changing the JSON printed on stdout.
    echo "reports: $dir -> $dir_tests tests, $dir_failed failures/errors (${#reports[@]} files)" >&2
    total_tests=$((total_tests + dir_tests))
    total_failed=$((total_failed + dir_failed))
    read_dirs=$((read_dirs + 1))
done

if (( read_dirs == 0 )); then
    echo "no surefire reports under $REPORTS_BASE or $REPORTS_BASE-* - run 'mvn test' first" >&2
    exit 1
fi

# --- build the shields endpoint JSON -----------------------------------
# Green only for a clean, non-empty run; anything else is red so a broken build
# is visible on the README rather than hidden behind a stale count. Several
# report directories mean several full runs of the same suite, so the green
# message says how many runs were summed instead of pretending the number is
# the size of the suite.
if (( total_tests > 0 && total_failed == 0 )); then
    message="$total_tests passing"
    (( read_dirs > 1 )) && message="$message ($read_dirs locales)"
    color="brightgreen"
else
    message="$total_tests tests"
    color="red"
fi
json=$(printf '{"schemaVersion": 1, "label": "tests", "message": "%s", "color": "%s"}' \
    "$message" "$color")

# --- local dry run -----------------------------------------------------
# No token means we cannot (and must not) push; print what would be published.
# This stays a success: it is the documented way to inspect the badge locally.
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

auth_header="AUTHORIZATION: basic $(printf 'x-access-token:%s' "$GITHUB_TOKEN" | base64 | tr -d '\n')"

# Clone the badge branch; if it does not exist yet (first ever publication),
# start an orphan branch whose only content is .nojekyll plus the badge. The
# first clone stays quiet because a missing gh-pages branch is expected; the
# fallback clone is allowed to speak, and it cannot echo the token because the
# token is not in its URL.
prepare_repository() {
    rm -rf "$REPO_DIR"
    if git -c http.extraheader="$auth_header" clone --quiet --depth 1 \
        --branch gh-pages "$REMOTE" "$REPO_DIR" 2>/dev/null; then
        return 0
    fi
    if ! git -c http.extraheader="$auth_header" clone --quiet --depth 1 \
        "$REMOTE" "$REPO_DIR"; then
        return 1
    fi
    git -C "$REPO_DIR" checkout --quiet --orphan gh-pages
    git -C "$REPO_DIR" rm -rf --quiet . 2>/dev/null || true
    : > "$REPO_DIR/.nojekyll"
}

# A concurrent run can advance gh-pages between clone and push, so the whole
# fetch/rebase/push cycle is retried a few times before giving up.
attempt=1
while (( attempt <= MAX_ATTEMPTS )); do
    if ! prepare_repository; then
        echo "cannot clone https://github.com/$REPO_SLUG.git for the badge branch (attempt $attempt of $MAX_ATTEMPTS)" >&2
        attempt=$((attempt + 1))
        sleep 2
        continue
    fi

    mkdir -p "$REPO_DIR/badges"
    cp "$JSON_FILE" "$REPO_DIR/$BADGE_PATH"

    git -C "$REPO_DIR" config user.name "github-actions[bot]"
    git -C "$REPO_DIR" config user.email "github-actions[bot]@users.noreply.github.com"
    git -C "$REPO_DIR" add --all

    # The badge already carries this count: nothing to publish, and this is a
    # success, not a skipped failure.
    if git -C "$REPO_DIR" diff --cached --quiet; then
        echo "test count unchanged - badge already up to date"
        exit 0
    fi

    # A failed commit means the clone is unusable; retrying cannot help, so name
    # the repository and stop rather than leaving the badge stale behind exit 0.
    if ! git -C "$REPO_DIR" commit --quiet -m "chore: update test count badge"; then
        echo "cannot commit the badge update in the clone of $REPO_SLUG" >&2
        exit 1
    fi
    # Rebase onto a newer gh-pages (if any) so the push is a fast-forward. No
    # upstream commit is a normal case (fresh branch, or nobody else pushed).
    git -C "$REPO_DIR" -c http.extraheader="$auth_header" fetch --quiet origin gh-pages 2>/dev/null || true
    git -C "$REPO_DIR" rebase --quiet origin/gh-pages 2>/dev/null || true

    if git -C "$REPO_DIR" -c http.extraheader="$auth_header" push --quiet origin HEAD:gh-pages; then
        echo "published $BADGE_PATH to gh-pages"
        exit 0
    fi

    echo "push to $REPO_SLUG rejected or failed (attempt $attempt of $MAX_ATTEMPTS), retrying" >&2
    attempt=$((attempt + 1))
    sleep 2
done

echo "cannot publish the test count badge to $REPO_SLUG after $MAX_ATTEMPTS attempts" >&2
exit 1
