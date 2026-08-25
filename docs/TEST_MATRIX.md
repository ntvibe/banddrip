# BandDrip test matrix

This document separates behavior that can be validated before hardware arrives from behavior that requires a real Xiaomi Smart Band 10.

## Hardware-free: required before merge

### Data and safety

- fresh at 9 minutes
- stale at exactly 10 minutes
- stale value remains visible only as red + strike-through
- exact age remains visible in stale state
- invalid/missing glucose timestamp fails closed
- excessive future clock skew fails closed
- delta equals current minus immediately previous valid reading
- missing previous reading renders `Δ —`
- IOB requires its own timestamp
- stale IOB renders `IOB —`
- malformed packets are rejected on Android and wearable sides
- incompatible protocol versions are rejected

### Display scenarios

- 2-digit glucose
- 3-digit glucose
- large 3-digit glucose such as 350
- all trend arrows
- positive, negative, zero and unavailable delta
- 1m, 9m, 10m, 12m, 99m and 100m+ age text
- IOB `0.000 U`, `0.250 U`, multi-unit values and unavailable IOB
- IOB enabled and disabled
- mg/dL and mmol/L formatting
- unavailable reading (`—`)

### Source / transport

- deterministic mock source
- Nightscout latest-two-entry normalization
- Nightscout optional IOB parsing
- source error does not generate a glucose packet
- virtual transport produces protocol v1 packets

### Build

- Android unit tests pass
- Android debug APK builds
- Vela debug RPK builds
- committed/release Vela builds cannot contain enabled DEMO mode

## Xiaomi emulator / AIoT-IDE

- render on `xiaomi10Band` / 212×520 target
- check capsule-edge clipping
- visually compare fresh and stale states
- verify all arrows fit
- verify 3-digit glucose and long age values fit
- verify strikethrough crosses the glucose value rather than trend arrow
- verify IOB-off layout does not leave awkward spacing

## Requires physical Smart Band 10

- installation of BandDrip RPK on global firmware
- final Android/Vela package and signing identity
- Mi Fitness coexistence
- `system.interconnect` phone-to-band packet delivery
- reconnect behavior after Bluetooth loss / app process death
- background reliability with Android battery restrictions
- real OLED readability and wrist ergonomics
- battery impact
- production polling cadence
- vibration/notification behavior if added later

Hardware-dependent behavior must remain explicitly marked unverified until tested on a physical global Smart Band 10.
