# 🩸 BandDrip

Open-source glucose display for Xiaomi Smart Band, built with Xiaomi Vela.

> **Status:** virtual MVP in active development. Android and Vela builds run in CI, Nightscout is implemented as the first real glucose source, and the Band 10 UI/safety logic can be exercised without hardware. Installation and phone↔band transport on a physical global Xiaomi Smart Band 10 still require validation.

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
- At **10+ minutes**, glucose becomes **red with a native strike-through** while its exact age remains visible.
- IOB has independent freshness; stale IOB renders as `IOB —` even when glucose is fresh.
- Missing or invalid data is never invented or silently presented as current.

## What works today

### Android companion

- deterministic mock glucose source
- Nightscout source (`latest 2 SGV entries` + optional recent device-status IOB)
- delta computed locally as `current - previous valid glucose`
- Nightscout trend normalization
- HTTPS-only Nightscout transport with optional access token
- normalized protocol-v1 packets
- source-independent `GlucoseSource` interface
- source→validation→transport engine
- virtual Band transport
- interactive Band-style preview
- IOB toggle, enabled by default
- one-tap 12-minute stale-state injection for testing
- reading validation before transport
- unit tests for freshness, Nightscout normalization, protocol and fail-closed behavior

### Xiaomi Vela app

- 212×520 Band 10-oriented layout
- glucose + trend / delta + freshness / IOB hierarchy
- 30-second age recalculation even without a new glucose packet
- exact 10-minute stale boundary
- red + native text strike-through for stale glucose
- independent IOB freshness
- protocol-version guard
- second independent reading-validation gate before rendering
- unavailable state (`—`) instead of fake fallback data
- opt-in emulator DEMO mode with a visible marker
- emulator fixtures for fresh, stale, 350 mg/dL, mmol/L, stale IOB and missing-delta states
- release guard that refuses to package while DEMO mode is enabled

### CI

GitHub Actions builds both halves independently:

- Android unit tests + debug APK
- Vela build + debug RPK

Successful runs upload installable build artifacts for further testing.

## Architecture

```text
Nightscout / future xDrip+ / Juggluco adapters
                         |
                         v
                 Android companion
             normalize + validate data
                         |
                         v
                 BandDrip protocol v1
                         |
                         v
            BandTransport abstraction
               |                  |
               v                  v
       Virtual transport     Xiaomi transport
          (working)          (hardware TBD)
                                  |
                                  v
                        system.interconnect
                                  |
                                  v
                         BandDrip Vela app
                         validate + render
```

Source-specific logic stays on Android. The band receives a small normalized message and does not need CGM credentials or direct internet access.

Repository layout:

```text
apps/
  android/        Android companion + virtual test bench
  band/           Xiaomi Vela wearable app
packages/
  protocol/       Source-independent JSON contracts
docs/
  ARCHITECTURE.md
  EMULATOR.md
  NIGHTSCOUT.md
  SAFETY.md
  TEST_MATRIX.md
```

## Project principles

1. **Freshness is as important as the glucose value.**
2. Never silently present stale data as current.
3. Delta means current glucose minus the immediately previous valid glucose reading.
4. IOB has its own freshness and must never inherit glucose freshness.
5. Keep the normal band display uncluttered.
6. Keep secrets, CGM URLs, tokens and credentials off the wearable and out of the repository.
7. Treat Nightscout, xDrip+, Juggluco and future inputs as interchangeable source adapters.
8. Prefer official Xiaomi Vela APIs and stock firmware where possible.
9. Build public-first: documented interfaces, small components and contributor-friendly boundaries.
10. Fail closed when data or protocol validation fails.

## Development without a Band 10

A physical band is **not** required for most development.

The Android app can already exercise:

```text
Mock / Nightscout
       ↓
 normalization
       ↓
 safety validation
       ↓
 protocol v1
       ↓
 virtual transport
       ↓
 Band-style preview
```

The Vela app can independently build and run against deterministic emulator fixtures. See [`docs/EMULATOR.md`](docs/EMULATOR.md) and [`docs/TEST_MATRIX.md`](docs/TEST_MATRIX.md).

## Still hardware-dependent

These claims remain intentionally unverified until a physical **global Xiaomi Smart Band 10** is available:

- installing BandDrip on stock global Band 10 firmware
- final Vela/native package and signing identity requirements
- coexistence with normal Mi Fitness pairing and features
- Android→Mi Fitness/Band `system.interconnect` delivery
- reconnect/background reliability
- real OLED readability and wrist ergonomics
- battery impact

The Xiaomi-specific Android transport stays isolated behind `BandTransport` so these unknowns do not contaminate source parsing or UI logic.

## Safety notice

BandDrip is an unofficial secondary glucose display and is not a medical device. Do not use it as the sole basis for treatment decisions. Data may be delayed, unavailable, or incorrect. Use at your own risk and verify important readings in your approved CGM/pump system.

See [`docs/SAFETY.md`](docs/SAFETY.md) for the display-freshness rules.

## Contributing

Contributions and technical research are welcome. See [`CONTRIBUTING.md`](CONTRIBUTING.md).

BandDrip is an independent community project and is not affiliated with or endorsed by Xiaomi or any CGM/pump manufacturer.
