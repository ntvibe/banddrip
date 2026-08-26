package org.banddrip.app.transport

import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.junit.Assert.assertEquals
import org.junit.Test

class GlucoseWeatherEncoderTest {
    private val now = 1_900_000_000_000L

    @Test
    fun `encodes complete mgdl reading into weather carriers`() {
        val reading = BandDripReading(
            glucose = 123.0,
            units = GlucoseUnits.MgDl,
            glucoseTimestampMs = now - 2 * 60_000L,
            delta = 5.0,
            trend = Trend.Flat,
            iobUnits = 0.250,
            iobTimestampMs = now - 60_000L,
            source = "test",
        )

        val payload = GlucoseWeatherEncoder.encode(reading, showIob = true, nowMs = now)

        assertEquals(396, payload.currentTempKelvin)
        assertEquals(2, payload.humidity)
        assertEquals(105, payload.aqi)
        assertEquals(4f, payload.uvIndex)
        assertEquals(350f, payload.pressureMb)
    }

    @Test
    fun `normalizes mmol reading and signed delta into mgdl carriers`() {
        val reading = BandDripReading(
            glucose = 6.8,
            units = GlucoseUnits.MmolL,
            glucoseTimestampMs = now,
            delta = -0.3,
            trend = Trend.FortyFiveDown,
            source = "test",
        )

        val payload = GlucoseWeatherEncoder.encode(reading, nowMs = now)

        assertEquals(396, payload.currentTempKelvin)
        assertEquals(95, payload.aqi)
        assertEquals(3f, payload.uvIndex)
    }

    @Test
    fun `does not publish stale or disabled iob`() {
        val staleIob = BandDripReading(
            glucose = 110.0,
            units = GlucoseUnits.MgDl,
            glucoseTimestampMs = now,
            delta = null,
            trend = Trend.Unknown,
            iobUnits = 1.125,
            iobTimestampMs = now - 10 * 60_000L,
            source = "test",
        )

        assertEquals(0f, GlucoseWeatherEncoder.encode(staleIob, nowMs = now).pressureMb)

        val freshIob = staleIob.copy(iobTimestampMs = now - 60_000L)
        assertEquals(
            0f,
            GlucoseWeatherEncoder.encode(freshIob, showIob = false, nowMs = now).pressureMb,
        )
    }

    @Test
    fun `uses explicit sentinels for missing delta and no data`() {
        val reading = BandDripReading(
            glucose = 100.0,
            units = GlucoseUnits.MgDl,
            glucoseTimestampMs = now,
            delta = null,
            trend = Trend.Unknown,
            source = "test",
        )

        assertEquals(500, GlucoseWeatherEncoder.encode(reading, nowMs = now).aqi)

        val unavailable = GlucoseWeatherEncoder.unavailable(now)
        assertEquals(273, unavailable.currentTempKelvin)
        assertEquals(99, unavailable.humidity)
        assertEquals(500, unavailable.aqi)
        assertEquals(0f, unavailable.pressureMb)
    }
}
