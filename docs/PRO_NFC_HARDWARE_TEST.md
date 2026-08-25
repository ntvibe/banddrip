# Smart Band 10 Pro NFC hardware test

This is the first physical-device validation path for BandDrip on Xiaomi Smart Band 10 Pro NFC.

## Goal

Prove real OLED geometry and the watch-face channel before any firmware modification, Quick App experiment, or live-data bridge work.

Target display geometry: **336 × 480 portrait pixels**.

## Baseline first

1. Pair the band normally with stock Mi Fitness.
2. Verify normal sync, raise-to-wake, stock watch face and notifications.
3. Open `Settings → System → About` on the band and record model, firmware/system version and OS version.
4. Record Mi Fitness version and account region if visible.
5. If a firmware update is offered, record both current and offered versions before updating.

Do not expose serial numbers, MAC addresses, AuthKeys or payment information in public reports.

## Zero-risk display test

Use Xiaomi's official custom-photo band-display flow first. This installs no executable BandDrip code.

1. Copy the BandDrip 336×480 fresh test image to the phone gallery.
2. Mi Fitness → Device → Manage band displays → All → Custom.
3. Pick a Custom/photo display, tap Edit, then Add photo.
4. Select the test image and choose a clock style/position that overlaps the BandDrip block as little as possible.
5. Apply and sync.
6. Photograph the band straight-on and at normal wrist distance.
7. Repeat with the stale-state image.

Validate:

- image scaling/cropping
- safe margins
- glucose/trend readability
- delta + exact age readability
- IOB readability
- stale red + strike-through visibility
- NFC/payment behavior remains normal

## Do not do yet

- Do not install the regular Band 10 BandDrip RPK on Pro NFC.
- Do not cross-flash CN firmware.
- Do not modify NFC/payment components.
- Do not factory-reset unless recovery requires it.

## Experimental native custom-face route

Only after the baseline is captured:

1. Use a custom-watchface tool that explicitly supports Xiaomi Smart Band 10 Pro and the exact NFC variant.
2. Read/obtain the device AuthKey using that tool's documented process.
3. First install a known-compatible harmless watch face to prove transport and recovery.
4. Only then attempt a BandDrip native test face.

The custom-photo baseline and the native custom-face route are intentionally separate. A successful photo sync proves display geometry only; it does not prove custom Lua/watch-face code execution or live Android data transport.

## Evidence to capture

- About screen photo with serial/MAC hidden
- current firmware and offered firmware, if any
- Mi Fitness version/region
- fresh-state BandDrip image on the real OLED
- stale-state BandDrip image on the real OLED
- whether NFC/payment remains normal

## Current architectural expectation

Global Smart Band 10 Pro firmware should be treated as a watch-face-first target. The regular Smart Band 10 Quick App / `system.interconnect` RPK architecture remains a separate target until hardware evidence proves otherwise.
