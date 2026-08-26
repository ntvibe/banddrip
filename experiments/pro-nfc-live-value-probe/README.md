# BandDrip Pro NFC live-value probe

This experiment answers one narrow hardware question:

> Can a Lua watchface on the Xiaomi Smart Band 10 Pro NFC react to a value changed from Gadgetbridge without reinstalling the watchface?

## Channel under test

Xiaomi Lua watchfaces expose the built-in `dataman` topics `AppAlarmHour` and `AppAlarmMinute`. Values arrive as fixed-point integers (`value // 256`).

The probe treats the alarm time as a base-60 transport:

```text
decodedValue = hour * 60 + minute
```

Examples:

| Alarm time | Decoded value |
| --- | ---: |
| 02:03 | 123 |
| 02:04 | 124 |
| 03:00 | 180 |

This is a transport experiment only. It is **not** the intended production BandDrip data channel because it temporarily repurposes an alarm.

## Hardware test

1. Build/download `BandDripLiveValueProbe.face` and install it through Gadgetbridge.
2. Activate the face. Confirm the bottom diagnostic shows a real battery percentage. That proves `dataman` is alive.
3. In Gadgetbridge, create/enable a band alarm at **02:03**. Wake/re-open the watchface if needed.
4. Success stage A: the face shows `123` and `alarm 02:03`.
5. Change the same alarm to **02:04** without reinstalling the face.
6. Success stage B: the face changes to `124` and `alarm 02:04`.
7. Delete/disable the test alarm when finished so it cannot ring later.

## Interpretation

- **123 -> 124 live:** stock firmware exposes a phone-writable watchface data channel. BandDrip can move on to a cleaner encoder/transport search and Android automation.
- **Battery updates, alarm stays WAITING:** `dataman` works but these alarm topics are absent/blocked on this firmware. Probe another phone-writable dataman source.
- **Even battery stays `--%`:** the Band 10 Pro NFC Lua runtime differs enough that this dataman API is unavailable; inspect the runtime/firmware interface before continuing.

## Why this probe

The RPK/Quick App writer route reaches the Xiaomi SPP transport on this band but the firmware does not answer the third-party app install service. A watchface `.face` does install successfully. This probe therefore stays entirely inside the already-working watchface path and tests a stock system data source rather than trying another RPK.
