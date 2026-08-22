# BandDrip architecture

BandDrip is split into three intentionally small layers so data sources, Android transport, and wearable UI can evolve independently.

```text
Glucose source
  Nightscout / xDrip+ / Juggluco / future adapters
        |
        v
Android companion
  normalize -> validate -> freshness policy -> interconnect transport
        |
        v
Xiaomi Vela app
  render glucose + trend + delta + exact age + optional IOB
```

## Design goals

- Keep the Xiaomi Smart Band UI extremely simple and glanceable.
- Treat freshness as first-class data, never decoration.
- Never present a stale glucose value as current.
- Keep source-specific logic out of the band app.
- Keep all credentials and private CGM URLs on the phone.
- Make the protocol generic enough for future wearable targets.
- Preserve stock Xiaomi firmware and Mi Fitness behavior.

## Components

### `apps/band`

Xiaomi Vela JS app for Smart Band 10. It receives normalized readings from the Android counterpart through `system.interconnect` and derives display freshness locally from the reading timestamp.

### `apps/android`

Android companion. The first implementation will provide a mock source and Nightscout adapter, normalize readings, expose user settings such as IOB visibility, and forward compact messages to the band.

### `packages/protocol`

Source-independent message contract shared conceptually by both apps. The wearable must not need to understand Nightscout, xDrip+, Juggluco, pump APIs, or authentication.

## Package/signing note

Xiaomi's Vela interconnect documentation requires coordination between the wearable package identity/signature and the Android counterpart. The exact production identity and signing workflow will be finalized after real-device validation on the global Smart Band 10. Signing keys are never stored in this repository.

## MVP data flow

1. Android source adapter obtains the newest glucose reading and the immediately previous valid reading.
2. Android normalizes the reading into the BandDrip protocol.
3. Delta is `current glucose - immediately previous valid glucose`.
4. Android forwards the normalized reading to the Vela app.
5. The band stores the latest message timestamp in memory and continuously derives `Xm ago` from the glucose measurement timestamp.
6. At 10 minutes or older, the band renders the glucose value red with a strike-through while retaining the exact age.
7. IOB is shown by default and can be disabled from the Android app.

## Safety behavior is part of the protocol

A renderer must never infer that a glucose value is current simply because communication is connected. Freshness is based on the measurement timestamp. Missing or invalid timestamps are treated as stale/unavailable.
