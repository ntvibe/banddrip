# BandDrip Lua glance-face experiment

Goal: make glucose visible on the Smart Band home/watch-face surface immediately after raise-to-wake, with no tap or swipe.

## Proposed bridge

```text
Android source (xDrip / Nightscout)
        ↓
BandDrip Android relay
        ↓
Xiaomi system.interconnect
        ↓
BandDrip Vela RPK
        ↓
system.file → internal://files/glance.json
        ↓
underlying NuttX Quick App file sandbox
        ↓
Lua/LVGL BandDrip watch face
```

The Vela app already writes an intentionally small `glance.json` after every accepted reading and after relevant display-setting changes.

The Lua probe attempts these read-only paths in order:

1. `/data/quickapp/file/org.banddrip.app/glance.json`
2. `/data/quickapp/files/org.banddrip.app/glance.json`
3. `/data/quickapp/file/org.banddrip.app/files/glance.json`

The first path is the current research-backed mapping for `internal://files/` on Vela systems. The other paths are harmless compatibility probes.

## Why Lua

Community Band 10-class watch-face experiments demonstrate Lua/LVGL watch faces reading normal NuttX paths and `/dev/uorb/...` devices using `io.open`. That makes a read-only bridge into the Vela Quick App file sandbox plausible, but **not yet proven on a global Smart Band 10**.

## Safety / freshness behavior

The face never invents a reading. It reads the original measurement timestamp and computes freshness itself.

- `<10m`: normal glucose display
- `10m+`: red glucose + strike-through indicator
- exact `Xm ago` remains visible
- delta comes from the normalized reading
- IOB is independently hidden/unavailable when stale
- if no readable state exists, glucose is `—`

The face refreshes immediately on resume and then every 15 seconds while active. It pauses its timer when the page is paused.

## Hardware acceptance test

Once a global Smart Band 10 is available:

1. Install the matching BandDrip RPK.
2. Deliver a mock reading to the RPK.
3. Verify `internal://files/glance.json` was written.
4. Install the Lua face package.
5. Raise wrist and verify the face reads the same glucose/delta/timestamp/IOB.
6. Leave the source unchanged across the 9m → 10m boundary and confirm stale rendering changes without a new phone packet.
7. Verify IOB freshness independently.
8. Measure active/AOD battery impact.

If cross-sandbox reads are denied on the shipping Band 10 firmware, this experiment fails safely and the face remains unavailable; the normal Vela app continues to work independently.
