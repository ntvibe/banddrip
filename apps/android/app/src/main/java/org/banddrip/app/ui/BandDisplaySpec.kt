package org.banddrip.app.ui

import android.content.Context
import org.json.JSONObject

data class BandDisplayColors(
    val background: String,
    val primary: String,
    val stale: String,
    val delta: String,
    val separator: String,
    val age: String,
    val iob: String,
    val demo: String,
)

data class BandDisplaySpec(
    val version: Int,
    val device: String,
    val designWidth: Float,
    val designHeight: Float,
    val capsuleRadius: Float,
    val contentTop: Float,
    val contentHorizontalPadding: Float,
    val glucoseRowWidth: Float,
    val glucoseRowHeight: Float,
    val glucoseFontSize: Float,
    val glucoseFontWeight: Int,
    val trendFontSize: Float,
    val trendFontWeight: Int,
    val trendMarginLeft: Float,
    val metaMarginTop: Float,
    val metaRowWidth: Float,
    val metaRowHeight: Float,
    val metaFontSize: Float,
    val metaFontWeight: Int,
    val separatorFontSize: Float,
    val separatorMargin: Float,
    val iobMarginTop: Float,
    val iobFontSize: Float,
    val iobFontWeight: Int,
    val colors: BandDisplayColors,
) {
    companion object {
        fun load(context: Context): BandDisplaySpec {
            val json = context.assets.open("band10-v1.json")
                .bufferedReader()
                .use { JSONObject(it.readText()) }
            val colors = json.getJSONObject("colors")
            return BandDisplaySpec(
                version = json.getInt("version"),
                device = json.getString("device"),
                designWidth = json.getDouble("designWidth").toFloat(),
                designHeight = json.getDouble("designHeight").toFloat(),
                capsuleRadius = json.getDouble("capsuleRadius").toFloat(),
                contentTop = json.getDouble("contentTop").toFloat(),
                contentHorizontalPadding = json.getDouble("contentHorizontalPadding").toFloat(),
                glucoseRowWidth = json.getDouble("glucoseRowWidth").toFloat(),
                glucoseRowHeight = json.getDouble("glucoseRowHeight").toFloat(),
                glucoseFontSize = json.getDouble("glucoseFontSize").toFloat(),
                glucoseFontWeight = json.getInt("glucoseFontWeight"),
                trendFontSize = json.getDouble("trendFontSize").toFloat(),
                trendFontWeight = json.getInt("trendFontWeight"),
                trendMarginLeft = json.getDouble("trendMarginLeft").toFloat(),
                metaMarginTop = json.getDouble("metaMarginTop").toFloat(),
                metaRowWidth = json.getDouble("metaRowWidth").toFloat(),
                metaRowHeight = json.getDouble("metaRowHeight").toFloat(),
                metaFontSize = json.getDouble("metaFontSize").toFloat(),
                metaFontWeight = json.getInt("metaFontWeight"),
                separatorFontSize = json.getDouble("separatorFontSize").toFloat(),
                separatorMargin = json.getDouble("separatorMargin").toFloat(),
                iobMarginTop = json.getDouble("iobMarginTop").toFloat(),
                iobFontSize = json.getDouble("iobFontSize").toFloat(),
                iobFontWeight = json.getInt("iobFontWeight"),
                colors = BandDisplayColors(
                    background = colors.getString("background"),
                    primary = colors.getString("primary"),
                    stale = colors.getString("stale"),
                    delta = colors.getString("delta"),
                    separator = colors.getString("separator"),
                    age = colors.getString("age"),
                    iob = colors.getString("iob"),
                    demo = colors.getString("demo"),
                ),
            )
        }
    }
}
