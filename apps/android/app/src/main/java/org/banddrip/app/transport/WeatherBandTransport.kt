package org.banddrip.app.transport

import android.content.Context
import org.banddrip.app.model.BandDripReading

/**
 * Stock-firmware Smart Band 10 Pro NFC transport.
 *
 * BandDrip acts as a Gadgetbridge external weather provider. Gadgetbridge owns
 * the authenticated Xiaomi connection; this transport only submits the encoded
 * glucose payload to Gadgetbridge's exported GenericWeatherReceiver.
 */
class WeatherBandTransport(
    context: Context,
    private val packetSink: (String) -> Unit = {},
) : BandTransport {
    override val id: String = "gadgetbridge-weather"

    private val appContext = context.applicationContext

    @Volatile
    private var showIob: Boolean = true

    override suspend fun sendSettings(showIob: Boolean) {
        this.showIob = showIob
    }

    override suspend fun sendReading(reading: BandDripReading) {
        sendPayload(GlucoseWeatherEncoder.encode(reading, showIob = showIob))
    }

    suspend fun sendUnavailable() {
        sendPayload(GlucoseWeatherEncoder.unavailable())
    }

    private fun sendPayload(payload: GlucoseWeatherEncoder.Payload) {
        val result = GadgetbridgeWeatherBridge.send(appContext, payload)
        check(result.receivers > 0) {
            "Gadgetbridge is not installed or no supported Gadgetbridge package is visible"
        }

        packetSink(
            buildString {
                append(payload.toWeatherJson())
                append("\n# receivers=")
                append(result.packages.joinToString(","))
            },
        )
    }
}
