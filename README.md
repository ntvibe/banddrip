# 🩸 BandDrip

Open-source glucose display for Xiaomi Smart Band.

> **Status:** virtual MVP in active development. Android, Vela and the browser Lab build in CI. xDrip and Nightscout source paths are implemented. Physical Xiaomi Smart Band 10 installation, live `system.interconnect` transport and the zero-tap watch-face bridge still require real-device validation.

BandDrip is designed around one interaction goal:

> **raise wrist → see current glucose immediately**

The project aims to preserve stock Xiaomi firmware and the normal Mi Fitness experience wherever possible.

## Default glance screen

```text
       112 ↘

     +6 · 3m ago

     IOB 0.250 U
```

Core display contract:

- glucose + trend are visually dominant
- delta versus the immediately previous valid reading is non-negotiable
- exact freshness (`Xm ago`) is always visible
- IOB is shown by default and can be disabled in Android
- at **10+ minutes**, glucose turns **red + struck through** while exact age remains visible
- IOB has independent freshness and must not silently remain current
- missing/invalid data fails closed instead of inventing a reading

## Android companion

BandDrip Android acts as the source, settings, reliability and installation console.

### Sources

- **Mock** — editable test values + deterministic edge-case cycle
- **Nightscout** — accepts base URL + token or a full tracking URL containing `?token=...`
- **xDrip local server** — Nightscout-compatible local `/sgv.json` source
- **xDrip broadcast** — receives xDrip glucose broadcasts directly

Delta is computed from consecutive valid glucose readings rather than fabricated when unavailable.

### Reliability

- persistent foreground relay service
- `START_STICKY`
- boot/app-update restart when enabled
- battery-optimization exemption flow
- Xiaomi/HyperOS autostart and battery-management shortcuts
- notification permission handling

BandDrip does **not** request Accessibility Service merely to stay alive; screen-control privilege is unrelated to the relay job.

### All-in-one package direction

The Android CI pipeline builds a single APK containing:

```text
BandDrip APK
├── Android companion
├── matching BandDrip RPK
└── Mi Fitness installer helper
```

The APK/RPK signing identities are checked in CI. Installation automation remains experimental until verified with stock Global Mi Fitness + a physical band.

## Xiaomi Vela wearable

The Smart Band 10 RPK implements:

- 212×520 Band 10 layout
- glucose + trend / delta + exact age / IOB hierarchy
- independent band-side age recalculation
- exact 10-minute stale boundary
- red + native line-through stale state
- independent IOB freshness
- protocol-version guard
- defensive packet validation
- unavailable state (`—`) instead of fake fallback glucose
- deterministic emulator fixtures
- wearable regression tests

## BandDrip Lab

`apps/lab/` is a dependency-free multi-device browser simulator prepared for GitHub Pages.

Initial profiles:

- **Smart Band 10** — primary BandDrip target, 212×520
- **Smart Band 10 Pro** — experimental watch-face target, 336×480
- **Smart Band 9 Pro** — Vela-emulator research, 336×480
- **Smart Band 8 Pro** — Vela-emulator research, 336×480

The Lab lets contributors change glucose, units, trend, delta, exact age and IOB, and instantly exercise fresh/stale/high/mmol/missing-delta cases.

Device capability metadata lives in `packages/devices/` so adding another Xiaomi band does not require hard-coding another UI.

See [`docs/LAB.md`](docs/LAB.md).

## Three evidence levels

BandDrip deliberately does not call every preview an emulator.

### 1. Browser simulator

Fast layout and safety-state iteration. Useful, but not firmware evidence.

### 2. Xiaomi Vela emulator

A separate heavy GitHub Action downloads Xiaomi's Vela emulator environment, creates a virtual band using the known `xiaomi_band_pro` skin, boots it under Xvfb/KVM, attempts ADB control and captures evidence artifacts.

This workflow is manual-only because the emulator environment is large and should not run on every commit.

### 3. Physical hardware

The final authority for:

- stock Mi Fitness installation
- Android↔band transport
- reconnect/background behavior
- zero-tap watch-face shared-state experiment
- OLED readability and raise-to-wake UX
- battery consumption
- NFC/payment coexistence

## Architecture

```text
Mock / Nightscout / xDrip
            ↓
     Android companion
   normalize + validate
            ↓
   BandDrip protocol v1
            ↓
     BandTransport
      ↙          ↘
 virtual       Xiaomi
 transport   interconnect
                  ↓
            BandDrip RPK
                  ↓
       experimental glance state
                  ↓
        BandDrip watch face
```

Source-specific credentials stay on Android. The wearable receives normalized data and must never need CGM credentials or direct internet access.

Repository layout:

```text
apps/
  android/          Android companion + virtual test bench
  band/             Xiaomi Vela wearable app
  lab/              multi-device browser simulator
packages/
  protocol/         source-independent JSON contract
  display-spec/     Band 10 rendering contract
  devices/          Xiaomi device geometry/capability profiles
tools/
  lab/              device validation
  vela-emulator/    CI virtual-device helpers
docs/
  LAB.md
  ARCHITECTURE.md
  EMULATOR.md
  NIGHTSCOUT.md
  SAFETY.md
  TEST_MATRIX.md
```

## Project principles

1. **Freshness is as important as the glucose value.**
2. Never silently present stale data as current.
3. Delta means current minus the immediately previous valid glucose reading.
4. IOB has its own freshness.
5. Keep the wrist display glanceable and uncluttered.
6. Keep secrets, private CGM URLs, tokens and credentials off the wearable and out of the repository.
7. Treat Nightscout, xDrip and future sources as interchangeable adapters.
8. Prefer official Xiaomi APIs and stock firmware where practical.
9. Separate browser simulation, firmware emulation and hardware evidence.
10. Fail closed when source data or protocol validation fails.

## Safety notice

BandDrip is an unofficial secondary glucose display and is not a medical device. Do not use it as the sole basis for treatment decisions. Data may be delayed, unavailable or incorrect. Verify important readings in the approved CGM/medical-device system.

See [`docs/SAFETY.md`](docs/SAFETY.md).

## Contributing

Contributions and technical research are welcome. See [`CONTRIBUTING.md`](CONTRIBUTING.md).

BandDrip is an independent community project and is not affiliated with or endorsed by Xiaomi or any CGM/pump manufacturer.
