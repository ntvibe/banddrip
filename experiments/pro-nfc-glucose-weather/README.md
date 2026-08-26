# BandDrip Glucose Weather Face

Production MVP watchface for Xiaomi Smart Band 10 Pro NFC on stock firmware.

## Transport

BandDrip Android acts as an external weather provider for Gadgetbridge. Gadgetbridge owns the authenticated Xiaomi connection and forwards the synthetic weather payload to the band. The watchface decodes the weather `dataman` fields back into glucose state.

| Weather carrier | BandDrip value |
|---|---|
| `weatherCurrentTemperature` | glucose mg/dL |
| `weatherCurrentHumidity` | glucose age in minutes |
| `weatherCurrentAirQualityIndex` | delta + 100; 500 = missing |
| `weatherCurrentUVIndex` | trend code 0..7 |
| `weatherCurrentPressure` | `100 + IOB*1000`; 0 = hidden/missing |

## Safety behavior

The watchface tracks when the transport last updated and increases the age locally. This prevents a phone/Gadgetbridge failure from leaving an old value looking fresh. At 10 minutes the glucose value and age become red and the glucose is struck through. IOB is hidden once the glucose is stale.

The Android relay intentionally does not blank a good reading immediately on a transient source/network error; the watchface ages the last value naturally into the stale state.

This is an unofficial secondary display, not a medical device or treatment-decision source.
