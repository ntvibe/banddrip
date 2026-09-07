# BandDrip Lab architecture

BandDrip uses three evidence levels instead of conflating a browser mock with real firmware behavior.

## 1. Browser simulator — fast

`apps/lab/` is a dependency-free static application published through GitHub Pages. It reads profiles from `packages/devices/` and lets contributors exercise glucose, trend, delta, exact age, IOB, units and the 10-minute stale transition.

Initial profiles:

- Smart Band 10 — primary BandDrip target, 212×520
- Smart Band 10 Pro — experimental watch-face target, 336×480
- Smart Band 9 Pro — emulator research, 336×480
- Smart Band 8 Pro — emulator research, 336×480

A profile describes geometry and research capability. It does **not** claim that BandDrip transport works on that hardware.

## 2. Xiaomi Vela emulator — heavy

`.github/workflows/vela-emulator-lab.yml` is intentionally manual. Xiaomi's emulator environment is large and QEMU/GUI-derived, so downloading/booting it on every PR would waste CI time.

The current known full-emulator skin is `xiaomi_band_pro` with a 336×480 geometry associated with Band 8 Pro / Band 9 Pro research. This workflow should be treated as experimental until it successfully boots on GitHub-hosted runners.

The regular Band 10 remains primarily covered by Xiaomi's Vela build tooling + its Band 10 screen profile and our browser/Android previews until a suitable full-system Band 10 image/skin is verified.

## 3. Physical hardware — final authority

Only a real band can prove:

- Mi Fitness installation and pairing behavior
- Android↔band transport and reconnect behavior
- watch-face/RPK shared-state experiments
- OLED readability
- raise-to-wake experience
- power consumption
- NFC/payment coexistence

## GitHub Pages

The Pages workflow builds a static artifact on pull requests and deploys from `main`. Repository Pages must be configured to use **GitHub Actions** as the source before the first deployment can succeed.
