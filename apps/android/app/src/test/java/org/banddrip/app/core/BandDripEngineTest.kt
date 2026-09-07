package org.banddrip.app.core

import kotlinx.coroutines.runBlocking
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.banddrip.app.source.GlucoseSource
import org.banddrip.app.transport.VirtualBandTransport
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BandDripEngineTest {
    @Test
    fun validReadingProducesReadingPacket() = runBlocking {
        val now = System.currentTimeMillis()
        val source = fixedSource(
            BandDripReading(
                glucose = 112.0,
                units = GlucoseUnits.MgDl,
                glucoseTimestampMs = now,
                delta = 6.0,
                trend = Trend.FortyFiveDown,
                iobUnits = 0.25,
                iobTimestampMs = now,
                source = "test",
            ),
        )
        val packets = mutableListOf<String>()
        val transport = VirtualBandTransport(packets::add)

        val snapshot = BandDripEngine().refresh(source, transport, showIob = true)

        assertNull(snapshot.errorMessage)
        assertNotNull(snapshot.reading)
        assertTrue(packets.any { it.contains("\"type\":\"reading\"") })
    }

    @Test
    fun invalidReadingNeverProducesReadingPacket() = runBlocking {
        val source = fixedSource(
            BandDripReading(
                glucose = 0.0,
                units = GlucoseUnits.MgDl,
                glucoseTimestampMs = System.currentTimeMillis(),
                delta = 0.0,
                trend = Trend.Flat,
            ),
        )
        val packets = mutableListOf<String>()
        val transport = VirtualBandTransport(packets::add)

        val snapshot = BandDripEngine().refresh(source, transport, showIob = true)

        assertNotNull(snapshot.errorMessage)
        assertNull(snapshot.reading)
        assertTrue(packets.none { it.contains("\"type\":\"reading\"") })
    }

    private fun fixedSource(reading: BandDripReading) = object : GlucoseSource {
        override val id: String = "test"
        override suspend fun latestReading(): BandDripReading = reading
    }
}
