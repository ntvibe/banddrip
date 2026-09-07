package org.banddrip.app.reliability

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object ReliabilityController {
    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java)
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        return manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
            brand.contains("redmi") || brand.contains("poco")
    }

    fun requestBatteryOptimizationExemption(activity: Activity): Boolean = launchFirstAvailable(
        activity,
        listOf(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            },
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        ),
    )

    fun openAppBatterySettings(context: Context): Boolean = launchFirstAvailable(
        context,
        listOf(
            Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        ),
    )

    fun openNotificationSettings(context: Context): Boolean = launchFirstAvailable(
        context,
        listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        ),
    )

    fun openXiaomiAutostart(context: Context): Boolean = launchFirstAvailable(
        context,
        listOf(
            explicit(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity",
            ),
            explicit(
                "com.miui.securitycenter",
                "com.miui.permcenter.permissions.PermissionsEditorActivity",
            ).apply { putExtra("extra_pkgname", context.packageName) },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        ),
    )

    fun openXiaomiBatterySaver(context: Context): Boolean = launchFirstAvailable(
        context,
        listOf(
            explicit(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
            ).apply {
                putExtra("package_name", context.packageName)
                putExtra("package_label", "BandDrip")
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            },
        ),
    )

    private fun explicit(packageName: String, className: String): Intent = Intent().apply {
        component = ComponentName(packageName, className)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun launchFirstAvailable(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            val candidate = Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val canResolve = candidate.resolveActivity(context.packageManager) != null
            if (!canResolve) continue
            if (runCatching { context.startActivity(candidate) }.isSuccess) return true
        }
        return false
    }
}
