#!/usr/bin/env bash
#
# Records the README's hero animation from the packaged application, once per
# language, so the screenshot in the documentation is a picture of the artifact
# a user downloads rather than of a developer's checkout:
#
#   ./scripts/capture-hero-gif.sh              # both languages
#   ./scripts/capture-hero-gif.sh en           # just one
#   ./scripts/capture-hero-gif.sh --list       # what it would write
#
# Output: docs/media/hero-en.gif and docs/media/hero-tr.gif.
#
# Why an application image and not `mvn javafx:run`: the image is what ships,
# so it is what the document should show. Run `./scripts/package.sh` first.
#
# Why the capture is scripted at all: a hand-made recording is unrepeatable -
# nobody can re-take it after a toolbar change, and it drifts from the product
# silently. Every input here is fixed in this file (window geometry, theme,
# language, the words typed, the target length), so re-running it after a UI
# change produces a comparable result instead of a new opinion.
#
# Why Xvfb and xfwm4: Xvfb alone has no window manager, so the window would
# have no title bar and the title bar is part of what the reader sees. xfwm4
# draws the frame the capture region is measured from.
#
# Why `JAVA_TOOL_OPTIONS=-Duser.home=...`: the application persists settings,
# recent files and the single-instance lock under ~/.jtypewriter, and the
# developer's real directory must not be read (a remembered document would put
# someone else's text in the recording) or written. A throwaway HOME is the
# same isolation smoke-appimage.sh uses.
#
# The GIF is a derived document asset, not an icon: it is not byte-reproducible
# (encoder versions differ) and it is therefore not covered by
# generate-icons.py's hash manifest. It lives in docs/ because
# src/main/resources is packed into the jar and generate-icons.py --check
# rejects stray images there.
#
# Requirements: Xvfb, xfwm4, xdotool, ffmpeg (with x11grab), xwininfo.
#
set -euo pipefail

cd "$(dirname "$0")/.."

APP_NAME="JTypeWriter"
IMAGE_BIN="releases/linux/JTypeWriter/bin/JTypeWriter"
OUT_DIR="docs/media"
SCREEN="1280x900x24"
WINDOW_WIDTH=1000
WINDOW_HEIGHT=480
WINDOW_X=140
WINDOW_Y=90
# 15 fps: the paragraph grows on nearly every frame instead of in blocks. The
# per-character delay is derived from TARGET_SECONDS below rather than fixed,
# so the clip is the same length whatever the sample text says.
FPS=15
# The whole clip, start to finish. The README autoplays this on first paint, so
# a fixed length is part of the document contract; the typing rate is what
# moves when the text changes.
TARGET_SECONDS=10
# Recorded before the first keystroke and after the last one: the status bar's
# words-per-minute figure settles over a second or so, and a GIF that cuts off
# mid-count is a GIF of a bug under load.
PRE_ROLL_SECONDS=1
POST_ROLL_SECONDS=2.5
OUTPUT_WIDTH=900
# A README opens on every visitor's first screen; two files at 3 MiB each is
# already a noticeable download, so the budget is a hard failure, not advice.
MAX_BYTES=3145728

# Turkish needs the dotless i, s-cedilla and g-breve in the recording, so the
# sample text is Turkish in the Turkish capture - an English paragraph with a
# Turkish toolbar would not show what the language actually does.
declare -A SAMPLE_TEXT=(
    [en]='Later the street drew back into its own darkness. Somewhere a door closed, and the page kept filling anyway.'
    [tr]='Sonra sokak kendi karanlığına çekildi. Uzakta bir kapı kapandı ve sayfa dolmaya devam etti.'
)
# The document name is part of every frame's title bar, so it is the language's
# own word for the same idea rather than a transliteration of it.
declare -A DOCUMENT_NAME=(
    [en]='night-notes.txt'
    [tr]='gece-notlari.txt'
)

note() { echo "==> $*"; }
fail() { echo "error: $*" >&2; exit 1; }

WANT=()
for argument in "$@"; do
    case "$argument" in
        --list)  LIST_ONLY=1 ;;
        en|tr)   WANT+=("$argument") ;;
        *) fail "unknown option: $argument (expected en, tr or --list)" ;;
    esac
