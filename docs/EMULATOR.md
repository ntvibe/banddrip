# BandDrip emulator workflow

BandDrip should be visually and logically exercised in Xiaomi's Vela tooling before testing on a physical Smart Band 10.

## What the emulator is for

Use the virtual environment to verify:

- 212×520 capsule-screen layout and safe margins
- glucose + trend visual hierarchy
- delta and exact `Xm ago` freshness text
- IOB formatting
- 10-minute stale threshold presentation
- red + strike-through stale state
- timer-driven age refresh when no new reading arrives
- no-reading and malformed-reading states

Do **not** treat emulator success as proof that Android ↔ band interconnect works. Xiaomi recommends real-device debugging for phone communication because simulator-to-phone communication requires additional Bluetooth setup.

## Band 10 display target

- Physical resolution: `212 × 520`
- Device pixel ratio: `2.0`
- Horizontal dp width: `106dp`
- Shape: capsule / pill

The application currently uses `designWidth: 212`, so layout dimensions map directly to the 212px design draft and are scaled by Vela when needed.

## Fast visual preview

In Xiaomi AIoT-IDE, use the multi-screen UI simulator and select the `xiaomi10Band` profile where available.

This is the fastest way to inspect clipping, hierarchy, edge safety, typography, and spacing.

## Full Vela simulator

1. Open/import `apps/band` as the Xiaomi Vela JS application project.
2. Install dependencies:

   ```bash
   npm install
   ```

3. In AIoT-IDE Device Management, create/start a Vela simulator.
4. For exact Band 10 geometry, use the Band 10 device profile where available, or a custom band target with `212 × 520` geometry.
5. Start the project:

   ```bash
   npm start
   ```

The current Xiaomi tooling also supports building an RPK with:

```bash
npm run build
```

## Mock glucose states

Committed source always keeps emulator mock mode OFF:

```js
export const DEV_MOCK_MODE = false
```

For a local emulator session only, change it to:

```js
export const DEV_MOCK_MODE = true
```

in:

`apps/band/src/common/dev-mock.js`

When enabled, BandDrip displays a visible `DEMO` marker and cycles through a new scenario every 5 seconds:

1. **Fresh default** — `112 ↘`, `+6 · 3m ago`, `IOB 0.250 U`.
2. **Stale glucose** — same reading at `12m ago`; glucose and arrow red, glucose natively struck through.
3. **Large 3-digit** — `350 ↑`, `+22`, testing the widest common mg/dL layout.
4. **mmol/L formatting** — `6.2 →`, `+0.3`, exercising decimal glucose/delta.
5. **Stale IOB** — fresh glucose with 12-minute-old IOB; the band must render `IOB —`.
6. **Missing delta** — fresh glucose with no valid previous reading; the band must render `Δ —`.

The scenario objects live in `DEV_MOCK_SEQUENCE`, so additional display edge cases can be added without changing production transport code.

## Release protection

`npm run release` refuses to run unless `DEV_MOCK_MODE` is committed as `false`.

Mock data must never be used as a fallback when real glucose data is absent. Production behavior for missing/invalid data is an unavailable reading (`—`).

## Acceptance checklist

Before moving from emulator work to the physical Band 10:

- [ ] `112 ↘` fits without clipping
- [ ] `350 ↑` fits without clipping
- [ ] mmol/L decimal values fit without clipping
- [ ] trend arrow is visually secondary to glucose
- [ ] `+6 · 3m ago` is clearly readable but ~half the glucose scale
- [ ] `IOB 0.250 U` remains readable at the bottom
- [ ] capsule edges do not clip any text
- [ ] fresh state is not red
- [ ] stale state is unmistakably red + struck through
- [ ] strike-through follows the glucose text width rather than a fixed pixel width
- [ ] exact stale age remains visible
- [ ] missing glucose displays `—`, never the last value as current
- [ ] stale IOB displays `IOB —`
- [ ] missing previous glucose displays `Δ —`
- [ ] no medical disclaimer is placed on the normal band display

After these pass, the next test is the Android companion + `system.interconnect` on a real global Smart Band 10.
