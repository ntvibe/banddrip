# Security policy

BandDrip handles data that can be sensitive, including glucose readings and optional Nightscout credentials. Security and privacy bugs should be treated accordingly.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for a vulnerability that could expose credentials, private URLs, glucose data, or other personal information.

Prefer GitHub's private vulnerability reporting / Security Advisory flow for this repository when available.

A useful report should include:

- affected BandDrip version or commit
- affected component (`apps/android`, `apps/band`, protocol, CI, etc.)
- reproduction steps using mock/sanitized data
- expected vs actual behavior
- impact assessment

Do not include real access tokens, API secrets, private Nightscout URLs, real glucose exports, or personally identifying health data.

## Credential rules

- No credentials are embedded in the wearable app.
- Secrets must never be committed to the repository.
- Example/test fixtures must use synthetic values only.
- Current development Nightscout tokens are session-only and intentionally not persisted yet.
- Future credential persistence must use Android platform security primitives and must never store plaintext secrets in repository-controlled files.

## Data minimization

The BandDrip wearable protocol should carry only what the display needs. The current reading packet is limited to normalized display data such as glucose, timestamp, delta, trend, optional IOB and small source/sequence metadata. It must not carry Nightscout credentials.

## Safety-related bugs

Please treat these as high-priority defects even when they are not conventional security vulnerabilities:

- stale glucose displayed as current
- missing/invalid timestamps displayed as fresh
- incorrect delta semantics
- stale IOB displayed as current
- malformed packets rendering plausible glucose values
- demo/mock values appearing without an unmistakable development marker
- release builds containing enabled mock data

BandDrip deliberately validates readings on both Android and wearable sides to reduce the chance that one failed layer can silently present invalid data.
