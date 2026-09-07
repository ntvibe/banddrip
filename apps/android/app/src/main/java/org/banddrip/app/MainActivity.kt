package org.banddrip.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.banddrip.app.config.AppSettingsStore
import org.banddrip.app.config.RelaySettings
import org.banddrip.app.config.SourceMode
import org.banddrip.app.config.XDripConnectionMode
import org.banddrip.app.core.BandDripEngine
import org.banddrip.app.core.RelayStateStore
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.banddrip.app.reliability.ReliabilityController
import org.banddrip.app.service.BandDripRelayService
import org.banddrip.app.source.GlucoseSource
import org.banddrip.app.source.MockGlucoseSource
import org.banddrip.app.source.NightscoutSource
import org.banddrip.app.source.XDripHttpSource
import org.banddrip.app.source.XDripSource
import org.banddrip.app.transport.VirtualBandTransport
import org.banddrip.app.ui.BandPreview
import org.banddrip.app.ui.BandSetupPanel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BandDripHome(this)
                }
            }
        }
    }
}

@Composable
private fun BandDripHome(activity: ComponentActivity) {
    val context = LocalContext.current
    val settingsStore = remember { AppSettingsStore(context) }
    val stateStore = remember { RelayStateStore(context) }
    var settings by remember { mutableStateOf(settingsStore.load()) }
    var reading by remember { mutableStateOf(stateStore.loadReading()) }
    var relayStatus by remember { mutableStateOf(stateStore.status()) }
    var sourceTestStatus by remember { mutableStateOf("Not tested yet") }
    var lastPacket by remember { mutableStateOf(stateStore.lastPacket()) }
    var reliabilityTick by remember { mutableStateOf(0) }
    var sourceBusy by remember { mutableStateOf(false) }

    val engine = remember { BandDripEngine() }
    val scope = rememberCoroutineScope()
    val transport = remember {
        VirtualBandTransport { packet ->
            lastPacket = packet
            stateStore.savePacket(packet)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        reliabilityTick += 1
    }

    LaunchedEffect(Unit) {
        while (true) {
            reading = stateStore.loadReading() ?: reading
            relayStatus = stateStore.status()
            lastPacket = stateStore.lastPacket()
            reliabilityTick += 1
            delay(2_000)
        }
    }

    fun persist(next: RelaySettings = settings, restartRelay: Boolean = false): Boolean {
        return try {
            settings = next
            settingsStore.save(next)
            settings = settingsStore.load()
            if (restartRelay && settings.backgroundEnabled) {
                runCatching {
                    BandDripRelayService.start(
                        context,
                        BandDripRelayService.ACTION_SETTINGS_CHANGED,
                    )
                }.onFailure {
                    relayStatus = "Could not restart relay: ${safeUiError(it)}"
                }
            }
            true
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            sourceTestStatus = "Failed ✕ · settings: ${safeUiError(error)}"
            false
        }
    }

    fun currentSource(): GlucoseSource? = when (settings.sourceMode) {
        SourceMode.Mock -> MockGlucoseSource(configProvider = { settings.mock })
        SourceMode.Nightscout -> settings.nightscoutUrl.takeIf { it.isNotBlank() }?.let {
            NightscoutSource(it, settings.nightscoutToken.ifBlank { null })
        }
        SourceMode.XDrip -> when (settings.xdripConnectionMode) {
            XDripConnectionMode.Broadcast -> XDripSource(context)
            XDripConnectionMode.LocalServer -> XDripHttpSource(
                settings.xdripServerUrl,
                settings.xdripServerSecret.ifBlank { null },
            )
        }
    }

    fun refreshSelectedSource() {
        if (sourceBusy) return
        sourceBusy = true
        sourceTestStatus = "Testing…"

        scope.launch {
            try {
                if (!persist()) return@launch
                val source = try {
                    currentSource()
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    sourceTestStatus = "Failed ✕ · configuration: ${safeUiError(error)}"
                    return@launch
                }

                if (source == null) {
                    sourceTestStatus = "Failed ✕ · source is not configured"
                    return@launch
                }

                val snapshot = engine.refresh(source, transport, settings.showIob)
                if (snapshot.errorMessage != null) {
                    sourceTestStatus = "Failed ✕ · ${snapshot.errorMessage}"
                } else if (snapshot.reading == null) {
                    sourceTestStatus = "Connected ✓ · no glucose reading received"
                } else {
                    reading = snapshot.reading
                    stateStore.saveReading(
                        snapshot.reading,
                        "${snapshot.sourceId} connected · manual test",
                    )
                    sourceTestStatus = "Connected ✓ · ${formatReadingSummary(snapshot.reading)}"
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                sourceTestStatus = "Failed ✕ · ${safeUiError(error)}"
            } finally {
                sourceBusy = false
            }
        }
    }

    val notificationAllowed = ReliabilityController.hasNotificationPermission(context)
    val batteryUnrestricted = ReliabilityController.isIgnoringBatteryOptimizations(context)
    val xdripInstalled = remember(reliabilityTick) {
        isPackageInstalled(context.packageManager, "com.eveningoutpost.dexdrip")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("BandDrip", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Glucose relay + Smart Band 10 control console",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider()
        SectionTitle("Glucose source")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SourceMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.sourceMode == mode,
                    onClick = {
                        sourceTestStatus = "Not tested yet"
                        persist(settings.copy(sourceMode = mode), restartRelay = true)
                    },
                    label = { Text(mode.label()) },
                )
            }
        }

        when (settings.sourceMode) {
            SourceMode.Mock -> MockSettingsPanel(
                settings = settings,
                onChange = { persist(it, restartRelay = true) },
            )
            SourceMode.Nightscout -> NightscoutSettingsPanel(
                settings = settings,
                onChange = { settings = it },
                onSave = { persist(settings, restartRelay = true) },
            )
            SourceMode.XDrip -> XDripSettingsPanel(
                settings = settings,
                xdripInstalled = xdripInstalled,
                onChange = { settings = it },
                onSave = { persist(settings, restartRelay = true) },
            )
        }

        Button(
            onClick = ::refreshSelectedSource,
            enabled = !sourceBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (sourceBusy) {
                    "Testing…"
                } else {
                    when (settings.sourceMode) {
                        SourceMode.Mock -> "Generate / test mock reading"
                        SourceMode.Nightscout -> "Test Nightscout connection"
                        SourceMode.XDrip -> if (settings.xdripConnectionMode == XDripConnectionMode.LocalServer) {
                            "Test xDrip local server"
                        } else {
                            "Check xDrip broadcast data"
                        }
                    }
                },
            )
        }

        ConnectionStatus(sourceTestStatus)

        HorizontalDivider()
        SectionTitle("Smart Band 10 preview")
        Text(
            "The preview is driven by the same 212×520 display specification that Vela CI validates against the RPK.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            BandPreview(reading = reading, showIob = settings.showIob)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show IOB", fontWeight = FontWeight.SemiBold)
                Text("Enabled by default", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = settings.showIob,
                onCheckedChange = { persist(settings.copy(showIob = it), restartRelay = true) },
            )
        }

        HorizontalDivider()
        BandSetupPanel()

        HorizontalDivider()
        SectionTitle("Always-on relay")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Background relay", fontWeight = FontWeight.SemiBold)
                Text(
                    "Persistent foreground service; restarts after reboot when enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = settings.backgroundEnabled,
                onCheckedChange = { enabled ->
                    val next = settings.copy(backgroundEnabled = enabled)
                    if (!persist(next)) return@Switch
                    if (enabled) {
                        runCatching { BandDripRelayService.start(context) }
                            .onFailure { relayStatus = "Could not start relay: ${safeUiError(it)}" }
                    } else {
                        runCatching { BandDripRelayService.stop(context) }
                    }
                },
            )
        }

        Text(
            "Relay: ${if (stateStore.isServiceRunning()) "RUNNING ✓" else "STOPPED"} · $relayStatus",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            if (lastPacket == null) "No Band protocol packet generated yet" else "Band protocol packet ready ✓",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()
        SectionTitle("Reliability setup")
        ReliabilityRow("Foreground relay", settings.backgroundEnabled && stateStore.isServiceRunning())
        ReliabilityRow("Notifications allowed", notificationAllowed)
        ReliabilityRow("Battery optimization disabled", batteryUnrestricted)

        if (ReliabilityController.isXiaomiDevice()) {
            Text(
                "Xiaomi/HyperOS may kill background apps independently. Enable Autostart and set Battery saver to No restrictions for BandDrip.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!notificationAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(
                onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Allow notifications") }
        }
        if (!batteryUnrestricted) {
            OutlinedButton(
                onClick = { ReliabilityController.requestBatteryOptimizationExemption(activity) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Allow unrestricted battery use") }
        }
        if (ReliabilityController.isXiaomiDevice()) {
            OutlinedButton(
                onClick = { ReliabilityController.openXiaomiAutostart(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Open Xiaomi Autostart") }
            OutlinedButton(
                onClick = { ReliabilityController.openXiaomiBatterySaver(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Open Xiaomi battery settings") }
        }
        OutlinedButton(
            onClick = { ReliabilityController.openAppBatterySettings(context) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Open Android app battery settings") }

        Text(
            "Accessibility permission is intentionally not requested. It would allow BandDrip to inspect/control other apps but does not make the glucose relay more reliable. Foreground service + boot restart + unrestricted battery/autostart are the relevant mechanisms.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "BandDrip is an unofficial secondary glucose display and is not a medical device. Do not use it as the sole basis for treatment decisions. Data may be delayed, unavailable, or incorrect. Use at your own risk and verify important readings in your approved CGM/pump system.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MockSettingsPanel(settings: RelaySettings, onChange: (RelaySettings) -> Unit) {
    val mock = settings.mock
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Glucose", mock.glucose, Modifier.weight(1f)) {
                it?.let { value -> onChange(settings.copy(mock = mock.copy(glucose = value))) }
            }
            NullableNumberField("Delta", mock.delta, Modifier.weight(1f)) {
                onChange(settings.copy(mock = mock.copy(delta = it)))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntField("Age min", mock.ageMinutes, Modifier.weight(1f)) {
                onChange(settings.copy(mock = mock.copy(ageMinutes = it.coerceIn(0, 240))))
            }
            NullableNumberField("IOB U", mock.iobUnits, Modifier.weight(1f)) {
                onChange(settings.copy(mock = mock.copy(iobUnits = it)))
            }
        }
        IntField("IOB age min", mock.iobAgeMinutes, Modifier.fillMaxWidth()) {
            onChange(settings.copy(mock = mock.copy(iobAgeMinutes = it.coerceIn(0, 240))))
        }
        Text("Trend", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Trend.FortyFiveUp, Trend.Flat, Trend.FortyFiveDown).forEach { trend ->
                FilterChip(
                    selected = mock.trend == trend,
                    onClick = { onChange(settings.copy(mock = mock.copy(trend = trend))) },
                    label = { Text(trend.shortLabel()) },
                )
            }
        }
        Text("Units", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlucoseUnits.entries.forEach { units ->
                FilterChip(
                    selected = mock.units == units,
                    onClick = { onChange(settings.copy(mock = mock.copy(units = units))) },
                    label = { Text(units.wireValue) },
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-cycle edge cases", fontWeight = FontWeight.SemiBold)
                Text("Fresh, stale, high, mmol/L, stale IOB, missing delta", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = mock.autoCycle,
                onCheckedChange = { onChange(settings.copy(mock = mock.copy(autoCycle = it))) },
            )
        }
        if (mock.autoCycle) {
            IntField("Cycle interval sec", mock.intervalSeconds, Modifier.fillMaxWidth()) {
                onChange(settings.copy(mock = mock.copy(intervalSeconds = it.coerceIn(2, 60))))
            }
        }
    }
}

@Composable
private fun NightscoutSettingsPanel(
    settings: RelaySettings,
    onChange: (RelaySettings) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = settings.nightscoutUrl,
            onValueChange = { onChange(settings.copy(nightscoutUrl = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nightscout URL or full tracking link") },
            placeholder = { Text("https://your-site.example/?token=track-…") },
            singleLine = true,
        )
        OutlinedTextField(
            value = settings.nightscoutToken,
            onValueChange = { onChange(settings.copy(nightscoutToken = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Token (optional if included in URL)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        IntField("Poll every minutes", settings.nightscoutPollMinutes, Modifier.fillMaxWidth()) {
            onChange(settings.copy(nightscoutPollMinutes = it.coerceIn(1, 30)))
        }
        Text(
            "Paste a full tracking link or a clean base URL. Embedded tokens are stripped from the stored URL and encrypted locally. Connection-test failures remain on this screen as diagnostics.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save Nightscout settings")
        }
    }
}

@Composable
private fun XDripSettingsPanel(
    settings: RelaySettings,
    xdripInstalled: Boolean,
    onChange: (RelaySettings) -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XDripConnectionMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.xdripConnectionMode == mode,
                    onClick = { onChange(settings.copy(xdripConnectionMode = mode)) },
                    label = { Text(if (mode == XDripConnectionMode.LocalServer) "Local server" else "Broadcast") },
                )
            }
        }

        if (settings.xdripConnectionMode == XDripConnectionMode.LocalServer) {
            OutlinedTextField(
                value = settings.xdripServerUrl,
                onValueChange = { onChange(settings.copy(xdripServerUrl = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("xDrip server URL") },
                placeholder = { Text("http://127.0.0.1:17580") },
                singleLine = true,
            )
            OutlinedTextField(
                value = settings.xdripServerSecret,
                onValueChange = { onChange(settings.copy(xdripServerSecret = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Web Service Secret (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Text(
                "For xDrip on this phone, enable Local Web Service in xDrip Inter-App settings and use http://127.0.0.1:17580. Nightscout is not required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "xDrip installed: ${if (xdripInstalled) "YES ✓" else "not detected"}",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Enable xDrip's broadcast-data option. BandDrip listens for BgEstimate broadcasts and computes delta from consecutive readings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save xDrip settings")
        }
    }
}

@Composable
private fun ConnectionStatus(status: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Connection", fontWeight = FontWeight.SemiBold)
        Text(status, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReliabilityRow(label: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(if (ok) "OK ✓" else "NEEDS SETUP", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun NumberField(label: String, value: Double, modifier: Modifier, onChange: (Double?) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.replace(',', '.').toDoubleOrNull()) },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun NullableNumberField(label: String, value: Double?, modifier: Modifier, onChange: (Double?) -> Unit) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { onChange(it.replace(',', '.').toDoubleOrNull()) },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@Composable
private fun IntField(label: String, value: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toIntOrNull()?.let(onChange) },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

private fun SourceMode.label(): String = when (this) {
    SourceMode.Mock -> "Mock"
    SourceMode.Nightscout -> "Nightscout"
    SourceMode.XDrip -> "xDrip"
}

private fun Trend.shortLabel(): String = when (this) {
    Trend.FortyFiveUp -> "↗"
    Trend.Flat -> "→"
    Trend.FortyFiveDown -> "↘"
    else -> wireValue
}

private fun formatReadingSummary(reading: BandDripReading): String {
    val glucose = if (reading.units == GlucoseUnits.MgDl) {
        reading.glucose.toInt().toString()
    } else {
        "%.1f".format(reading.glucose)
    }
    val age = ((System.currentTimeMillis() - reading.glucoseTimestampMs).coerceAtLeast(0L) / 60_000L)
    return "$glucose ${reading.units.wireValue} · ${age}m ago"
}

private fun safeUiError(error: Throwable): String =
    error.message?.trim()?.takeIf { it.isNotBlank() }?.take(280) ?: error::class.java.simpleName

private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0)
    }
}.isSuccess