done
if [[ ${#WANT[@]} -eq 0 ]]; then
    WANT=(en tr)
fi

if [[ "${LIST_ONLY:-0}" == "1" ]]; then
    for language in "${WANT[@]}"; do
        echo "$OUT_DIR/hero-$language.gif"
    done
    exit 0
fi

for tool in Xvfb xfwm4 xdotool ffmpeg ffprobe xwininfo; do
    command -v "$tool" >/dev/null 2>&1 \
        || fail "$tool is not installed (apt-get install xvfb xfwm4 xdotool ffmpeg x11-utils)"
done
[[ -x "$IMAGE_BIN" ]] \
    || fail "$IMAGE_BIN is missing or not executable - run ./scripts/package.sh first"

VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -n 1)
[[ -n "$VERSION" ]] || fail "could not read the version from pom.xml"

WORK=$(mktemp -d "${TMPDIR:-/tmp}/jtw-hero-XXXXXX")
DISPLAY_NUMBER=""
XVFB_PID=""
WM_PID=""
APP_PID=""

cleanup() {
    [[ -n "$APP_PID" ]] && kill "$APP_PID" 2>/dev/null || true
    [[ -n "$WM_PID" ]] && kill "$WM_PID" 2>/dev/null || true
    [[ -n "$XVFB_PID" ]] && kill "$XVFB_PID" 2>/dev/null || true
    [[ -n "$DISPLAY_NUMBER" && "${KEEP_WORK:-0}" != "1" ]] && rm -f "/tmp/.X11-unix/X$DISPLAY_NUMBER"
    if [[ "${KEEP_WORK:-0}" == "1" ]]; then
        note "kept the work directory: $WORK"
    else
        rm -rf "$WORK"
    fi
}
trap cleanup EXIT

pick_display() {
    local number
    for number in $(seq 90 120); do
        if [[ ! -e "/tmp/.X11-unix/X$number" ]]; then
            echo "$number"
            return
        fi
    done
    fail "no free X display between :90 and :120"
}

seed_settings() {
    local language="$1" home="$2"
    local ui_language
    # AppSettings stores `Language.name()`, so the value is upper case; a lower
    # case value is ignored and the UI falls back to the system locale, which is
    # how the first take of the Turkish recording ended up with an English
    # toolbar.
    ui_language=$(printf '%s' "$language" | tr '[:lower:]' '[:upper:]')
    mkdir -p "$home/.jtypewriter"
    # Written before the first launch, so the application opens straight into
    # the state the recording needs instead of being driven there with clicks.
    # window.maximized=false is the one that matters most: a maximized window
    # would be captured at the screen size instead of the documented geometry.
    cat >"$home/.jtypewriter/settings.properties" <<EOF
schema.version=$VERSION
language=$ui_language
theme=DARK
text.color=DEFAULT
font.size=16
sound.volume=0.0
toolbar.visible=true
stats.size=MEDIUM
notice.storage.shown=true
welcome.enabled=false
window.maximized=false
window.fullscreen=false
window.x=$WINDOW_X
window.y=$WINDOW_Y
window.width=$WINDOW_WIDTH
window.height=$WINDOW_HEIGHT
EOF
}

# The window with the largest area is the main stage. `xdotool search` also
# matches the window manager's 10x10 helper window of the same name, and
# capturing that would record a black rectangle. The floor rejects both the
# helper and a window that is still being laid out, so the caller can poll.
main_window() {
    local id geometry area best="" best_area=0 width height
    for id in $(xdotool search --name "$APP_NAME" 2>/dev/null || true); do
        geometry=$(xdotool getwindowgeometry --shell "$id" 2>/dev/null) || continue
        width=$(sed -n 's/^WIDTH=//p' <<<"$geometry")
        height=$(sed -n 's/^HEIGHT=//p' <<<"$geometry")
        area=$((width * height))
        if ((width >= WINDOW_WIDTH * 3 / 4 && height >= WINDOW_HEIGHT * 3 / 4 && area > best_area)); then
            best_area=$area
            best="$id"
        fi
    done
    [[ -n "$best" ]] || return 1
    echo "$best"
}

# The region to grab is the window manager's frame, not the application's own
# window: the frame is the parent of the client window and it carries the title
# bar, which the README's picture must include. xwininfo reports the frame's
# absolute position and size; the client is inset by the decoration.
frame_geometry() {
    local client="$1" parent
    parent=$(xwininfo -id "$client" -tree | sed -n 's/.*Parent window id: \(0x[0-9a-f]*\).*/\1/p')
    if [[ -n "$parent" && "$parent" != "0x0" ]]; then
        xwininfo -id "$parent"
    else
        xwininfo -id "$client"
    fi
}

capture() {
    local language="$1" home="$WORK/home-$1"
    local document="$WORK/${DOCUMENT_NAME[$1]}"
    local output="$OUT_DIR/hero-$language.gif" raw="$WORK/raw-$language.mp4"

    note "recording the $language hero"
    seed_settings "$language" "$home"
    : >"$document"

    # user.home is what AppDirs reads; HOME and the working directory are set
    # too so nothing else can reach the developer's own files by accident.
    DISPLAY=":$DISPLAY_NUMBER" HOME="$home" \
        JAVA_TOOL_OPTIONS="-Duser.home=$home" \
        "$IMAGE_BIN" "$document" >"$WORK/app-$language.log" 2>&1 &
    APP_PID=$!

    local deadline=$((SECONDS + 60)) id
    while ((SECONDS < deadline)); do
        id=$(main_window || true)
        [[ -n "$id" ]] && break
        if ! kill -0 "$APP_PID" 2>/dev/null; then
            cat "$WORK/app-$language.log" >&2
            fail "the application exited before showing a window"
        fi
        sleep 0.3
    done
    [[ -n "${id:-}" ]] || {
        xwininfo -root -tree >&2 || true
        fail "no $APP_NAME window appeared on :$DISPLAY_NUMBER"
    }

    local info
    # The frame must be the decorated window, and it must have stopped moving:
    # the window manager reparents the stage a moment after it appears, and a
    # geometry read during that transition is a few pixels off - enough to clip
    # the title bar out of the recording. Two identical consecutive readings
    # mean the layout has settled.
    local frame_x frame_y frame_width frame_height
    local last_width="" last_height="" stable=0
    local frame_deadline=$((SECONDS + 20))
    while ((SECONDS < frame_deadline)); do
        info=$(frame_geometry "$id")
        frame_x=$(sed -n 's/^ *Absolute upper-left X: *//p' <<<"$info")
        frame_y=$(sed -n 's/^ *Absolute upper-left Y: *//p' <<<"$info")
        frame_width=$(sed -n 's/^ *Width: *//p' <<<"$info")
        frame_height=$(sed -n 's/^ *Height: *//p' <<<"$info")
        case "$frame_width:$frame_height" in
        "$last_width:$last_height") ;;
        *) stable=0; last_width="$frame_width"; last_height="$frame_height" ;;
        esac
        if ((frame_width >= WINDOW_WIDTH - 8 && frame_width <= WINDOW_WIDTH + 8)) \
            && ((frame_height >= WINDOW_HEIGHT && frame_height <= WINDOW_HEIGHT + 60)); then
            stable=$((stable + 1))
            ((stable >= 2)) && break
        else
            stable=0
        fi
        sleep 0.3
    done
    note "frame ${frame_width}x${frame_height}+${frame_x}+${frame_y}"
    if ((stable < 2)); then
        xwininfo -root -tree >&2 || true
        fail "the window frame settled at ${frame_width}x${frame_height}, not the ${WINDOW_WIDTH}x${WINDOW_HEIGHT} window seeded in the settings"
    fi

    # The X pointer defaults to the middle of the screen, which is inside the
    # text area: an I-beam would be frozen into every frame. Park it outside
    # the frame and never move it again.
    xdotool mousemove 1270 880
    xdotool windowactivate --sync "$id" 2>/dev/null || true
    sleep 0.5

    # The typing rate is derived from the target length: characters are counted
    # as characters, not bytes, which is why the locale is pinned (a Turkish
    # paragraph is mostly multi-byte, and byte counting would halve the rate).
    local characters delay total
    characters=$(printf '%s' "${SAMPLE_TEXT[$language]}" | LC_ALL=C.UTF-8 wc -m)
    characters=$((characters))
    delay=$(LC_ALL=C awk -v target="$TARGET_SECONDS" -v pre="$PRE_ROLL_SECONDS" \
        -v post="$POST_ROLL_SECONDS" -v chars="$characters" \
        'BEGIN { d = (target - pre - post) * 1000 / chars; if (d < 1) d = 1; printf "%d", d }')
    total=$(LC_ALL=C awk -v pre="$PRE_ROLL_SECONDS" -v post="$POST_ROLL_SECONDS" \
        -v chars="$characters" -v delay="$delay" \
        'BEGIN { printf "%.2f", pre + post + (chars * delay / 1000) }')
    note "typing $characters characters at ${delay} ms each, recording ${total} s"

    ffmpeg -loglevel error -y \
        -f x11grab -framerate "$FPS" \
        -video_size "${frame_width}x${frame_height}" \
        -i ":$DISPLAY_NUMBER+$frame_x,$frame_y" \
        -t "$total" "$raw" &
    local recorder=$!

    sleep "$PRE_ROLL_SECONDS"
    # --delay keeps the typing visible frame by frame; without it xdotool types
    # the paragraph between two captured frames.
    xdotool type --delay "$delay" "${SAMPLE_TEXT[$language]}"
    wait "$recorder" || fail "ffmpeg failed to record the $language capture"

    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
    APP_PID=""

    mkdir -p "$OUT_DIR"
    # Two passes: a palette measured from the recording, then applied to it.
    # A single-pass GIF encoder quantises per frame and the flat theme colours
    # flicker between frames; 128 colours are enough for one theme and keep the
    # file inside the budget.
    ffmpeg -loglevel error -y -i "$raw" \
        -vf "fps=$FPS,scale=$OUTPUT_WIDTH:-2:flags=lanczos,palettegen=stats_mode=diff:max_colors=128" \
        "$WORK/palette-$language.png"
    ffmpeg -loglevel error -y -i "$raw" -i "$WORK/palette-$language.png" \
        -lavfi "fps=$FPS,scale=$OUTPUT_WIDTH:-2:flags=lanczos[x];[x][1:v]paletteuse=dither=sierra2_4a" \
        -loop 0 "$output"

    local bytes duration width
    bytes=$(stat -c %s "$output")
    duration=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$output")
    width=$(ffprobe -v error -select_streams v:0 -show_entries stream=width -of csv=p=0 "$output")
    printf '%s: %s bytes, %s s, %s wide\n' "$output" "$bytes" "$duration" "$width"
    # The length is part of the document contract, so it is checked instead of
    # assumed: a changed sample text or a slower machine must not silently
    # stretch the clip the README says lasts ten seconds.
    if ! LC_ALL=C awk -v actual="$duration" -v target="$TARGET_SECONDS" \
        'BEGIN { exit (actual > target - 0.5 && actual < target + 0.5) ? 0 : 1 }'; then
        fail "$output lasts ${duration}s, not the ${TARGET_SECONDS}s it should"
    fi
    if ((bytes > MAX_BYTES)); then
        fail "$output is $bytes bytes, over the $MAX_BYTES byte budget - shorten the sample text or lower OUTPUT_WIDTH"
    fi
}

DISPLAY_NUMBER=$(pick_display)
export DISPLAY=":$DISPLAY_NUMBER"
Xvfb ":$DISPLAY_NUMBER" -screen 0 "$SCREEN" >"$WORK/xvfb.log" 2>&1 &
XVFB_PID=$!
for _ in $(seq 1 50); do
    xwininfo -root -tree >/dev/null 2>&1 && break
    sleep 0.2
done
xwininfo -root -tree >/dev/null 2>&1 \
    || fail "Xvfb on :$DISPLAY_NUMBER never became reachable (see $WORK/xvfb.log)"
note "Xvfb on :$DISPLAY_NUMBER ($SCREEN)"

xfwm4 >"$WORK/wm.log" 2>&1 &
WM_PID=$!
sleep 1

# The window geometry in the settings file is a frame size; a manager that
# ignores it would silently record a different crop, so verify once.
for language in "${WANT[@]}"; do
    capture "$language"
done

note "done"
