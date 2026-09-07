package org.banddrip.app.safety

import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingValidatorTest {
    private val now = 1_800_000_000_000L

    @Test
    fun rejectsNonPositiveGlucose() {
        val errors = ReadingValidator.validate(validReading().copy(glucose = 0.0), now)
        assertTrue(errors.any { it.contains("glucose") })
    }

    @Test
    fun rejectsIobWithoutTimestamp() {
        val errors = ReadingValidator.validate(
            validReading().copy(iobUnits = 0.25, iobTimestampMs = null),
            now,
        )
        assertTrue(errors.any { it.contains("IOB timestamp") })
    }

    private fun validReading() = BandDripReading(
        glucose = 112.0,
        units = GlucoseUnits.MgDl,
        glucoseTimestampMs = now,
        delta = 6.0,
        trend = Trend.Flat,
    )
}
