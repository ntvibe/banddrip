package org.banddrip.app.transport

import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.banddrip.app.safety.FreshnessPolicy
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Production weather-carrier codec for the stock-firmware Smart Band 10 Pro NFC.
 *
 * BandDrip deliberately uses Gadgetbridge's Generic Weather provider interface as
 * a transport only. The companion watchface decodes the values back into glucose
 * state and never presents them as weather.
 *
 * Carrier map (band-side integer after Xiaomi's fixed-point conversion):
 *   temperature -> glucose mg/dL
 *   humidity    -> glucose age in minutes
 *   AQI         -> signed delta + 100 (500 means missing)
 *   UV index    -> trend enum 0..7
 *   pressure    -> 100 + IOB milli-units (0 means hidden/unavailable)
 *
 * Gadgetbridge expects temperatures in Kelvin and Xiaomi sends Celsius to the
 * band, therefore currentTemp = 273 + glucose makes the band-side value equal to
 * the mg/dL integer.
 */
object GlucoseWeatherEncoder {
    private const val KELVIN_OFFSET = 273
    private const val DELTA_OFFSET = 100
    private const val DELTA_MISSING = 500
    private const val IOB_BASE = 100
    private const val IOB_MAX_UNITS = 9.999

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
            .put("forecasts", JSONArray())
            .put("hourly", JSONArray())
            .toString()
    }

    fun encode(
        reading: BandDripReading,
        showIob: Boolean = true,
        nowMs: Long = System.currentTimeMillis(),
    ): Payload {
        val glucoseMgDl = when (reading.units) {
            GlucoseUnits.MgDl -> reading.glucose
            GlucoseUnits.MmolL -> reading.glucose * 18.0182
        }.roundToInt().coerceIn(20, 500)

        val deltaMgDl = reading.delta?.let {
            when (reading.units) {
                GlucoseUnits.MgDl -> it
                GlucoseUnits.MmolL -> it * 18.0182
            }
        }?.roundToInt()

        val ageMinutes = FreshnessPolicy.ageMinutes(reading.glucoseTimestampMs, nowMs)
            ?.toInt()
            ?.coerceIn(0, 99)
            ?: 99

        val encodedDelta = deltaMgDl
            ?.let { (DELTA_OFFSET + it).coerceIn(1, 199) }
            ?: DELTA_MISSING

        val trendCode = when (reading.trend) {
            Trend.Unknown -> 0
            Trend.DoubleDown -> 1
            Trend.SingleDown -> 2
            Trend.FortyFiveDown -> 3
            Trend.Flat -> 4
            Trend.FortyFiveUp -> 5
            Trend.SingleUp -> 6
            Trend.DoubleUp -> 7
        }

        val iobUnits = reading.iobUnits
        val iobTimestampMs = reading.iobTimestampMs
        val iobFresh = iobUnits != null &&
            iobTimestampMs != null &&
            !FreshnessPolicy.isStale(iobTimestampMs, nowMs)
        val encodedIob = if (showIob && iobFresh && iobUnits != null) {
            val milliUnits = (iobUnits.coerceIn(0.0, IOB_MAX_UNITS) * 1000.0)
                .roundToInt()
            (IOB_BASE + milliUnits).toFloat()
        } else {
            0f
        }

        return Payload(
            timestampSeconds = (nowMs / 1000L).toInt(),
            location = "BandDrip",
            currentTempKelvin = KELVIN_OFFSET + glucoseMgDl,
            humidity = ageMinutes,
            // Condition is not needed for decoding; keep it meteorologically
            // harmless so Xiaomi does not special-case the payload.
            conditionCode = 800,
            uvIndex = trendCode.toFloat(),
            aqi = encodedDelta,
            pressureMb = encodedIob,
            windSpeedKmh = 0f,
            windDirectionDegrees = 0,
            todayMinTempKelvin = KELVIN_OFFSET + glucoseMgDl,
            todayMaxTempKelvin = KELVIN_OFFSET + glucoseMgDl,
        )
    }

    /** Explicit no-data marker used by diagnostics and future source switching. */
    fun unavailable(nowMs: Long = System.currentTimeMillis()): Payload = Payload(
        timestampSeconds = (nowMs / 1000L).toInt(),
        location = "BandDrip NO DATA",
        currentTempKelvin = KELVIN_OFFSET,
        humidity = 99,
        conditionCode = 804,
        uvIndex = 0f,
        aqi = DELTA_MISSING,
        pressureMb = 0f,
        windSpeedKmh = 0f,
        windDirectionDegrees = 0,
        todayMinTempKelvin = KELVIN_OFFSET,
        todayMaxTempKelvin = KELVIN_OFFSET,
    )

    // Hardware probes are intentionally fresh so a single A/B test verifies
    // glucose, age, delta, trend and the IOB carrier on the real watchface.
    fun probeA(nowMs: Long = System.currentTimeMillis()) = Payload(
        timestampSeconds = (nowMs / 1000L).toInt(),
        location = "BandDrip TEST A",
        currentTempKelvin = KELVIN_OFFSET + 123,
        humidity = 2,
        conditionCode = 800,
        uvIndex = 6f,
        aqi = 155,
        pressureMb = IOB_BASE + 250f,
        windSpeedKmh = 12f,
        windDirectionDegrees = 271,
        todayMinTempKelvin = KELVIN_OFFSET + 111,
        todayMaxTempKelvin = KELVIN_OFFSET + 133,
    )

    fun probeB(nowMs: Long = System.currentTimeMillis()) = Payload(
        timestampSeconds = (nowMs / 1000L).toInt(),
        location = "BandDrip TEST B",
        currentTempKelvin = KELVIN_OFFSET + 124,
        humidity = 3,
        conditionCode = 801,
        uvIndex = 7f,
        aqi = 156,
        pressureMb = IOB_BASE + 275f,
        windSpeedKmh = 20f,
        windDirectionDegrees = 272,
        todayMinTempKelvin = KELVIN_OFFSET + 112,
        todayMaxTempKelvin = KELVIN_OFFSET + 134,
    )
}
