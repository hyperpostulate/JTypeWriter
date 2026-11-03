#!/usr/bin/env python3
"""Regenerate the bundled application icons from the canonical SVG.

The artwork has exactly one source, ``src/main/resources/icons/jtypewriter.svg``.
This script renders the PNG sizes and the Windows ``.ico``
from it so the committed binaries can always be traced back to that file.

Usage:
    scripts/generate-icons.py               # regenerate everything
    scripts/generate-icons.py --check       # fail if the assets are stale

Requires ImageMagick for rasterising the SVG - the ImageMagick 7 ``magick``
driver, or the legacy ``convert`` of version 6 when that is what the machine
has; everything else is plain Python. Accepting both is what lets the same
command run on a developer machine and on a CI runner whose distribution
still packages ImageMagick 6.

The SVG deliberately avoids gradients and filters because ImageMagick's internal
MSVG renderer paints gradient fills as solid black. Render with
``-background none`` so the corners stay transparent (a plain ``magick in.svg
out.png`` floods them with black).
"""

from __future__ import annotations

import argparse
import io
import shutil
import struct
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ICON_DIR = ROOT / "src" / "main" / "resources" / "icons"
SVG = ICON_DIR / "jtypewriter.svg"

# PNG sizes kept in the repository. 16/24/32 feed JavaFX and Windows trays,
# 64..512 the launchers and package icons, 1024 the source for the downscales.
PNG_SIZES = (16, 24, 32, 48, 64, 128, 256, 512, 1024)

# ICO entries: (size, chunk id). Modern Windows (and jpackage, which needs 10+)
# reads PNG-compressed entries at any size, so the .ico stays small.
ICO_SIZES = (16, 24, 32, 48, 64, 128, 256)

RENDER_DENSITY = "384"


def magick() -> list[str]:
    """The ImageMagick command, as a prefix: ``magick`` (v7) or ``convert`` (v6).

    Every operation here is a single-image read-modify-write, so the two
    drivers are interchangeable for these flags and the choice is purely
    "which one this machine installed".
    """
    for candidate in (["magick"], ["convert"]):
        if shutil.which(candidate[0]):
            return candidate
    sys.exit("ImageMagick ('magick' or 'convert') is required to rasterise the SVG")


def run(command: list[str]) -> None:
    subprocess.run(command, check=True)


def render_base(workdir: Path) -> Path:
    """Rasterise the SVG once at 1024 px, then downscale from that master.

    Rendering every size from the SVG directly would pay the (slow) MSVG parse
    cost seven times and give noticeably different stroke weights per size.
    """
    base = workdir / "base-1024.png"
    run([
        *magick(),
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
        *magick(), str(base),
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
    if subprocess.run([*magick(), "-version"], capture_output=True).returncode != 0:
        sys.exit("ImageMagick is required to rasterise the SVG")

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
