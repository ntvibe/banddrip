package org.banddrip.app.source

import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend

class MockGlucoseSource(
    private val clock: () -> Long = System::currentTimeMillis,
) : GlucoseSource {
    override val id: String = "mock"

    private var index = 0
    private val values = listOf(106.0, 112.0, 109.0, 115.0, 118.0, 114.0)

    override suspend fun latestReading(): BandDripReading {
        val currentIndex = index % values.size
        val previousIndex = (currentIndex - 1 + values.size) % values.size
        val glucose = values[currentIndex]
        val previous = values[previousIndex]
        index += 1

        return BandDripReading(
            glucose = glucose,
            units = GlucoseUnits.MgDl,
            glucoseTimestampMs = clock(),
            delta = glucose - previous,
            trend = when {
                glucose - previous >= 6 -> Trend.FortyFiveUp
                glucose - previous <= -6 -> Trend.FortyFiveDown
                else -> Trend.Flat
            },
            iobUnits = 0.250,
            iobTimestampMs = clock(),
            sequence = index.toLong(),
            source = id,
        )
    }
}
