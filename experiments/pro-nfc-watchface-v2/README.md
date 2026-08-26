# BandDrip Smart Band 10 Pro NFC watchface v2

v2 is the first **live-source** hardware test for the user's Xiaomi Smart Band 10 Pro NFC.

It intentionally reuses the exact watchface container that already worked on the physical band in v1:

- `DeviceType=367`
- 336x480 Lua/LVGL runtime
- EasyFace `.face` package
- install through Gadgetbridge

## What changed from v1

v1 proved that a custom Lua watchface can install and run on this Smart Band 10 Pro NFC. Its glucose value was deliberately static demo data.

v2 removes the demo state and tries to read real glucose state from the band filesystem every 15 seconds and immediately after the face resumes.

### Source 1: WatchDrip-compatible Quick App

A public Xiaomi Band 9 Lua WatchDrip face reads `info.json` from these paths:

```text
//data/quickapp/files/com.thatguysservice.huami_xdrip/info.json
//data/quickapp/files/com.application.watch.watchdrip/info.json
```

That community implementation reads:

- `bg.val`
- `bg.delta`
- `bg.trend`
- `bg.time`
- `status.isMgdl`

BandDrip v2 now supports the same schema and path convention.

Reference implementation:
`miguelavh/LUA_Watchdrip_Watchface_graph_Xiaomi_band9`

### Source 2: BandDrip glance bridge

v2 also probes the candidate paths already used by the BandDrip glance-face experiment:

```text
//data/quickapp/files/org.banddrip.app/glance.json
/data/quickapp/file/org.banddrip.app/glance.json
/data/quickapp/files/org.banddrip.app/glance.json
/data/quickapp/file/org.banddrip.app/files/glance.json
```

The BandDrip schema supports glucose, +/- delta, trend, original glucose timestamp, units, IOB, independent IOB timestamp and the IOB display toggle.

## Display behavior

```text
BANDDRIP V2 • WATCHDRIP     (or BANDDRIP)

          112 ↘

      +6  ·  3m ago

        IOB 0.250 U

           10:24
        WED 26 AUG
```

The core display rules remain non-negotiable:

- large glucose number + trend arrow
- +/- delta directly below
- exact `Xm ago` freshness
- IOB visible by default when available
- at 10 minutes stale: glucose and arrow turn red and glucose receives a strike-through
- freshness continues aging on the band even if no new phone packet arrives
- no readable source: display `---`, `--m ago`, and `IOB —`; never invent a glucose value

## What this test proves

If v2 shows `WATCHDRIP` or `BANDDRIP` in the header and live values change, cross-sandbox file reading works on this Smart Band 10 Pro NFC firmware.

If it stays on `WAITING`, the watchface itself is still functioning; it simply means none of the candidate data files are currently readable. That is useful evidence and does not imply the `.face` failed.

## Gadgetbridge scope

Gadgetbridge is currently the proven installation/device-control route. v2 does **not** pretend that Gadgetbridge already provides a direct live-data API to this Lua watchface. The immediate experiment is to reuse filesystem state produced by a WatchDrip-compatible or BandDrip Quick App.

## Build

GitHub Actions workflow: `Build Pro NFC watchface v2`.

Artifact: `BandDripProV2-face` containing `BandDripProV2.face`.
