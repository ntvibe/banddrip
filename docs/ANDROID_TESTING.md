# Android companion testing without a Band

The Android debug APK is useful before a physical Xiaomi Smart Band 10 is available. It acts as a hardware-free development console for the full source → normalization → safety → protocol path.

## What this build can prove

```text
Mock / Nightscout / xDrip local server / xDrip broadcast
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
       Smart Band 10 preview from shared display spec
```

It does **not** prove Xiaomi `system.interconnect` or Mi Fitness interoperability.

## Install

Use the `banddrip-android-debug-apk` artifact from a successful **Android build** GitHub Actions run. The artifact contains `app-debug.apk`.

Because this is a development APK outside Google Play, Android may ask for permission to install apps from the browser/file manager you use to open it.

## Mock test

1. Open BandDrip and select **Mock**.
2. Enter custom glucose, delta, age and IOB values or enable **Auto-cycle edge cases**.
3. Tap **Generate / test mock reading**.
4. Confirm the preview shows glucose + trend, delta + exact age, and IOB when enabled.
5. Test a 10+ minute age and confirm glucose is red + struck through while exact age remains visible.
6. Enable auto-cycle to exercise fresh, stale, high, mmol/L, stale-IOB and missing-delta states.

## Nightscout test

BandDrip accepts either of these forms:

```text
https://your-nightscout.example/
```

with a token entered separately, **or** a complete browser tracking link:

```text
https://your-nightscout.example/?token=track-...
```

When settings are saved, an embedded `?token=` is removed from the persisted URL and the token is stored encrypted using Android Keystore-backed AES-GCM ciphertext.

1. Select **Nightscout**.
2. Paste the full tracking URL or use base URL + separate token.
3. Tap **Save Nightscout settings**.
4. Tap **Test Nightscout connection**.
5. Require an explicit `Connected ✓ · <glucose> · Xm ago` result before enabling the background relay.
6. Compare glucose, trend, timestamp and delta with the source system.
7. If recent IOB exists in device-status data, compare it with the source. If BandDrip cannot verify recent IOB, it must show `IOB —` rather than guessing.

Nightscout requires HTTPS. The default background polling interval is 1 minute.

## xDrip local server test (recommended when xDrip is on the same phone)

xDrip+ provides a local web service in **Inter-App settings**. On the same phone it normally listens at:

```text
http://127.0.0.1:17580
```

Its `/sgv.json` endpoint emulates the Nightscout SGV endpoint.

1. In xDrip+, enable its **Local Web Service**.
2. In BandDrip select **xDrip → Local server**.
3. Keep the default URL `http://127.0.0.1:17580` when xDrip is running on the same phone.
4. If you configured an xDrip Web Service Secret, enter the same secret in BandDrip. BandDrip sends its SHA-1 value as xDrip's `api-secret` header.
5. Tap **Save xDrip settings**.
6. Tap **Test xDrip local server** and require a `Connected ✓` result.

Loopback access does not require Nightscout and does not require xDrip's Open Web Service option. xDrip's SGV endpoint supplies glucose data but may not provide IOB; BandDrip must show `IOB —` when no current IOB provider exists.

## xDrip broadcast test

BandDrip can also listen for xDrip's `com.eveningoutpost.dexdrip.BgEstimate` broadcasts.

1. In xDrip+, enable its **broadcast data** option in Inter-App settings.
2. In BandDrip select **xDrip → Broadcast**.
3. Wait for a new xDrip reading.
4. Tap **Check xDrip broadcast data**.
5. BandDrip computes delta from consecutive broadcasts rather than inventing a delta for the first received reading.

If a specific xDrip build does not deliver protected broadcasts to BandDrip, use xDrip's broadcast-without-permission option or use the local web-service mode instead.

## Always-on reliability

BandDrip uses Android mechanisms that are relevant to a long-running glucose relay:

- foreground service with persistent low-priority notification
- `START_STICKY` restart behavior
- restart after device boot / app update when background relay is enabled
- notification permission flow
- battery-optimization exemption flow
- Xiaomi/HyperOS Autostart and battery-saver shortcuts where available

BandDrip intentionally does **not** request Accessibility Service access. Accessibility grants screen-inspection/control privileges but is not a legitimate keep-alive mechanism for this relay.

## Important failure tests

- Use an invalid source URL: connection status must become `Failed ✕`; no fabricated replacement reading should appear.
- Cross the 10-minute boundary: the preview must transition to stale automatically without requiring a new packet.
- Use a source with unavailable IOB: glucose may remain valid while IOB shows `—`.
- Stop xDrip or Nightscout connectivity and confirm the displayed reading continues to age rather than being silently refreshed.
- Reboot the phone with background relay enabled and verify BandDrip restarts after boot.

## Reporting a test result

Include:

- BandDrip commit SHA / Actions run
- Android version and phone model
- source mode: Mock / Nightscout / xDrip local server / xDrip broadcast
- visible BandDrip status/error text
- a screenshot when the issue is visual

Do **not** post Nightscout tokens, xDrip secrets, private URLs, glucose exports, or personally identifying health data in a public GitHub issue.
