package org.banddrip.app.source

import org.banddrip.app.config.MockSettings
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend

class MockGlucoseSource(
    private val clock: () -> Long = System::currentTimeMillis,
    private val configProvider: () -> MockSettings = { MockSettings() },
) : GlucoseSource {
    override val id: String = "mock"
    private var index = 0L

    override suspend fun latestReading(): BandDripReading {
        val configured = configProvider()
        val scenario = if (configured.autoCycle) autoScenario(index) else configured
        index += 1
        val now = clock()

        return BandDripReading(
            glucose = scenario.glucose,
            units = scenario.units,
            glucoseTimestampMs = now - scenario.ageMinutes * 60_000L,
            delta = scenario.delta,
            trend = scenario.trend,
            iobUnits = scenario.iobUnits,
            iobTimestampMs = scenario.iobUnits?.let { now - scenario.iobAgeMinutes * 60_000L },
            sequence = index,
            source = id,
        )
    }

    private fun autoScenario(step: Long): MockSettings = when ((step % 6).toInt()) {
        0 -> MockSettings(glucose = 112.0, delta = 6.0, ageMinutes = 3, iobUnits = 0.250, trend = Trend.FortyFiveDown)
        1 -> MockSettings(glucose = 112.0, delta = 6.0, ageMinutes = 12, iobUnits = 0.250, trend = Trend.FortyFiveDown)
        2 -> MockSettings(glucose = 350.0, delta = 18.0, ageMinutes = 1, iobUnits = 0.600, trend = Trend.SingleUp)
        3 -> MockSettings(glucose = 6.2, delta = 0.3, ageMinutes = 2, iobUnits = 0.125, trend = Trend.FortyFiveUp, units = GlucoseUnits.MmolL)
        4 -> MockSettings(glucose = 98.0, delta = -4.0, ageMinutes = 4, iobUnits = 0.300, iobAgeMinutes = 15, trend = Trend.FortyFiveDown)
        else -> MockSettings(glucose = 105.0, delta = null, ageMinutes = 2, iobUnits = null, trend = Trend.Flat)
    }
}
