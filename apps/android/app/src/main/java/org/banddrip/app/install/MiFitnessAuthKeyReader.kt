package org.banddrip.app.install

import android.content.Context
import org.banddrip.app.config.AppSettingsStore

/**
 * Account-free AuthKey extraction for a band already paired with Mi Fitness.
 *
 * Mi Fitness writes the BLE authentication key to its external-storage logs.
 * Android's app sandbox normally prevents BandDrip from reading another app's
 * Android/data directory directly, so this helper uses the optional Shizuku
 * shell bridge. The key never leaves the phone and is stored encrypted through
 * AppSettingsStore/Android Keystore.
 */
object MiFitnessAuthKeyReader {
    data class Outcome(
        val found: Boolean,
        val message: String,
        val candidateCount: Int = 0,
    )

    private val logPaths = listOf(
        "/sdcard/Android/data/com.mi.health/files/log/XiaomiFit.main.log",
        "/sdcard/Android/data/com.mi.health/files/log/XiaomiFit.device.log",
        "/sdcard/Android/data/com.mi.health/files/log/Transfer.device.log",
        "/sdcard/Android/data/com.xiaomi.wearable/files/log/XiaomiFit.main.log",
        "/sdcard/Android/data/com.xiaomi.wearable/files/log/XiaomiFit.device.log",
        "/sdcard/Android/data/com.xiaomi.wearable/files/log/Transfer.device.log",
    )

    suspend fun readAndStore(context: Context): Outcome {
        val status = ShizukuShell.status(context)
        if (!status.installed) return Outcome(false, "Install Shizuku first")
        if (!status.binderAlive) return Outcome(false, "Start Shizuku using Wireless debugging")
        if (!status.permissionGranted) return Outcome(false, "Grant BandDrip Shizuku permission first")

        // Read only the tail of each current log. Auth keys are repeated in Mi
        // Fitness logs and the latest occurrence is the most relevant one.
        // Fixed paths only: no user-controlled shell text is interpolated.
        val script = buildString {
            append("for f in ")
            append(logPaths.joinToString(" ") { "'$it'" })
            append("; do if [ -f \"$f\" ]; then echo '__BANDDRIP_LOG__'\"$f\"; tail -c 8388608 \"$f\"; fi; done")
        }

        val shell = runCatching { ShizukuShell.run("sh", "-c", script) }
            .getOrElse { return Outcome(false, "Could not read Mi Fitness logs: ${safeMessage(it)}") }
        if (!shell.ok) {
            return Outcome(false, "Mi Fitness log read failed: ${shell.stderr.ifBlank { shell.stdout }.take(220)}")
        }

        val candidates = parseAuthKeys(shell.stdout)
        if (candidates.isEmpty()) {
            return Outcome(
                found = false,
                message = "No 32-character AuthKey found yet. Open Mi Fitness, sync the band once, then retry.",
            )
        }

        // Gadgetbridge and community research recommend the latest key in the
        // log when multiple historical entries exist. Distinct keys are counted
        // so the UI can tell the user if the log contained historical devices.
        val distinct = candidates.distinct()
        val latest = candidates.last()
        AppSettingsStore(context).saveBandAuthKey(latest)

        return Outcome(
            found = true,
            candidateCount = distinct.size,
            message = if (distinct.size == 1) {
                "AuthKey found ✓ · saved encrypted on this phone"
            } else {
                "AuthKey found ✓ · ${distinct.size} historical keys seen · latest saved encrypted"
            },
        )
    }

    internal fun parseAuthKeys(text: String): List<String> {
        val fieldPattern = Regex(
            "(?i)(?:encryptKey|token|authKey|huamiAuthKey)[\\\"':=\\s]+([0-9a-f]{32})(?![0-9a-f])",
        )
        return fieldPattern.findAll(text)
            .map { it.groupValues[1].lowercase() }
            .toList()
    }

    private fun safeMessage(error: Throwable): String =
        error.message?.trim()?.takeIf { it.isNotBlank() }?.take(220) ?: error::class.java.simpleName
}
