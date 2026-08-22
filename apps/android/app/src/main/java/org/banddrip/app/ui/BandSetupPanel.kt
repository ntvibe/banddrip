package org.banddrip.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.banddrip.app.install.BandBundleManager
import org.banddrip.app.install.MiFitnessAssistedInstaller
import org.banddrip.app.install.ShizukuShell

@Composable
fun BandSetupPanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var installStatus by remember { mutableStateOf("Ready to inspect setup") }
    var stagedHash by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            tick += 1
            delay(2_000)
        }
    }

    val bundle = remember(tick) { BandBundleManager.status(context) }
    val shizuku = remember(tick) { ShizukuShell.status(context) }

    Column(verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(9f))) {
        Text("Band setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            "The Android APK carries its matching Vela RPK. BandDrip chooses the least-friction installer path available on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SetupRow("Wearable RPK bundled", bundle.rpkBundled)
        SetupRow("Installer helper bundled", bundle.launcherBundled)
        SetupRow("Mi Fitness installed", bundle.miFitnessInstalled)
        SetupRow("Shizuku running", shizuku.binderAlive)
        SetupRow("BandDrip Shizuku permission", shizuku.permissionGranted)

        Button(
            enabled = !busy && bundle.rpkBundled && bundle.miFitnessInstalled,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true
                installStatus = "Preparing BandDrip wearable payload…"
                scope.launch {
                    val result = runCatching {
                        val staged = BandBundleManager.stage(context)
                        stagedHash = staged.rpkSha256.take(12)

                        // Prefer a genuine public Mi Fitness file handler if Xiaomi
                        // ever exposes one. Current stock builds normally do not.
                        val publicPath = BandBundleManager.tryOpenRpkWithMiFitness(context, staged)
                        if (publicPath.isSuccess) {
                            "Mi Fitness accepted the bundled RPK through its public file handler."
                        } else if (ShizukuShell.status(context).permissionGranted) {
                            val outcome = MiFitnessAssistedInstaller(context).install()
                            outcome.message
                        } else {
                            "RPK staged. For one-button installation, start Shizuku and grant BandDrip permission; otherwise use the manual RPK fallback below."
                        }
                    }
                    installStatus = result.getOrElse {
                        "Install setup failed safely: ${safeMessage(it)}"
                    }
                    busy = false
                    tick += 1
                }
            },
        ) {
            Text(if (busy) "Installing…" else "Install BandDrip on band")
        }

        if (!shizuku.installed) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    installStatus = ShizukuShell.openDownloadPage(context)
                        .fold({ "Opened Shizuku download page" }, { "Could not open Shizuku page: ${safeMessage(it)}" })
                },
            ) { Text("Get Shizuku for one-button install") }
        } else if (!shizuku.binderAlive) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    installStatus = ShizukuShell.openManager(context)
                        .fold({ "Open Shizuku and start it with Wireless debugging" }, { "Could not open Shizuku: ${safeMessage(it)}" })
                },
            ) { Text("Open / start Shizuku") }
        } else if (!shizuku.permissionGranted) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    installStatus = ShizukuShell.requestPermission()
                        .fold({ "Shizuku permission request opened" }, { "Permission request failed: ${safeMessage(it)}" })
                },
            ) { Text("Grant BandDrip Shizuku permission") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(8f))) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    installStatus = BandBundleManager.launchMiFitness(context)
                        .fold({ "Mi Fitness opened" }, { "Could not open Mi Fitness: ${safeMessage(it)}" })
                },
            ) { Text("Mi Fitness") }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = bundle.rpkBundled,
                onClick = {
                    installStatus = runCatching {
                        val staged = BandBundleManager.stage(context)
                        stagedHash = staged.rpkSha256.take(12)
                        BandBundleManager.shareRpk(context, staged).getOrThrow()
                        "RPK share/install chooser opened"
                    }.getOrElse { "Could not stage/share RPK: ${safeMessage(it)}" }
                },
            ) { Text("RPK fallback") }
        }

        Text(
            installStatus + (stagedHash?.let { " · RPK $it…" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Watch-face glance mode is being developed as a separate Band 10 home-surface package. It will be bundled here too once the live-data bridge is proven; the app will not claim it works before hardware/runtime validation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SetupRow(label: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(if (ok) "OK ✓" else "NOT READY", fontWeight = FontWeight.SemiBold)
    }
}

private fun safeMessage(error: Throwable): String =
    error.message?.trim()?.takeIf { it.isNotBlank() }?.take(220) ?: error::class.java.simpleName
