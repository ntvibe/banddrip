package org.banddrip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.banddrip.app.safety.FreshnessPolicy

@Composable
fun BandPreview(
    reading: BandDripReading?,
    showIob: Boolean,
    modifier: Modifier = Modifier,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }

    val stale = reading == null || FreshnessPolicy.isStale(reading.glucoseTimestampMs, nowMs)
    val mainColor = if (stale && reading != null) MaterialTheme.colorScheme.error else Color.White

    Box(
        modifier = modifier
            .width(132.dp)
            .height(324.dp)
            .clip(RoundedCornerShape(66.dp))
            .background(Color.Black),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 58.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reading?.let(::glucoseText) ?: "—",
                    style = TextStyle(
                        color = mainColor,
                        fontSize = 45.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (stale && reading != null) TextDecoration.LineThrough else null,
                    ),
                )
                if (reading != null) {
                    Text(
                        text = trendArrow(reading.trend),
                        color = mainColor,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = reading?.let(::deltaText) ?: "Δ —",
                    color = Color(0xFF64D2FF),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text("·", color = Color(0xFF6E6E73), fontSize = 16.sp)
                Text(
                    text = reading?.let { ageText(it.glucoseTimestampMs, nowMs) } ?: "age —",
                    color = Color(0xFFB0B0B5),
                    fontSize = 16.sp,
                )
            }

            if (showIob) {
                Text(
                    text = reading?.let { iobText(it, nowMs) } ?: "IOB —",
                    color = Color(0xFFD1D1D6),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun glucoseText(reading: BandDripReading): String = when (reading.units) {
    GlucoseUnits.MgDl -> reading.glucose.toInt().toString()
    GlucoseUnits.MmolL -> "%.1f".format(reading.glucose)
}

private fun deltaText(reading: BandDripReading): String {
    val delta = reading.delta ?: return "Δ —"
    return when (reading.units) {
        GlucoseUnits.MgDl -> "%+d".format(delta.toInt())
        GlucoseUnits.MmolL -> "%+.1f".format(delta)
    }
}

private fun ageText(timestampMs: Long, nowMs: Long): String {
    val age = FreshnessPolicy.ageMinutes(timestampMs, nowMs) ?: return "age —"
    return "${age}m ago"
}

private fun iobText(reading: BandDripReading, nowMs: Long): String {
    val iob = reading.iobUnits ?: return "IOB —"
    val timestamp = reading.iobTimestampMs ?: return "IOB —"
    if (FreshnessPolicy.isStale(timestamp, nowMs)) return "IOB —"
    return "IOB %.3f U".format(iob)
}

private fun trendArrow(trend: Trend): String = when (trend) {
    Trend.DoubleUp -> "⇈"
    Trend.SingleUp -> "↑"
    Trend.FortyFiveUp -> "↗"
    Trend.Flat -> "→"
    Trend.FortyFiveDown -> "↘"
    Trend.SingleDown -> "↓"
    Trend.DoubleDown -> "⇊"
    Trend.Unknown -> "?"
}
