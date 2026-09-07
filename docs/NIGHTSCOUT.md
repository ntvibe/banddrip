# Nightscout source

BandDrip treats Nightscout as one glucose source adapter, not as part of the wearable protocol.

## Current implementation

The Android companion requests:

- `GET /api/v1/entries/sgv.json?count=2`
- `GET /api/v1/devicestatus.json?count=1` when available

The newest two SGV entries are normalized into a `BandDripReading`. BandDrip intentionally computes delta itself:

`delta = current glucose - immediately previous valid glucose`

It does not trust a server-provided delta field for the value shown on the band.

Nightscout direction strings are normalized to the BandDrip trend enum.

## IOB

IOB is optional. The adapter currently looks for a numeric `iob` value in recent `openaps`, `loop`, or `pump` device-status data. An IOB value is only accepted with a parseable device-status timestamp.

The wearable applies freshness independently to IOB. A fresh glucose value never makes an old IOB value appear current.

## Authentication

The current development console accepts an optional Nightscout access token and appends it as a `token` query parameter. The development build keeps the token in memory only; it is deliberately not persisted yet.

Do not commit Nightscout URLs, tokens, API secrets, glucose exports, or personal data to the repository.

## Network policy

BandDrip requires an HTTPS Nightscout URL. Plain HTTP is rejected by the source adapter.

## Failure behavior

Network errors, malformed JSON, missing glucose timestamps, or invalid numeric values fail closed. Invalid readings are not forwarded to the band.

The band also validates incoming packets independently before rendering them.
