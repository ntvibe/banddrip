package org.banddrip.app.protocol

import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolCodecTest {
    @Test
    fun encodesRequiredReadingFields() {
        val packet = ProtocolCodec.encodeReading(
            BandDripReading(
                glucose = 112.0,
                units = GlucoseUnits.MgDl,
                glucoseTimestampMs = 1_800_000_000_000L,
                delta = 6.0,
                trend = Trend.FortyFiveDown,
                iobUnits = 0.250,
                iobTimestampMs = 1_800_000_000_000L,
                source = "mock",
            ),
        )
        val json = JSONObject(packet)

        assertEquals(1, json.getInt("protocolVersion"))
        assertEquals("reading", json.getString("type"))
        assertEquals(112.0, json.getDouble("glucose"), 0.0)
        assertEquals(6.0, json.getDouble("delta"), 0.0)
        assertEquals("fortyFiveDown", json.getString("trend"))
        assertEquals(0.250, json.getDouble("iobUnits"), 0.0)
    }

    @Test
    fun settingsDefaultsCanExplicitlyCarryIobState() {
        val json = JSONObject(ProtocolCodec.encodeSettings(showIob = true))
        assertTrue(json.getBoolean("showIob"))
    }
}
