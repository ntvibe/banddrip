package org.banddrip.app.install

import android.content.Context
import kotlinx.coroutines.delay

/**
 * Reproduces the known ADB Mi Fitness ThirdAppDebugFragment workflow through
 * Shizuku so a computer is not required after one-time wireless-debugging setup.
 *
 * Every UI action is text/class matched. There are deliberately no fallback
 * screen coordinates: when Mi Fitness changes, automation stops safely.
 */
class MiFitnessAssistedInstaller(private val context: Context) {
    data class Outcome(
        val completedAutomatically: Boolean,
        val stage: String,
        val message: String,
        val stagedRpkPath: String? = null,
    )

    suspend fun install(): Outcome {
        val bundle = runCatching { BandBundleManager.stage(context) }
            .getOrElse { return fail("bundle", it.message ?: "Could not stage bundled RPK") }
        val launcher = bundle.launcherDex
            ?: return fail("bundle", "This APK does not contain the Mi Fitness launcher helper", bundle.rpk.absolutePath)

        val shizuku = ShizukuShell.status(context)
        if (!shizuku.installed) return fail("shizuku", "Install Shizuku first", bundle.rpk.absolutePath)
        if (!shizuku.binderAlive) return fail("shizuku", "Start Shizuku using Wireless debugging", bundle.rpk.absolutePath)
        if (!shizuku.permissionGranted) return fail("shizuku", "Grant BandDrip permission in Shizuku", bundle.rpk.absolutePath)

        val sourceLauncher = shellQuote(BandBundleManager.externalReadablePath(launcher))
        val sourceRpk = shellQuote(BandBundleManager.externalReadablePath(bundle.rpk))
        val deviceLauncher = "/data/local/tmp/banddrip-launch.dex"
        val downloadDir = "/sdcard/Download/BandDrip"
        val deviceRpk = "$downloadDir/org_banddrip_app.rpk"

        shellOrThrow("mkdir", "sh", "-c", "mkdir -p /data/local/tmp '$downloadDir'")
        shellOrThrow("stage launcher", "sh", "-c", "cp $sourceLauncher '$deviceLauncher'")
        shellOrThrow("stage RPK", "sh", "-c", "cp $sourceRpk '$deviceRpk'")

        val apkPathResult = ShizukuShell.run("sh", "-c", "pm path ${BandBundleManager.MI_FITNESS_PACKAGE} | head -n 1 | cut -d: -f2")
        val apkPath = apkPathResult.stdout.trim()
        if (!apkPathResult.ok || apkPath.isBlank()) {
            return fail("mi-fitness", "Mi Fitness is not installed or its APK path could not be resolved", deviceRpk)
        }

        val launch = ShizukuShell.run(
            "sh",
            "-c",
            "CLASSPATH='$deviceLauncher' app_process / LaunchFragment '${shellEscapeSingle(apkPath)}'",
        )
        if (!launch.ok || !launch.stdout.contains("SUCCESS")) {
            val detail = (launch.stderr.ifBlank { launch.stdout }).take(240)
            return fail("mi-fitness", "Could not open Mi Fitness Third App Support: $detail", deviceRpk)
        }

        delay(2_500)
        if (!tapNode(text = "click to input package name")) {
            return partial("package", "Mi Fitness installer opened. Enter package name '${BandBundleManager.BAND_PACKAGE}' manually.", deviceRpk)
        }

        delay(900)
        if (!tapNode(className = "android.widget.EditText")) {
            return partial("package", "Package dialog opened, but BandDrip could not identify its text field.", deviceRpk)
        }

        // Reproduce the community-tested package entry sequence while avoiding
        // keyboard auto-space after dots.
        shellOrThrow("type package", "input", "text", "org")
        shellOrThrow("type package", "input", "keyevent", "56")
        shellOrThrow("type package", "input", "keyevent", "67")
        shellOrThrow("type package", "input", "text", "banddrip")
        shellOrThrow("type package", "input", "keyevent", "56")
        shellOrThrow("type package", "input", "keyevent", "67")
        shellOrThrow("type package", "input", "text", "app")

        delay(400)
        if (!tapNode(text = "OK")) {
            return partial("package", "Package name was entered. Tap OK manually.", deviceRpk)
        }

        delay(1_000)
        if (!tapNode(text = "install third app")) {
            return partial("install", "Package is configured. Tap 'install third app' manually.", deviceRpk)
        }

        delay(1_800)
        // Android's document picker often exposes the staged file immediately in
        // Recents. If it does not, stop with the picker already open.
        if (!tapNode(text = "org_banddrip_app.rpk")) {
            return partial(
                "file-picker",
                "File picker is open. Select Download/BandDrip/org_banddrip_app.rpk to finish.",
                deviceRpk,
            )
        }

        return Outcome(
            completedAutomatically = true,
            stage = "submitted",
            message = "RPK selected. Mi Fitness should now transfer BandDrip to the band; verify its success message on the phone/band.",
            stagedRpkPath = deviceRpk,
        )
    }

    private suspend fun tapNode(text: String? = null, className: String? = null): Boolean {
        val xml = uiDump() ?: return false
        val bounds = findNodeBounds(xml, text, className) ?: return false
        val x = (bounds[0] + bounds[2]) / 2
        val y = (bounds[1] + bounds[3]) / 2
        val tap = ShizukuShell.run("input", "tap", x.toString(), y.toString())
        return tap.ok
    }

    private suspend fun uiDump(): String? {
        val result = ShizukuShell.run(
            "sh",
            "-c",
            "uiautomator dump /sdcard/banddrip-ui.xml >/dev/null 2>&1 && cat /sdcard/banddrip-ui.xml",
        )
        return result.stdout.takeIf { result.ok && it.contains("<hierarchy") }
    }

    internal fun findNodeBounds(xml: String, text: String?, className: String?): IntArray? {
        val nodes = Regex("<node\\b[^>]+/>").findAll(xml)
        for (match in nodes) {
            val node = match.value
            if (text != null && attribute(node, "text") != text) continue
            if (className != null && attribute(node, "class") != className) continue
            val raw = attribute(node, "bounds") ?: continue
            val bounds = Regex("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]").matchEntire(raw) ?: continue
            return IntArray(4) { index -> bounds.groupValues[index + 1].toInt() }
        }
        return null
    }

    private fun attribute(node: String, name: String): String? =
        Regex("\\b${Regex.escape(name)}=\"([^\"]*)\"").find(node)?.groupValues?.get(1)

    private suspend fun shellOrThrow(label: String, vararg command: String) {
        val result = ShizukuShell.run(*command)
        if (!result.ok) {
            error("$label failed: ${result.stderr.ifBlank { result.stdout }.take(220)}")
        }
    }

    private fun partial(stage: String, message: String, path: String) = Outcome(
        completedAutomatically = false,
        stage = stage,
        message = message,
        stagedRpkPath = path,
    )

    private fun fail(stage: String, message: String, path: String? = null) = Outcome(
        completedAutomatically = false,
        stage = stage,
        message = message,
        stagedRpkPath = path,
    )

    private fun shellQuote(value: String): String = "'${shellEscapeSingle(value)}'"
    private fun shellEscapeSingle(value: String): String = value.replace("'", "'\\''")
}
