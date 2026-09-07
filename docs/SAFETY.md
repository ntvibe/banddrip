# Safety and display rules

BandDrip is an unofficial secondary glucose display. It is not intended to replace an approved CGM, pump, receiver, or treatment workflow.

## Non-negotiable wearable behavior

### Freshness

The band always shows the exact age of the glucose measurement.

- `0–9 minutes`: render normally.
- `10+ minutes`: render the glucose value in red with a strike-through and continue showing the exact age.
- Missing, invalid, or future-skewed timestamps: treat the value as unavailable/stale rather than current.
- Connection state must never reset reading age.

Example fresh state:

```text
       112 ↘

     +6 · 3m ago

     IOB 0.250 U
```

Example stale state:

```text
       112̶ ↘

    +6 · 12m ago

     IOB 0.250 U
```

The real UI uses red plus a visible strike line for stale glucose. Color is not the only stale indicator.

### Delta

Delta is always displayed when two valid consecutive readings are available.

`delta = current glucose - immediately previous valid glucose`

If a previous valid reading is unavailable, display an explicit unavailable marker rather than estimating a delta.

### IOB

IOB is visible by default and may be disabled in Android settings. IOB has its own timestamp/freshness and must not be presented indefinitely when its source stops updating.

## Android disclaimer

The Android companion should keep a small persistent disclaimer near the bottom of the main/settings UI and provide the full notice during onboarding and under About.

Suggested concise wording:

> BandDrip is an unofficial secondary glucose display and is not a medical device. Do not use it as the sole basis for treatment decisions. Data may be delayed, unavailable, or incorrect. Use at your own risk and verify important readings in your approved CGM/pump system.

The project should not claim that this notice eliminates every possible legal liability. Licensing, warranty disclaimers, and jurisdiction-specific legal obligations are separate from UI copy.

## Fail safe, not fail pretty

When uncertain, BandDrip should make missing/stale data obvious instead of preserving a visually attractive but potentially misleading number.
