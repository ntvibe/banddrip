package org.banddrip.app.source

import android.content.Context
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.json.JSONObject

class XDripSource(context: Context) : GlucoseSource {
    private val store = XDripReadingStore(context)
    override val id: String = "xdrip"

    override suspend fun latestReading(): BandDripReading? = store.latest()
}

class XDripReadingStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ingest(
        glucoseMgDl: Double,
        timestampMs: Long,
        slopeName: String?,
        sourceDescription: String?,
    ): BandDripReading? {
        if (!glucoseMgDl.isFinite() || glucoseMgDl <= 0.0 || timestampMs <= 0L) return null
        val previous = latest()
        val delta = previous
            ?.takeIf { it.glucoseTimestampMs < timestampMs && timestampMs - it.glucoseTimestampMs <= 20 * 60_000L }
            ?.let { glucoseMgDl - it.glucose }

        val reading = BandDripReading(
            glucose = glucoseMgDl,
            units = GlucoseUnits.MgDl,
            glucoseTimestampMs = timestampMs,
            delta = delta,
            trend = mapTrend(slopeName),
            iobUnits = null,
            iobTimestampMs = null,
            sequence = timestampMs,
            source = sourceDescription?.takeIf { it.isNotBlank() }?.let { "xdrip:$it" } ?: "xdrip",
        )
        prefs.edit().putString(KEY_LATEST, encode(reading)).apply()
        return reading
    }

    fun latest(): BandDripReading? = prefs.getString(KEY_LATEST, null)?.let(::decode)

    fun lastReceivedAtMs(): Long? = latest()?.glucoseTimestampMs

    private fun mapTrend(raw: String?): Trend {
        val normalized = raw.orEmpty().lowercase().filter { it.isLetterOrDigit() }
        return when (normalized) {
            "doubleup" -> Trend.DoubleUp
            "singleup" -> Trend.SingleUp
            "fortyfiveup", "45up" -> Trend.FortyFiveUp
            "flat" -> Trend.Flat
            "fortyfivedown", "45down" -> Trend.FortyFiveDown
            "singledown" -> Trend.SingleDown
            "doubledown" -> Trend.DoubleDown
            else -> Trend.Unknown
        }
    }

    private fun encode(reading: BandDripReading): String = JSONObject()
        .put("glucose", reading.glucose)
        .put("timestamp", reading.glucoseTimestampMs)
        .put("delta", reading.delta ?: JSONObject.NULL)
        .put("trend", reading.trend.wireValue)
        .put("source", reading.source ?: "xdrip")
        .toString()

    private fun decode(raw: String): BandDripReading? = runCatching {
        val json = JSONObject(raw)
        BandDripReading(
            glucose = json.getDouble("glucose"),
            units = GlucoseUnits.MgDl,
            glucoseTimestampMs = json.getLong("timestamp"),
            delta = if (json.isNull("delta")) null else json.getDouble("delta"),
            trend = Trend.entries.firstOrNull { it.wireValue == json.optString("trend") } ?: Trend.Unknown,
            source = json.optString("source", "xdrip"),
        )
    }.getOrNull()

    companion object {
        private const val PREFS_NAME = "banddrip-xdrip-v1"
        private const val KEY_LATEST = "latest"
    }
}
