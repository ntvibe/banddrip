package org.banddrip.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.banddrip.app.config.AppSettingsStore
import org.banddrip.app.core.RelayStateStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val supported = intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (!supported) return

        if (!AppSettingsStore(context).load().backgroundEnabled) return
        runCatching {
            BandDripRelayService.start(context)
        }.onFailure {
            RelayStateStore(context).setStatus(
                "Relay restart was blocked after boot; open BandDrip to restore it.",
            )
        }
    }
}
