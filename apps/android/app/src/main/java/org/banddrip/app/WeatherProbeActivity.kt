package org.banddrip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.banddrip.app.transport.GadgetbridgeWeatherBridge
import org.banddrip.app.transport.GlucoseWeatherEncoder

/** Temporary hardware lab UI. Remove the second launcher entry after the weather
 * carrier is characterized and move GadgetbridgeWeatherBridge into the relay. */
class WeatherProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var status by remember { mutableStateOf("Install/activate the Weather Transport watchface first.") }

                    fun send(label: String, payload: GlucoseWeatherEncoder.Payload) {
                        status = runCatching {
                            val result = GadgetbridgeWeatherBridge.send(this@WeatherProbeActivity, payload)
                            if (result.receivers == 0) {
                                "$label: no Gadgetbridge weather receiver found"
                            } else {
                                "$label sent ✓  receiver(s): ${result.packages.joinToString()}"
                            }
                        }.getOrElse { error ->
                            "$label failed: ${error.message ?: error::class.java.simpleName}"
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("BandDrip Weather Probe", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Hardware probe build: synthetic weather → Gadgetbridge → Xiaomi → watchface.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "CI trigger build 1 · This sends synthetic weather directly to Gadgetbridge. The values are intentionally weird so we can identify Xiaomi's field transforms on the band.",
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        Button(
                            onClick = { send("TEST A", GlucoseWeatherEncoder.probeA()) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Send TEST A · 123 / 42 / 155 / 6") }

                        Button(
                            onClick = { send("TEST B", GlucoseWeatherEncoder.probeB()) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Send TEST B · 124 / 43 / 156 / 7") }

                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Expected primary carriers if Xiaomi passes them through: temperature 123→124, humidity 42→43, AQI 155→156, UV 6→7, wind angle 271→272, pressure 1012→1013.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
