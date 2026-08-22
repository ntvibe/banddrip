# Android companion testing without a Band

The Android debug APK is useful before a physical Xiaomi Smart Band 10 is available. It acts as a hardware-free development console for the full source → normalization → safety → protocol path.

## What this build can prove

The current Android build can exercise:

```text
Mock / Nightscout
       ↓
GlucoseSource
       ↓
BandDripReading
       ↓
ReadingValidator
       ↓
Protocol v1
       ↓
VirtualBandTransport
       ↓
Band-style preview
```

It does **not** prove Xiaomi `system.interconnect` or Mi Fitness interoperability.

## Install

Use the `banddrip-android-debug-apk` artifact from a successful **Android build** GitHub Actions run.

The artifact contains `app-debug.apk`.

Because this is a development APK outside Google Play, Android may ask for permission to install apps from the browser/file manager you use to open it.

## Mock test

1. Open BandDrip.
2. Leave **Mock** selected.
3. Keep **Show IOB** enabled.
4. Tap **Generate next mock reading**.
5. Confirm the virtual band shows glucose, trend, delta, exact age and `IOB 0.250 U`.
6. Generate several readings and confirm delta/trend change.
7. Toggle IOB off/on and confirm only the IOB row changes.
8. Tap **Inject 12m stale state** and confirm glucose turns red + struck through while `12m ago` remains visible.

## Nightscout test

1. Select **Nightscout**.
2. Enter the base HTTPS URL of a Nightscout-compatible server.
3. Enter an access token only when the server requires one.
4. Tap **Fetch Nightscout now**.
5. Confirm the preview displays the newest glucose reading.
6. Compare glucose, trend and timestamp with the source system.
7. Confirm delta equals the difference between the newest and immediately previous valid glucose entries.
8. If recent IOB exists in device-status data, compare it with the source. If BandDrip cannot verify recent IOB, it must show `IOB —` rather than guessing.

The current development build keeps the URL/token in UI state only. The token is intentionally not persisted yet.

## Important failure tests

- Temporarily use an invalid URL: the previous reading may remain visible only with its timestamp continuing to age; no fabricated replacement reading should appear.
- Wait/cross the 10-minute boundary: the preview must transition to stale automatically without requiring a new packet.
- Use a source with unavailable IOB: the glucose may remain valid while IOB shows `—`.

## Reporting a test result

When reporting an issue, include:

- BandDrip commit SHA / Actions run
- Android version and phone model
- whether the source was Mock or Nightscout
- the visible BandDrip status/error text
- a screenshot when the issue is visual

Do **not** post Nightscout tokens, private URLs, glucose exports, or personally identifying health data in a public GitHub issue.
