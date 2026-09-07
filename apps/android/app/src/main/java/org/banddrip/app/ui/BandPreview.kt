package org.banddrip.app.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.banddrip.app.safety.FreshnessPolicy

/**
 * Phone-side preview of the Vela page.
 *
 * Geometry, typography sizes and colors are loaded from packages/display-spec/band10-v1.json.
 * Vela CI asserts that apps/band/src/pages/index/index.ux still matches that same spec.
 */
@Composable
fun BandPreview(
    reading: BandDripReading?,
    showIob: Boolean,
    modifier: Modifier = Modifier,
    previewWidth: Dp = 180.dp,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val spec = remember { BandDisplaySpec.load(context) }
    val scale = previewWidth.value / spec.designWidth
    fun d(value: Float): Dp = (value * scale).dp
    fun textSize(value: Float) = with(density) { d(value).toSp() }
    fun color(hex: String) = Color(AndroidColor.parseColor(hex))

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMs = System.currentTimeMillis()
        }
    }

    val stale = reading == null || FreshnessPolicy.isStale(reading.glucoseTimestampMs, nowMs)
    val mainColor = if (stale && reading != null) color(spec.colors.stale) else color(spec.colors.primary)

    Box(
        modifier = modifier
            .width(previewWidth)
            .height(d(spec.designHeight))
            .clip(RoundedCornerShape(d(spec.capsuleRadius)))
            .background(color(spec.colors.background)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = d(spec.contentTop),
                    start = d(spec.contentHorizontalPadding),
                    end = d(spec.contentHorizontalPadding),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .width(d(spec.glucoseRowWidth))
                    .height(d(spec.glucoseRowHeight)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = reading?.let(::glucoseText) ?: "—",
                    style = TextStyle(
                        color = mainColor,
                        fontSize = textSize(spec.glucoseFontSize),
                        fontWeight = FontWeight(spec.glucoseFontWeight),
                        textDecoration = if (stale && reading != null) TextDecoration.LineThrough else null,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
                if (reading != null) {
                    Text(
                        text = trendArrow(reading.trend),
                        modifier = Modifier.padding(start = d(spec.trendMarginLeft)),
                        style = TextStyle(
                            color = mainColor,
                            fontSize = textSize(spec.trendFontSize),
                            fontWeight = FontWeight(spec.trendFontWeight),
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(d(spec.metaMarginTop)))

            Row(
                modifier = Modifier
                    .width(d(spec.metaRowWidth))
                    .height(d(spec.metaRowHeight)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = reading?.let(::deltaText) ?: "Δ —",
                    style = TextStyle(
                        color = color(spec.colors.delta),
                        fontSize = textSize(spec.metaFontSize),
                        fontWeight = FontWeight(spec.metaFontWeight),
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
                Text(
                    text = "·",
                    modifier = Modifier.padding(horizontal = d(spec.separatorMargin)),
                    style = TextStyle(
                        color = color(spec.colors.separator),
                        fontSize = textSize(spec.separatorFontSize),
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
                Text(
                    text = reading?.let { ageText(it.glucoseTimestampMs, nowMs) } ?: "age —",
                    style = TextStyle(
                        color = color(spec.colors.age),
                        fontSize = textSize(spec.metaFontSize),
                        fontWeight = FontWeight(spec.metaFontWeight),
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                )
            }

            if (showIob) {
                Spacer(modifier = Modifier.height(d(spec.iobMarginTop)))
                Text(
                    text = reading?.let { iobText(it, nowMs) } ?: "IOB —",
                    style = TextStyle(
                        color = color(spec.colors.iob),
                        fontSize = textSize(spec.iobFontSize),
                        fontWeight = FontWeight(spec.iobFontWeight),
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
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
