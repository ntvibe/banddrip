package org.banddrip.app.install

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

/**
 * Owns the install payloads bundled into the Android APK by CI.
 *
 * The Android app never downloads an RPK from an arbitrary remote URL at runtime.
 * The wearable payload is built from the same repository revision, bundled into
 * the APK, and can be staged for Mi Fitness / Shizuku installation.
 */
object BandBundleManager {
    const val BAND_PACKAGE = "org.banddrip.app"
    const val MI_FITNESS_PACKAGE = "com.xiaomi.wearable"
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    private const val RPK_ASSET = "bundled/banddrip.rpk"
    private const val LAUNCHER_ASSET = "bundled/launch.dex"
    private const val STAGED_RPK_NAME = "org_banddrip_app.rpk"
    private const val STAGED_LAUNCHER_NAME = "banddrip-launch.dex"

    data class BundleStatus(
        val rpkBundled: Boolean,
        val launcherBundled: Boolean,
        val miFitnessInstalled: Boolean,
        val shizukuInstalled: Boolean,
    )

    data class StagedPayload(
        val rpk: File,
        val launcherDex: File?,
        val rpkSha256: String,
    )

    fun status(context: Context): BundleStatus = BundleStatus(
        rpkBundled = hasAsset(context, RPK_ASSET),
        launcherBundled = hasAsset(context, LAUNCHER_ASSET),
        miFitnessInstalled = isPackageInstalled(context.packageManager, MI_FITNESS_PACKAGE),
        shizukuInstalled = isPackageInstalled(context.packageManager, SHIZUKU_PACKAGE),
    )

    fun stage(context: Context): StagedPayload {
        require(hasAsset(context, RPK_ASSET)) {
            "This APK does not contain a BandDrip RPK. Install a CI/release build that bundles the wearable app."
        }

        val root = File(context.getExternalFilesDir(null) ?: context.filesDir, "band-install").apply {
            mkdirs()
        }
        val rpk = File(root, STAGED_RPK_NAME)
        copyAsset(context, RPK_ASSET, rpk)

        val launcher = if (hasAsset(context, LAUNCHER_ASSET)) {
            File(root, STAGED_LAUNCHER_NAME).also { copyAsset(context, LAUNCHER_ASSET, it) }
        } else {
            null
        }

        return StagedPayload(
            rpk = rpk,
            launcherDex = launcher,
            rpkSha256 = sha256(rpk),
        )
    }

    fun launchMiFitness(context: Context): Result<Unit> = runCatching {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(MI_FITNESS_PACKAGE)
            ?: error("Mi Fitness is not installed")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }

    /**
     * Best-effort public intent path. Stock Mi Fitness generally does not expose a
     * public RPK installer; this method is intentionally capability-based and
     * returns failure instead of pretending installation was started.
     */
    fun tryOpenRpkWithMiFitness(context: Context, staged: StagedPayload): Result<Unit> = runCatching {
        val uri = contentUri(context, staged.rpk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/octet-stream")
            setPackage(MI_FITNESS_PACKAGE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = intent.resolveActivity(context.packageManager)
            ?: error("Stock Mi Fitness does not expose a public RPK file handler")
        context.grantUriPermission(MI_FITNESS_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.component = resolved
        context.startActivity(intent)
    }

    fun shareRpk(context: Context, staged: StagedPayload): Result<Unit> = runCatching {
        val uri = contentUri(context, staged.rpk)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Install BandDrip wearable app").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun externalReadablePath(file: File): String = file.absolutePath

    private fun contentUri(context: Context, file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.files",
        file,
    )

    private fun hasAsset(context: Context, path: String): Boolean = runCatching {
        context.assets.open(path).use { }
    }.isSuccess

    private fun copyAsset(context: Context, assetPath: String, destination: File) {
        destination.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }.isSuccess
}
