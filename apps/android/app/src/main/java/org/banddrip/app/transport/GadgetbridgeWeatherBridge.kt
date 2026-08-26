package org.banddrip.app.transport

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object GadgetbridgeWeatherBridge {
    const val ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER"
    const val EXTRA_WEATHER_JSON = "WeatherJson"

    // Package names documented by Gadgetbridge for external weather providers.
    private val knownPackages = listOf(
        "nodomain.freeyourgadget.gadgetbridge",
        "nodomain.freeyourgadget.gadgetbridge.nightly",
        "nodomain.freeyourgadget.gadgetbridge.nightly_nopebble",
        "com.espruino.gadgetbridge.banglejs",
        "com.espruino.gadgetbridge.banglejs.nightly",
    )

    data class SendResult(
        val receivers: Int,
        val packages: List<String>,
    )

    @Suppress("DEPRECATION")
    fun send(context: Context, payload: GlucoseWeatherEncoder.Payload): SendResult {
        val installed = knownPackages.filter { packageName ->
            try {
                context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }

        installed.forEach { packageName ->
            // setPackage keeps the broadcast external-app compatible while also
            // satisfying Android 14+'s explicit-intent requirement. Gadgetbridge
            // dynamically registers GenericWeatherReceiver as RECEIVER_EXPORTED
            // while a weather-capable device is initialized.
            val intent = Intent(ACTION_GENERIC_WEATHER)
                .setPackage(packageName)
                .putExtra(EXTRA_WEATHER_JSON, payload.toWeatherJson())
            context.sendBroadcast(intent)
        }

        return SendResult(
            receivers = installed.size,
            packages = installed,
        )
    }
}
