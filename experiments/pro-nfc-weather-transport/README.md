# Smart Band 10 Pro NFC weather transport probe

Goal: prove a multi-value phone -> Gadgetbridge -> Xiaomi weather service -> stock Lua watchface path.

## Why weather

Gadgetbridge exposes a documented generic weather-provider broadcast. Its Xiaomi weather service forwards several current-condition fields in one command: temperature, humidity, condition, wind, UV, AQI and pressure. A custom watchface can subscribe to Xiaomi `dataman` weather sources and decode those values.

This experiment does **not** intend to preserve real weather while BandDrip is using the channel. Weather is only a transport carrier.

## Test flow

1. Install `BandDripWeatherTransport.face` through Gadgetbridge and activate it.
2. Open the temporary **BandDrip Weather Probe** launcher activity from the Android app.
3. Tap **Send TEST A**. Photograph every page of the watchface.
4. Tap **Send TEST B**. Confirm which raw values change without reinstalling the face.

The Android test payloads deliberately use distinctive values so field transforms/clamping are easy to identify.

## Intended production ownership

The Android BandDrip app is the encoder. It fetches/receives the glucose reading, validates freshness and converts the reading to a synthetic Gadgetbridge `WeatherSpec` JSON. Gadgetbridge handles the Xiaomi protocol. The watchface is only a decoder/renderer.

Provisional mapping after the transport fields are verified on hardware:

- current temperature -> glucose mg/dL
- humidity -> reading age in minutes
- AQI -> signed delta with an offset
- UV or condition -> trend code
- wind angle or pressure -> IOB

Do not freeze this mapping until the hardware probe confirms exactly which weather `dataman` sources update and how Xiaomi transforms each value.
