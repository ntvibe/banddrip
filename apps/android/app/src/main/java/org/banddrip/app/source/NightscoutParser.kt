package org.banddrip.app.source

import java.time.Instant
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.json.JSONArray
import org.json.JSONObject

internal object NightscoutParser {
    data class IobSample(val units: Double, val timestampMs: Long)

    fun parseReading(entriesJson: String, iobJson: String? = null): BandDripReading? {
        val entries = JSONArray(entriesJson)
        if (entries.length() == 0) return null

        val latest = entries.getJSONObject(0)
        val glucose = latest.optDouble("sgv", Double.NaN)
        if (!glucose.isFinite()) return null

        val timestamp = timestampMs(latest) ?: return null
        val previous = if (entries.length() > 1) entries.optJSONObject(1) else null
        val previousGlucose = previous?.optDouble("sgv", Double.NaN)?.takeIf { it.isFinite() }
        val delta = previousGlucose?.let { glucose - it }
        val iob = iobJson?.let(::parseIob)

        return BandDripReading(
            glucose = glucose,
            units = GlucoseUnits.MgDl,
            glucoseTimestampMs = timestamp,
            delta = delta,
            trend = trend(latest.optString("direction", "")),
            iobUnits = iob?.units,
            iobTimestampMs = iob?.timestampMs,
            source = "nightscout",
        )
    }

    fun parseIob(devicestatusJson: String): IobSample? {
        val statuses = JSONArray(devicestatusJson)
        if (statuses.length() == 0) return null
        val status = statuses.getJSONObject(0)
        val createdAt = parseInstant(status.optString("created_at", "")) ?: return null

        val candidates = listOfNotNull(
            childObject(status, "openaps")?.let { findNumericIob(it) },
            childObject(status, "loop")?.let { findNumericIob(it) },
            childObject(status, "pump")?.let { findNumericIob(it) },
        )
        val value = candidates.firstOrNull { it.isFinite() && it >= 0.0 } ?: return null
        return IobSample(value, createdAt)
    }

    private fun childObject(parent: JSONObject, key: String): JSONObject? {
        val value = parent.opt(key) ?: return null
        return when (value) {
            is JSONObject -> value
            is String -> runCatching { JSONObject(value) }.getOrNull()
            else -> null
        }
    }

    private fun findNumericIob(node: JSONObject): Double? {
        if (node.has("iob")) {
            when (val value = node.opt("iob")) {
                is Number -> return value.toDouble()
                is JSONObject -> {
                    val nested = value.optDouble("iob", Double.NaN)
                    if (nested.isFinite()) return nested
                }
                is String -> value.toDoubleOrNull()?.let { return it }
            }
        }

        val keys = node.keys()
        while (keys.hasNext()) {
            val value = node.opt(keys.next())
            if (value is JSONObject) findNumericIob(value)?.let { return it }
        }
        return null
    }

    private fun timestampMs(entry: JSONObject): Long? {
        val epoch = entry.optLong("date", 0L)
        if (epoch > 0L) return epoch
        return parseInstant(entry.optString("dateString", ""))
    }

    private fun parseInstant(value: String): Long? =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()

    private fun trend(direction: String): Trend = when (direction.trim().lowercase()) {
        "doubleup" -> Trend.DoubleUp
        "singleup" -> Trend.SingleUp
        "fortyfiveup" -> Trend.FortyFiveUp
        "flat" -> Trend.Flat
        "fortyfivedown" -> Trend.FortyFiveDown
        "singledown" -> Trend.SingleDown
        "doubledown" -> Trend.DoubleDown
        else -> Trend.Unknown
    }
}
