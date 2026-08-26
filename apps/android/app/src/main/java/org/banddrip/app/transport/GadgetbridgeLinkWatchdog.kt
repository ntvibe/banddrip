package org.banddrip.app.transport

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.content.ContextCompat

/**
 * Small resilience layer around Gadgetbridge's documented Bluetooth Intent API.
 *
 * Gadgetbridge owns the Xiaomi SPP/RFCOMM socket. BandDrip only learns the most
 * recently initialized device address from Gadgetbridge's BLUETOOTH_CONNECTED
 * broadcast and, while the relay is active, periodically asks Gadgetbridge to
 * ensure that device is connected. If Gadgetbridge is already connected, its
 * command handler is a no-op and re-emits BLUETOOTH_CONNECTED.
 *
 * Gadgetbridge must have Developer options -> Intent API enabled. The address is
 * persisted locally on the phone and is never hardcoded into the public repo.
 */
class GadgetbridgeLinkWatchdog(
    context: Context,
    private val onEvent: (String) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var registered = false

    @Volatile
    private var lastKickElapsedMs = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_CONNECTED) return
            val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)?.trim().orEmpty()
            if (!isBluetoothAddress(address)) return

            prefs.edit()
                .putString(KEY_DEVICE_ADDRESS, address.uppercase())
                .putLong(KEY_LAST_CONFIRMED_AT_MS, System.currentTimeMillis())
                .apply()
            onEvent("Gadgetbridge link confirmed")
        }
    }

    fun start() {
        if (registered) return
        val filter = IntentFilter(ACTION_CONNECTED)
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        registered = true
    }

    fun stop() {
        if (!registered) return
        runCatching { appContext.unregisterReceiver(receiver) }
        registered = false
    }

    /**
     * Ask Gadgetbridge to connect the last device it confirmed to us.
     * Calls are throttled because the relay loop may run every few seconds.
     */
    fun ensureConnected(force: Boolean = false): KickResult {
        val address = knownAddress() ?: return KickResult.NoKnownDevice
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastKickElapsedMs < MIN_KICK_INTERVAL_MS) {
            return KickResult.Throttled
        }
        lastKickElapsedMs = now

        val installed = knownPackages.filter(::isInstalled)
        if (installed.isEmpty()) return KickResult.NoGadgetbridge

        installed.forEach { packageName ->
            appContext.sendBroadcast(
                Intent(ACTION_CONNECT)
                    .setPackage(packageName)
                    .putExtra(EXTRA_DEVICE_ADDRESS, address),
            )
        }
        onEvent("Asked Gadgetbridge to ensure band connection")
        return KickResult.Sent(address, installed)
    }

    fun knownAddress(): String? = prefs.getString(KEY_DEVICE_ADDRESS, null)
        ?.trim()
        ?.takeIf(::isBluetoothAddress)

    fun lastConfirmedAtMs(): Long = prefs.getLong(KEY_LAST_CONFIRMED_AT_MS, 0L)

    @Suppress("DEPRECATION")
    private fun isInstalled(packageName: String): Boolean = try {
        appContext.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    sealed interface KickResult {
        data object NoKnownDevice : KickResult
        data object NoGadgetbridge : KickResult
        data object Throttled : KickResult
        data class Sent(val address: String, val packages: List<String>) : KickResult
    }

    companion object {
        const val ACTION_CONNECT = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_CONNECT"
        const val ACTION_CONNECTED = "nodomain.freeyourgadget.gadgetbridge.BLUETOOTH_CONNECTED"
        const val EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS"

        private const val PREFS_NAME = "banddrip-gadgetbridge-link"
        private const val KEY_DEVICE_ADDRESS = "device-address"
        private const val KEY_LAST_CONFIRMED_AT_MS = "last-confirmed-at-ms"
        private const val MIN_KICK_INTERVAL_MS = 25_000L

        private val knownPackages = listOf(
            "nodomain.freeyourgadget.gadgetbridge",
            "nodomain.freeyourgadget.gadgetbridge.nightly",
            "nodomain.freeyourgadget.gadgetbridge.nightly_nopebble",
        )

        private val btAddressRegex = Regex("(?i)^[0-9a-f]{2}(:[0-9a-f]{2}){5}$")

        internal fun isBluetoothAddress(value: String): Boolean = btAddressRegex.matches(value)
    }
}
