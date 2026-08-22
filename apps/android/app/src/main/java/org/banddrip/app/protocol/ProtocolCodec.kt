package org.banddrip.app.protocol

import org.banddrip.app.model.BandDripReading
import org.json.JSONObject

object ProtocolCodec {
    const val PROTOCOL_VERSION = 1

    fun encodeReading(reading: BandDripReading): String {
        val json = JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("type", "reading")
            .put("glucose", reading.glucose)
            .put("units", reading.units.wireValue)
            .put("glucoseTimestampMs", reading.glucoseTimestampMs)
            .put("trend", reading.trend.wireValue)

        putNullable(json, "delta", reading.delta)
        putNullable(json, "iobUnits", reading.iobUnits)
        putNullable(json, "iobTimestampMs", reading.iobTimestampMs)
        putNullable(json, "sequence", reading.sequence)
        putNullable(json, "source", reading.source)
        return json.toString()
    }

    fun encodeSettings(showIob: Boolean): String = JSONObject()
        .put("protocolVersion", PROTOCOL_VERSION)
        .put("type", "settings")
        .put("showIob", showIob)
        .toString()

    private fun putNullable(json: JSONObject, key: String, value: Any?) {
        json.put(key, value ?: JSONObject.NULL)
    }
}
