# 🩸 BandDrip

Open-source glucose display for Xiaomi Smart Band, built with Xiaomi Vela.

> **Status:** early development scaffold. The first hardware target is the global Xiaomi Smart Band 10. Real-device installation and Xiaomi interconnect details still need validation on physical hardware.

BandDrip aims to provide a clean, glanceable secondary glucose display while preserving stock Xiaomi firmware and the normal Mi Fitness experience.

## Default band screen

```text
       112 ↘

     +6 · 3m ago

     IOB 0.250 U
```

The wearable UI intentionally stays minimal:

- **Glucose + trend arrow** are the dominant top row.
- **Delta** versus the immediately previous valid glucose reading is always visible when available.
- **Exact freshness** (`Xm ago`) is always visible.
- **IOB** is shown by default and can be disabled from the Android companion.
- At **10+ minutes**, the glucose value becomes **red with a strike-through** while the exact age remains visible.
- Missing or invalid timestamps are never presented as fresh data.

## Architecture

```text
Nightscout / xDrip+ / Juggluco / future sources
                         |
                         v
                 Android companion
             normalize + validate data
                         |
                         v
             Xiaomi system.interconnect
                         |
                         v
                 BandDrip Vela app
```

Source-specific logic stays on Android. The band receives a small normalized message and does not need CGM credentials or direct internet access.

Repository layout:

```text
apps/
  android/        Android companion
  band/           Xiaomi Vela wearable app
packages/
  protocol/       Source-independent reading contract
docs/
  ARCHITECTURE.md
  SAFETY.md
```

## Project principles

1. **Freshness is as important as the glucose value.**
2. Never silently present stale data as current.
3. Keep the band display uncluttered.
4. Keep secrets, CGM URLs, tokens, and credentials off the wearable and out of the repository.
5. Treat Nightscout, xDrip+, Juggluco, and future inputs as interchangeable source adapters.
6. Prefer official Xiaomi Vela APIs and stock firmware where possible.
7. Build public-first: documented interfaces, small components, and contributor-friendly boundaries.

## Development

### Xiaomi Vela app

The wearable scaffold follows Xiaomi's Vela JS application structure under `apps/band/src/` and declares `system.interconnect` for paired-phone communication.

The current package/signing identity is provisional. Xiaomi's interconnect requirements will be finalized after validation against a global Smart Band 10 and the current Mi Fitness developer installation path.

### Android companion

The Android app is a Jetpack Compose scaffold using the current Android toolchain. It currently provides the settings/safety shell and normalized data model. Source adapters and Xiaomi transport integration are the next implementation steps.

## Safety notice

BandDrip is an unofficial secondary glucose display and is not a medical device. Do not use it as the sole basis for treatment decisions. Data may be delayed, unavailable, or incorrect. Use at your own risk and verify important readings in your approved CGM/pump system.

See [`docs/SAFETY.md`](docs/SAFETY.md) for the display-freshness rules.

## Contributing

Contributions and technical research are welcome. See [`CONTRIBUTING.md`](CONTRIBUTING.md).

BandDrip is an independent community project and is not affiliated with or endorsed by Xiaomi or any CGM/pump manufacturer.
