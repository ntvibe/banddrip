# BandDrip Pro NFC dataman scanner

This hardware probe maps which Xiaomi Lua watchface `dataman` sources are actually accepted by the Smart Band 10 Pro NFC firmware.

The previous live-value probe failed because `dataman.subscribe("AppAlarmHour", ...)` throws `invalid data sources` on this device. The scanner therefore wraps every subscription in `pcall()` so one unsupported key cannot crash the watchface.

## What it shows

- `VALID x / 29`: number of candidate keys accepted by the runtime.
- `REJECTED y`: keys rejected immediately by `dataman.subscribe`.
- Each accepted key appears as `SUBSCRIBED` until it emits a value, then shows the decoded integer value.
- Pages rotate automatically every 5 seconds.

The candidate set intentionally focuses on non-weather system, health, clock and legacy app/alarm keys. The goal is to identify a stock-firmware source that Gadgetbridge or the phone can change live and that a watchface can read without reinstalling the `.face`.

## Next decision

1. If `systemStatusDisturb`, `systemStatusBluetooth`, or another phone-influenced source is accepted, test whether a Gadgetbridge-side change updates it live.
2. If legacy `AppAlarmHour` / `AppAlarmMinute` are rejected while other sources work, the alarm transport is ruled out for this firmware.
3. If only passive sources exist, move from `dataman` to another watchface-accessible IPC/storage mechanism rather than guessing more source names.
