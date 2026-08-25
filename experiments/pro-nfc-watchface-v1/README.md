# BandDrip Smart Band 10 Pro NFC watchface v1

This is the first physical-hardware acceptance build for the user's Xiaomi Smart Band 10 Pro NFC.

## Why DeviceType 367

The watchface project intentionally uses the known Mi Band 9 Pro container format:

- `DeviceType=367`
- `336x480`
- Lua/LVGL runtime

Community Smart Band 10 Pro testing reports that 10 Pro is backward-compatible with 9 Pro watch faces. This avoids inventing an unverified 10 Pro compiler target for the first test.

## What v1 proves

If `BandDripProV1.face` installs and can be selected on the real band, we have proven:

1. custom Lua watchface installation works on this exact Smart Band 10 Pro NFC / firmware combination;
2. the 336x480 Lua runtime starts correctly;
3. MiSans/LVGL labels render correctly;
4. the BandDrip glucose hierarchy is viable on the real OLED;
5. we have a safe base for the Android → watchface bridge.

## What v1 does NOT prove

The glucose is deliberately static demo data. `DEMO` is permanently visible in this first build.

It does **not** yet receive xDrip values from Android. That transport is the next milestone after the face itself is proven safe on hardware.

## Expected screen

```text
BANDDRIP • DEMO

      112 ↘

   +6  ·  3m ago

    IOB 0.250 U

       22:14
     TUE 25 AUG
```

## Safety

Do not install this on Mi Band 8 Pro / 9 Pro hardware during this test. The target under test is the user's Smart Band 10 Pro NFC only.

If the band goes black, becomes unresponsive, or reboots repeatedly, stop testing and return to a stock face. Do not repeatedly reinstall the file.

## Build

GitHub Actions workflow: `Build Pro NFC watchface v1`.

It runs on `windows-latest`, downloads the public community compiler used by the Lua watchface development template, generates the 336x480 preview image, compiles the `.fprj`, and uploads `BandDripProV1.face` as an Actions artifact.
