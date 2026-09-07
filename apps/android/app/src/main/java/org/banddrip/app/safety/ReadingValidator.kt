package org.banddrip.app.safety

import org.banddrip.app.model.BandDripReading

object ReadingValidator {
    fun validate(reading: BandDripReading, nowMs: Long = System.currentTimeMillis()): List<String> {
        val errors = mutableListOf<String>()

        if (!reading.glucose.isFinite() || reading.glucose <= 0.0) {
            errors += "glucose must be finite and positive"
        }
        if (FreshnessPolicy.ageMinutes(reading.glucoseTimestampMs, nowMs) == null) {
            errors += "glucose timestamp is invalid"
        }
        if (reading.delta != null && !reading.delta.isFinite()) {
            errors += "delta must be finite when present"
        }
        if (reading.iobUnits != null && (!reading.iobUnits.isFinite() || reading.iobUnits < 0.0)) {
            errors += "IOB must be finite and non-negative when present"
        }
        if (reading.iobUnits != null && reading.iobTimestampMs == null) {
            errors += "IOB timestamp is required when IOB is present"
        }
        if (reading.iobTimestampMs != null && FreshnessPolicy.ageMinutes(reading.iobTimestampMs, nowMs) == null) {
            errors += "IOB timestamp is invalid"
        }

        return errors
    }

    fun requireValid(reading: BandDripReading, nowMs: Long = System.currentTimeMillis()) {
        val errors = validate(reading, nowMs)
        require(errors.isEmpty()) { errors.joinToString("; ") }
    }
}
