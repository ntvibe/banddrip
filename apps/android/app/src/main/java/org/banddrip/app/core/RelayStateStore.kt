package org.banddrip.app.core

import android.content.Context
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.json.JSONObject

class RelayStateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveReading(reading: BandDripReading, status: String? = null) {
        prefs.edit()
            .putString(KEY_READING, encode(reading))
            .putLong(KEY_READING_SAVED_AT, System.currentTimeMillis())
            .apply {
                if (status != null) putString(KEY_STATUS, status)
            }
            .apply()
    }

    fun loadReading(): BandDripReading? = prefs.getString(KEY_READING, null)?.let(::decode)

    fun savePacket(packet: String) {
        prefs.edit().putString(KEY_PACKET, packet).putLong(KEY_PACKET_AT, System.currentTimeMillis()).apply()
    }

    fun lastPacket(): String? = prefs.getString(KEY_PACKET, null)

    fun setStatus(status: String) {
        prefs.edit().putString(KEY_STATUS, status).apply()
    }

    fun status(): String = prefs.getString(KEY_STATUS, "Ready").orEmpty()

    fun setServiceRunning(running: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, running).apply()
    }

    fun isServiceRunning(): Boolean = prefs.getBoolean(KEY_SERVICE_RUNNING, false)

    private fun encode(reading: BandDripReading): String = JSONObject()
        .put("glucose", reading.glucose)
        .put("units", reading.units.wireValue)
        .put("timestamp", reading.glucoseTimestampMs)
        .put("delta", reading.delta ?: JSONObject.NULL)
        .put("trend", reading.trend.wireValue)
        .put("iob", reading.iobUnits ?: JSONObject.NULL)
        .put("iobTimestamp", reading.iobTimestampMs ?: JSONObject.NULL)
        .put("sequence", reading.sequence ?: JSONObject.NULL)
        .put("source", reading.source ?: JSONObject.NULL)
        .toString()

    private fun decode(raw: String): BandDripReading? = runCatching {
        val json = JSONObject(raw)
        val units = GlucoseUnits.entries.firstOrNull { it.wireValue == json.getString("units") } ?: GlucoseUnits.MgDl
        val trend = Trend.entries.firstOrNull { it.wireValue == json.getString("trend") } ?: Trend.Unknown
        BandDripReading(
            glucose = json.getDouble("glucose"),
            units = units,
            glucoseTimestampMs = json.getLong("timestamp"),
            delta = json.optNullableDouble("delta"),
            trend = trend,
            iobUnits = json.optNullableDouble("iob"),
            iobTimestampMs = json.optNullableLong("iobTimestamp"),
            sequence = json.optNullableLong("sequence"),
            source = json.optNullableString("source"),
        )
    }.getOrNull()

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else getDouble(key)

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else getLong(key)

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else getString(key)

    companion object {
        private const val PREFS_NAME = "banddrip-relay-state-v1"
        private const val KEY_READING = "reading"
        private const val KEY_READING_SAVED_AT = "reading-saved-at"
        private const val KEY_PACKET = "packet"
        private const val KEY_PACKET_AT = "packet-at"
        private const val KEY_STATUS = "status"
        private const val KEY_SERVICE_RUNNING = "service-running"
    }
}
