# Contributing to BandDrip

Thanks for helping improve BandDrip.

The project is intentionally small and modular so contributors can work on a glucose source, Android transport, wearable UI, or documentation without needing to understand every layer.

## Before opening a change

- Keep personal CGM URLs, tokens, account identifiers, signing keys, and glucose history out of commits, screenshots, logs, and issues.
- Use synthetic/mock readings in examples and tests.
- Preserve the freshness rules in `docs/SAFETY.md`.
- Do not make medical-device or treatment claims.
- Prefer official Xiaomi Vela APIs and documented Android APIs before reverse-engineered paths.

## Core invariants

Changes must not break these behaviors:

1. Exact glucose age remains visible on the wearable.
2. A glucose reading at 10+ minutes old is visually stale: red plus strike-through.
3. Delta represents current glucose minus the immediately previous valid glucose reading.
4. Missing timestamps are never treated as fresh.
5. IOB is enabled by default but user-configurable from Android.
6. Source-specific credentials never need to reach the wearable.

## Repository areas

- `apps/band` — Xiaomi Vela wearable app.
- `apps/android` — Android companion and data sources.
- `packages/protocol` — source-independent message contract.
- `docs` — architecture, safety behavior, setup, and research notes.

## Pull requests

Keep PRs focused. Explain:

- what changed;
- why it changed;
- how it was tested;
- whether it affects stale-data, delta, trend, IOB, or transport behavior.

For UI changes, include screenshots using mock data where practical.

## Hardware findings

Real-device Xiaomi behavior is especially valuable. When documenting a finding, include the Smart Band model, global/CN variant, firmware version, Mi Fitness version, phone/Android version, and whether the result was reproduced.
