package org.banddrip.app.transport

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object GadgetbridgeWeatherBridge {
    const val ACTION_GENERIC_WEATHER = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER"
    const val EXTRA_WEATHER_JSON = "WeatherJson"

    data class SendResult(
        val receivers: Int,
        val packages: List<String>,
    )

    @Suppress("DEPRECATION")
    fun send(context: Context, payload: GlucoseWeatherEncoder.Payload): SendResult {
        val queryIntent = Intent(ACTION_GENERIC_WEATHER)
        val matches = context.packageManager.queryBroadcastReceivers(queryIntent, PackageManager.MATCH_DEFAULT_ONLY)

        val packages = mutableListOf<String>()
        matches.forEach { match ->
            val info = match.activityInfo ?: return@forEach
            val explicit = Intent(ACTION_GENERIC_WEATHER)
                .setComponent(ComponentName(info.packageName, info.name))
                .putExtra(EXTRA_WEATHER_JSON, payload.toWeatherJson())
            context.sendBroadcast(explicit)
            packages += info.packageName
        }

        return SendResult(
            receivers = packages.size,
            packages = packages.distinct(),
        )
    }
}
