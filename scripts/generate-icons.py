#!/usr/bin/env python3
"""Regenerate the bundled application icons from the canonical SVG.

The artwork has exactly one source, ``src/main/resources/icons/jtypewriter.svg``.
This script renders the PNG sizes, the Windows ``.ico`` and the macOS ``.icns``
from it so the committed binaries can always be traced back to that file.

Usage:
    scripts/generate-icons.py               # regenerate everything
    scripts/generate-icons.py --check       # fail if the assets are stale

Requires ImageMagick (``magick``) for rasterising the SVG; everything else is
plain Python.

The SVG deliberately avoids gradients and filters because ImageMagick's internal
MSVG renderer paints gradient fills as solid black. Render with
``-background none`` so the corners stay transparent (a plain ``magick in.svg
out.png`` floods them with black).
"""

from __future__ import annotations

import argparse
import io
import struct
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ICON_DIR = ROOT / "src" / "main" / "resources" / "icons"
SVG = ICON_DIR / "jtypewriter.svg"

# PNG sizes kept in the repository. 16/24/32 feed JavaFX and Windows trays,
# 64..512 the launchers and package icons, 1024 the macOS @2x chunk.
PNG_SIZES = (16, 24, 32, 48, 64, 128, 256, 512, 1024)

# ICO entries: (size, chunk id). Modern Windows (and jpackage, which needs 10+)
# reads PNG-compressed entries at any size, so the .ico stays small.
ICO_SIZES = (16, 24, 32, 48, 64, 128, 256)

# ICNS chunk ids -> pixel size. Apple's PNG-based chunk types.
ICNS_CHUNKS = (
    ("ic11", 32),    # 16x16 @2x
    ("ic12", 64),    # 32x32 @2x
    ("ic07", 128),   # 128x128
    ("ic13", 256),   # 128x128 @2x
    ("ic08", 256),   # 256x256
    ("ic14", 512),   # 256x256 @2x
    ("ic09", 512),   # 512x512
    ("ic10", 1024),  # 512x512 @2x
)

RENDER_DENSITY = "384"


def run(command: list[str]) -> None:
    subprocess.run(command, check=True)


def render_base(workdir: Path) -> Path:
    """Rasterise the SVG once at 1024 px, then downscale from that master.

    Rendering every size from the SVG directly would pay the (slow) MSVG parse
    cost seven times and give noticeably different stroke weights per size.
    """
    base = workdir / "base-1024.png"
    run([
        "magick",
        "-background", "none",
        "-density", RENDER_DENSITY,
        str(SVG),
        "-resize", "1024x1024",
        "-strip",
        "PNG32:" + str(base),
    ])
    if not base.is_file():
        sys.exit("ImageMagick did not produce " + str(base))
    return base


def render_png(base: Path, size: int, target: Path) -> None:
    run([
        "magick", str(base),
        "-filter", "Lanczos",
        "-resize", f"{size}x{size}",
        "-strip",
        "PNG32:" + str(target),
    ])


def build_ico(pngs: dict[int, bytes]) -> bytes:
    """PNG-compressed .ico (Vista+); every entry keeps its own alpha."""
    count = len(ICO_SIZES)
    header = struct.pack("<HHH", 0, 1, count)  # reserved, type=icon, count
    entries = bytearray()
    payload = bytearray()
    offset = len(header) + 16 * count
    for size in ICO_SIZES:
        data = pngs[size]
        entries += struct.pack(
            "<BBBBHHII",
            size if size < 256 else 0,  # 256 is encoded as 0
            size if size < 256 else 0,
            0,  # palette size
            0,  # reserved
            1,  # colour planes
            32,  # bits per pixel
            len(data),
            offset + len(payload),
        )
        payload += data
    return bytes(header + entries + payload)


def build_icns(pngs: dict[int, bytes]) -> bytes:
    """PNG-based .icns: each chunk is a big-endian (type, length, data) tuple."""
    body = bytearray()
    for chunk_id, size in ICNS_CHUNKS:
        data = pngs[size]
        body += chunk_id.encode("ascii")
        body += struct.pack(">I", 8 + len(data))
        body += data
    return b"icns" + struct.pack(">I", 8 + len(body)) + bytes(body)


def generate() -> dict[Path, bytes]:
    """Return {path: bytes} for every asset, without writing anything."""
    outputs: dict[Path, bytes] = {}
    with tempfile.TemporaryDirectory(prefix="jtw-icons-") as tmp:
        workdir = Path(tmp)
        base = render_base(workdir)
        pngs: dict[int, bytes] = {}
        for size in PNG_SIZES:
            target = workdir / f"jtypewriter-{size}.png"
            render_png(base, size, target)
            data = target.read_bytes()
            pngs[size] = data
            outputs[ICON_DIR / target.name] = data
    outputs[ICON_DIR / "jtypewriter.ico"] = build_ico(pngs)
    outputs[ICON_DIR / "jtypewriter.icns"] = build_icns(pngs)
    return outputs


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="verify the committed assets match the SVG instead of rewriting them",
    )
    args = parser.parse_args()

    if not SVG.is_file():
        sys.exit(f"canonical artwork missing: {SVG}")
    if subprocess.run(["magick", "-version"], capture_output=True).returncode != 0:
        sys.exit("ImageMagick ('magick') is required to rasterise the SVG")

    outputs = generate()

    if args.check:
        stale = [
            path.name for path, data in outputs.items()
            if not path.is_file() or path.read_bytes() != data
        ]
        if stale:
            print("icon assets are stale, run scripts/generate-icons.py:", file=sys.stderr)
            for name in stale:
                print("  " + name, file=sys.stderr)
            return 1
        print(f"icon assets are up to date ({len(outputs)} files)")
        return 0

    for path, data in outputs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        with io.open(path, "wb") as handle:
            handle.write(data)
        print(f"wrote {path.relative_to(ROOT)} ({len(data)} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
