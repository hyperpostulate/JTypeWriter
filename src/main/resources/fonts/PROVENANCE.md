# Provenance and licence of the bundled UI font

This statement covers the two font files in this directory, the UI chrome face
the application renders and measures with. They ship inside the jar next to this
file, so the provenance travels with the bytes it describes.

## What ships

| File | Size | SHA-256 |
| --- | --- | --- |
| `DejaVuSansCondensed.ttf` | 682,828 bytes | `24111144e604b4328c34f1e7f9a6bee725963bdbafceaf644a348e95037add26` |
| `DejaVuSansCondensed-Bold.ttf` | 667,844 bytes | `5a3bb1f72d8861c272498b80f1fe075163b4ea37bb27dd9cdd7b985e59b0286e` |

Both files are unmodified. Family name: `DejaVu Sans Condensed`; release 2.37.

## Author, source and licence

DejaVu fonts: <https://dejavu-fonts.github.io/> (release 2.37).

Copyright (c) 2003 by Bitstream, Inc. All Rights Reserved.
Bitstream Vera is a trademark of Bitstream, Inc.
DejaVu changes are in public domain.

Distributed under the Bitstream Vera licence (SPDX: `Bitstream-Vera`). The full
text is in `legal/Bitstream-Vera.txt` at the repository root and in
`legal/Bitstream-Vera.txt` next to the launcher in the application image.

## Why the font is bundled

The toolbar decides between its full and compact captions by measuring the
labels in AWT before it renders them. A system `SansSerif` alias does not
measure the same on every host: the same build produced 817 px for the French
compact row on one CI runner and 851 px on another, because each runner resolved
the alias to a different file. Bundling the face makes that number a property of
the release. `AppFonts` loads these files for the measurement, `FxApplication`
registers them with JavaFX, and `.root` in `base.css` names the family, so the
rendered text and the measured text come from the same bytes.

The Condensed cut is deliberate: it is narrow enough that the collapsed row
still fits a 900 px window in Spanish and French, the two longest locales. It
also carries U+2630 (the toolbar's menu glyph), which the proportional cuts of
Noto Sans and Liberation Sans do not, and it covers every character the six UI
languages use.

## Invariants

- `AppFontsTest` fails if a file is missing, unreadable, reports a different
  family, cannot render one of the UI labels, or measures a pinned string to a
  different width than the checked-in file.
- A hash change means the file changed: update this table and re-run the tests.
