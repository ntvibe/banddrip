package org.banddrip.app.model

data class BandDripReading(
    val glucose: Double,
    val units: GlucoseUnits,
    val glucoseTimestampMs: Long,
    val delta: Double?,
    val trend: Trend,
    val iobUnits: Double? = null,
    val iobTimestampMs: Long? = null,
    val sequence: Long? = null,
    val source: String? = null,
)

enum class GlucoseUnits(val wireValue: String) {
    MgDl("mg/dL"),
    MmolL("mmol/L"),
}

enum class Trend(val wireValue: String) {
    DoubleUp("doubleUp"),
    SingleUp("singleUp"),
    FortyFiveUp("fortyFiveUp"),
    Flat("flat"),
    FortyFiveDown("fortyFiveDown"),
    SingleDown("singleDown"),
    DoubleDown("doubleDown"),
    Unknown("unknown"),
}
