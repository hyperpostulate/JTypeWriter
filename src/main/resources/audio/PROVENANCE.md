# Provenance and licence of keystroke.wav

This statement covers `audio/keystroke.wav`, the keystroke sound played by
`TypewriterSound`. It ships inside the jar next to the asset, so the licence
travels with the bytes it describes.

## Author and licence

Copyright (c) 2026 Mesut ORMANLI.

The sample is original work created by the author by hand in Audacity. It
contains no third-party sample, recording or library source, and it is not
derived from any sample pack.

Distributed under the Apache License, Version 2.0 (Apache-2.0; see LICENSE in
the repository root); no separate attribution is required beyond that
licence's terms.

## Identity of the shipped file

- File: `keystroke.wav`
- Size: 71,204 bytes
- Format: PCM, 16-bit, stereo, 48,000 Hz
- Frames: 17,790
- Duration: 0.370625 s
- SHA-256: e4ed1ba70dc0f8b94b0edc79ed097f3285ab9812ae94a7ee0603f2a652c85a13

The hash lets a later reader confirm that the shipped bytes are the ones this
statement describes. Any edit to the sample must update this file, and the
`AudioAssetTest` suite fails when the two drift apart.

The WAV carries no embedded metadata (it has no LIST/INFO chunk), which is
why the provenance lives in this sidecar rather than inside the file.
