package org.banddrip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.banddrip.app.core.BandDripEngine
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.source.GlucoseSource
import org.banddrip.app.source.MockGlucoseSource
import org.banddrip.app.source.NightscoutSource
import org.banddrip.app.transport.VirtualBandTransport
import org.banddrip.app.ui.BandPreview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BandDripHome()
                }
            }
        }
    }
}

private enum class SourceMode { Mock, Nightscout }

@Composable
private fun BandDripHome() {
    var showIob by rememberSaveable { mutableStateOf(true) }
    var sourceMode by rememberSaveable { mutableStateOf(SourceMode.Mock) }
    var nightscoutUrl by rememberSaveable { mutableStateOf("") }
    var nightscoutToken by rememberSaveable { mutableStateOf("") }
    var reading by remember { mutableStateOf<BandDripReading?>(null) }
    var status by remember { mutableStateOf("Ready for virtual testing") }
    var lastPacket by remember { mutableStateOf<String?>(null) }

    val mockSource = remember { MockGlucoseSource() }
    val engine = remember { BandDripEngine() }
    val transport = remember { VirtualBandTransport { packet -> lastPacket = packet } }
    val scope = rememberCoroutineScope()

    fun selectedSource(): GlucoseSource = when (sourceMode) {
        SourceMode.Mock -> mockSource
        SourceMode.Nightscout -> NightscoutSource(
            baseUrl = nightscoutUrl,
            accessToken = nightscoutToken.ifBlank { null },
        )
    }

    fun refresh() {
        if (sourceMode == SourceMode.Nightscout && nightscoutUrl.isBlank()) {
            status = "Enter a Nightscout HTTPS URL first"
            return
        }

        scope.launch {
            status = "Refreshing…"
            val snapshot = engine.refresh(selectedSource(), transport, showIob)
            if (snapshot.errorMessage != null) {
                status = snapshot.errorMessage
            } else {
                reading = snapshot.reading
                status = if (snapshot.reading == null) {
                    "Source returned no glucose reading"
                } else {
                    "${snapshot.sourceId} → ${snapshot.transportId} packet sent"
                }
            }
        }
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
                "Virtual development console",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilterChip(
                selected = sourceMode == SourceMode.Mock,
                onClick = { sourceMode = SourceMode.Mock },
                label = { Text("Mock") },
            )
            FilterChip(
                selected = sourceMode == SourceMode.Nightscout,
                onClick = { sourceMode = SourceMode.Nightscout },
                label = { Text("Nightscout") },
            )
        }

        if (sourceMode == SourceMode.Nightscout) {
            OutlinedTextField(
                value = nightscoutUrl,
                onValueChange = { nightscoutUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nightscout URL") },
                placeholder = { Text("https://example.nightscout.site") },
                singleLine = true,
            )
            OutlinedTextField(
                value = nightscoutToken,
                onValueChange = { nightscoutToken = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Access token (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            Text(
                "Token is currently session-only and is not persisted by this development build.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Show IOB", fontWeight = FontWeight.SemiBold)
                Text(
                    "Enabled by default",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = showIob, onCheckedChange = { showIob = it })
        }

        Button(onClick = ::refresh, modifier = Modifier.fillMaxWidth()) {
            Text(if (sourceMode == SourceMode.Mock) "Generate next mock reading" else "Fetch Nightscout now")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            BandPreview(reading = reading, showIob = showIob)
        }

        if (reading != null) {
            Button(
                onClick = {
                    val stale = reading!!.copy(
                        glucoseTimestampMs = System.currentTimeMillis() - 12 * 60_000L,
                    )
                    reading = stale
                    scope.launch { transport.sendReading(stale) }
                    status = "Injected 12-minute stale reading"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Inject 12m stale state")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Status", fontWeight = FontWeight.SemiBold)
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (lastPacket == null) "No protocol packet yet" else "Protocol packet generated ✓",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Freshness policy", fontWeight = FontWeight.SemiBold)
            Text(
                "At 10+ minutes glucose is stale: red + strike-through, while exact age remains visible. IOB is independently hidden when stale.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "BandDrip is an unofficial secondary glucose display and is not a medical device. Do not use it as the sole basis for treatment decisions. Data may be delayed, unavailable, or incorrect. Use at your own risk and verify important readings in your approved CGM/pump system.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
