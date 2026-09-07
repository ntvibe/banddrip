package org.banddrip.app.xdrip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.banddrip.app.config.AppSettingsStore
import org.banddrip.app.config.SourceMode
import org.banddrip.app.core.RelayStateStore
import org.banddrip.app.service.BandDripRelayService
import org.banddrip.app.source.XDripReadingStore

class XDripBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NEW_BG_ESTIMATE) return

        val glucose = intent.getDoubleExtra(EXTRA_BG_ESTIMATE, Double.NaN)
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, 0L)
        val slopeName = intent.getStringExtra(EXTRA_BG_SLOPE_NAME)
        val sourceDescription = intent.getStringExtra(EXTRA_SOURCE_DESCRIPTION)

        val reading = XDripReadingStore(context).ingest(
            glucoseMgDl = glucose,
            timestampMs = timestamp,
            slopeName = slopeName,
            sourceDescription = sourceDescription,
        ) ?: return

        RelayStateStore(context).saveReading(reading, "xDrip broadcast received")
        val settings = AppSettingsStore(context).load()
        if (settings.backgroundEnabled && settings.sourceMode == SourceMode.XDrip) {
            runCatching {
                val serviceIntent = Intent(context, BandDripRelayService::class.java)
                    .setAction(BandDripRelayService.ACTION_PROCESS_XDRIP)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }.onFailure {
                RelayStateStore(context).setStatus(
                    "xDrip received; Android blocked relay restart. Open BandDrip reliability setup.",
                )
            }
        }
    }

    companion object {
        const val RECEIVER_PERMISSION = "com.eveningoutpost.dexdrip.permissions.RECEIVE_BG_ESTIMATE"
        const val ACTION_NEW_BG_ESTIMATE = "com.eveningoutpost.dexdrip.BgEstimate"
        const val EXTRA_BG_ESTIMATE = "com.eveningoutpost.dexdrip.Extras.BgEstimate"
        const val EXTRA_BG_SLOPE_NAME = "com.eveningoutpost.dexdrip.Extras.BgSlopeName"
        const val EXTRA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.Time"
        const val EXTRA_SOURCE_DESCRIPTION = "com.eveningoutpost.dexdrip.Extras.SourceDesc"
    }
}
