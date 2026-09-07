package org.banddrip.app.source

import org.banddrip.app.model.Trend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NightscoutParserTest {
    @Test
    fun computesDeltaFromPreviousReading() {
        val entries = """
            [
              {"sgv":112,"date":1800000000000,"direction":"FortyFiveDown"},
              {"sgv":106,"date":1799999700000,"direction":"Flat"}
            ]
        """.trimIndent()

        val reading = NightscoutParser.parseReading(entries)
        assertNotNull(reading)
        assertEquals(112.0, reading!!.glucose, 0.0)
        assertEquals(6.0, reading.delta!!, 0.0)
        assertEquals(Trend.FortyFiveDown, reading.trend)
    }

    @Test
    fun extractsOpenApsIobWithTimestamp() {
        val status = """
            [{
              "created_at":"2027-01-15T08:00:00Z",
              "openaps":{"iob":{"iob":0.25}}
            }]
        """.trimIndent()

        val iob = NightscoutParser.parseIob(status)
        assertNotNull(iob)
        assertEquals(0.25, iob!!.units, 0.0)
        assertEquals(1800000000000L, iob.timestampMs)
    }
}
