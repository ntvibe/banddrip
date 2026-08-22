package org.banddrip.app.install

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.InputStream

/**
 * Minimal shell bridge for the optional assisted Mi Fitness installer.
 *
 * Shizuku is never required for normal BandDrip glucose relay operation. It is
 * used only to reproduce the shell/ADB installer workflow without a computer.
 */
object ShizukuShell {
    const val PERMISSION_REQUEST_CODE = 8204

    data class Status(
        val installed: Boolean,
        val binderAlive: Boolean,
        val permissionGranted: Boolean,
    )

    data class Result(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    ) {
        val ok: Boolean get() = exitCode == 0
    }

    fun status(context: Context): Status {
        val installed = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(BandBundleManager.SHIZUKU_PACKAGE, 0)
        }.isSuccess
        val alive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val granted = alive && runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        return Status(installed, alive, granted)
    }

    fun requestPermission(): Result<Unit> = runCatching {
        check(Shizuku.pingBinder()) { "Shizuku is not running" }
        Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    fun openManager(context: Context): Result<Unit> = runCatching {
        val launch = context.packageManager.getLaunchIntentForPackage(BandBundleManager.SHIZUKU_PACKAGE)
            ?: error("Shizuku is not installed")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
    }

    fun openDownloadPage(context: Context): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun run(vararg command: String): Result = withContext(Dispatchers.IO) {
        check(Shizuku.pingBinder()) { "Shizuku is not running" }
        check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            "BandDrip has not been granted Shizuku permission"
        }

        // newProcess is private/deprecated in API 13 but remains the smallest
        // shell-transition path. Keep it isolated here so migration to a
        // Shizuku UserService does not touch installer logic.
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        ).apply { isAccessible = true }

        val remote = method.invoke(null, command, null, null)
            ?: error("Shizuku did not create a shell process")
        val remoteClass = remote.javaClass
        val stdoutStream = remoteClass.getMethod("getInputStream").invoke(remote) as InputStream
        val stderrStream = remoteClass.getMethod("getErrorStream").invoke(remote) as InputStream

        coroutineScope {
            val stdout = async(Dispatchers.IO) { stdoutStream.bufferedReader().use { it.readText() } }
            val stderr = async(Dispatchers.IO) { stderrStream.bufferedReader().use { it.readText() } }
            val exitCode = remoteClass.getMethod("waitFor").invoke(remote) as Int
            Result(
                exitCode = exitCode,
                stdout = stdout.await().trim(),
                stderr = stderr.await().trim(),
            )
        }
    }
}
