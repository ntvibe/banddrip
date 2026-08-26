package org.banddrip.app.transport

import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Encodes BandDrip state into Gadgetbridge's GenericWeatherReceiver schema.
 *
 * Gadgetbridge expects currentTemp/todayMinTemp/todayMaxTemp in Kelvin. Its
 * Xiaomi weather service subtracts 273 before sending the current temperature,
 * so currentTemp = 273 + glucoseMgDl makes the band-side Celsius value equal to
 * the glucose integer. The rest of the mapping remains provisional until the
 * Smart Band 10 Pro NFC weather probe confirms the exact dataman transforms.
 */
object GlucoseWeatherEncoder {
    private const val KELVIN_OFFSET = 273
    private const val DELTA_OFFSET = 100

    data class Payload(
        val timestampSeconds: Int,
        val location: String,
        val currentTempKelvin: Int,
        val humidity: Int,
        val conditionCode: Int,
        val uvIndex: Float,
        val aqi: Int,
        val pressureMb: Float,
        val windSpeedKmh: Float,
        val windDirectionDegrees: Int,
        val todayMinTempKelvin: Int,
        val todayMaxTempKelvin: Int,
    ) {
        fun toWeatherJson(): String = JSONObject()
            .put("timestamp", timestampSeconds)
            .put("location", location)
            .put("currentTemp", currentTempKelvin)
            .put("todayMinTemp", todayMinTempKelvin)
            .put("todayMaxTemp", todayMaxTempKelvin)
            .put("currentCondition", "BandDrip transport")
            .put("currentConditionCode", conditionCode)
            .put("currentHumidity", humidity)
            .put("windSpeed", windSpeedKmh)
            .put("windDirection", windDirectionDegrees)
            .put("uvIndex", uvIndex)
            .put("pressure", pressureMb)
            .put("airQuality", JSONObject().put("aqi", aqi))
            .put("forecasts", org.json.JSONArray())
            .put("hourly", org.json.JSONArray())
            .toString()
    }

    fun encode(reading: BandDripReading, nowMs: Long = System.currentTimeMillis()): Payload {
        val glucoseMgDl = when (reading.units) {
            GlucoseUnits.MgDl -> reading.glucose
            GlucoseUnits.MmolL -> reading.glucose * 18.0182
        }.roundToInt().coerceIn(20, 500)

        val deltaMgDl = reading.delta?.let {
            when (reading.units) {
                GlucoseUnits.MgDl -> it
                GlucoseUnits.MmolL -> it * 18.0182
            }
        }?.roundToInt() ?: 0

        val ageMinutes = ((nowMs - reading.glucoseTimestampMs).coerceAtLeast(0L) / 60_000L)
            .toInt()
            .coerceIn(0, 100)

        // AQI is an integer field. Offset keeps ordinary signed deltas positive.
        val encodedDelta = (DELTA_OFFSET + deltaMgDl).coerceIn(0, 500)

        // Trend code is intentionally tiny and deterministic.
        val trendCode = when (reading.trend) {
            Trend.DoubleDown -> 0
            Trend.SingleDown -> 1
            Trend.FortyFiveDown -> 2
            Trend.Flat -> 3
            Trend.FortyFiveUp -> 4
            Trend.SingleUp -> 5
            Trend.DoubleUp -> 6
            Trend.Unknown -> 7
        }

        // Candidate IOB carrier. 0.00-3.59 U maps directly into a legal degree
        // range. The hardware probe will tell us whether wind angle survives
        // Xiaomi's transform; production mapping can move to pressure if needed.
        val iobCentiUnits = ((reading.iobUnits ?: 0.0) * 100.0)
            .roundToInt()
            .coerceIn(0, 359)

        return Payload(
            timestampSeconds = (nowMs / 1000L).toInt(),
            location = "BandDrip",
            currentTempKelvin = KELVIN_OFFSET + glucoseMgDl,
            humidity = ageMinutes,
            conditionCode = conditionForTrend(reading.trend),
            uvIndex = trendCode.toFloat(),
            aqi = encodedDelta,
            // Distinct but meteorologically plausible pressure. We are not yet
            // relying on this field for production data.
            pressureMb = 1000f + (reading.sequence?.rem(50)?.toFloat() ?: 0f),
            windSpeedKmh = 12f,
            windDirectionDegrees = iobCentiUnits,
            todayMinTempKelvin = KELVIN_OFFSET + glucoseMgDl,
            todayMaxTempKelvin = KELVIN_OFFSET + glucoseMgDl,
        )
    }

    fun probeA(nowMs: Long = System.currentTimeMillis()) = Payload(
        timestampSeconds = (nowMs / 1000L).toInt(),
        location = "BandDrip TEST A",
        currentTempKelvin = KELVIN_OFFSET + 123,
        humidity = 42,
        conditionCode = 800,
        uvIndex = 6f,
        aqi = 155,
        pressureMb = 1012f,
        windSpeedKmh = 12f,
        windDirectionDegrees = 271,
        todayMinTempKelvin = KELVIN_OFFSET + 111,
        todayMaxTempKelvin = KELVIN_OFFSET + 133,
    )

    fun probeB(nowMs: Long = System.currentTimeMillis()) = Payload(
        timestampSeconds = (nowMs / 1000L).toInt(),
        location = "BandDrip TEST B",
        currentTempKelvin = KELVIN_OFFSET + 124,
        humidity = 43,
        conditionCode = 801,
        uvIndex = 7f,
        aqi = 156,
        pressureMb = 1013f,
        windSpeedKmh = 20f,
        windDirectionDegrees = 272,
        todayMinTempKelvin = KELVIN_OFFSET + 112,
        todayMaxTempKelvin = KELVIN_OFFSET + 134,
    )

    private fun conditionForTrend(trend: Trend): Int = when (trend) {
        Trend.DoubleDown -> 502
        Trend.SingleDown -> 500
        Trend.FortyFiveDown -> 803
        Trend.Flat -> 800
        Trend.FortyFiveUp -> 801
        Trend.SingleUp -> 802
        Trend.DoubleUp -> 200
        Trend.Unknown -> 804
    }
}
