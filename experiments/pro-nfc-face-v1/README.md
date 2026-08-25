# BandDrip Pro NFC Probe v1

First native hardware probe for Xiaomi Smart Band 10 Pro / Pro NFC.

## Target

- Display: 336 × 480
- Xiaomi watch-face `DeviceType`: `567`
- Runtime: Lua + LVGL
- Output: `BandDrip-Pro-NFC-Probe-v1.face`
- Watch-face ID used by CI: `26082501`

`DeviceType=567` is taken from a working public Smart Band 10 Pro Lua watch-face project (`m0tral/MiWatchLuaWatchfaces/MiBand10Pro/UorbGPS`).

## What v1 proves

This is intentionally a **native execution probe**, not the live glucose transport yet.

After installation the face should:

1. render the BandDrip 336×480 layout on the real OLED;
2. show the current clock;
3. start in a fresh demo state (`112 ↘`, `+6 · 3m ago`, `IOB 0.250 U`);
4. cycle on tap: **fresh → stale 12m → no data → fresh**;
5. show stale/no-data glucose in red with a visible strike bar.

If those work, we have proven that our own Lua code is executing as the active watch face on the Pro NFC. The next layer is Android → watch live state transport.

## Live-state contract planned for v2

Android owns data acquisition. The watch face never talks to xDrip directly.

```text
xDrip / another Android source
          ↓
BandDrip Android relay
          ↓
Pro NFC transport
          ↓
watch-face render(state)
```

The render-state shape is deliberately small:

```json
{
  "glucose": "112",
  "trend": "↘",
  "delta": "+6",
  "ageMinutes": 3,
  "iob": "0.250",
  "timestampMs": 1787670000000
}
```

The watch must age the reading locally from `timestampMs`. At 10 minutes glucose becomes stale (red + strike), even if the phone disappears. Missing previous glucose yields `Δ —`; missing/currently unverifiable IOB yields `IOB —`.

## Build

`.github/workflows/pro-nfc-face-build.yml` runs on Windows and uses the public `FangAiden/LuaDevTemplate` compiler environment only during CI. BandDrip does not commit or redistribute that compiler.

The FPRJ is converted to UTF-16 before compilation because the Xiaomi face project format declares UTF-16.

## Safety boundary

- No firmware flashing.
- No NFC/payment modification.
- No AuthKey/encryptKey is committed or required to build the face.
- Do not install this on Band 8/9 Pro or regular Band 10 targets.
- First installation is a hardware experiment; keep Mi Fitness paired and recovery/reset options available.
