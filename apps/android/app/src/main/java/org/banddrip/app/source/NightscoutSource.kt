package org.banddrip.app.source

import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.banddrip.app.model.BandDripReading

class NightscoutSource(
    private val baseUrl: String,
    private val accessToken: String? = null,
) : GlucoseSource {
    override val id: String = "nightscout"

    override suspend fun latestReading(): BandDripReading? = withContext(Dispatchers.IO) {
        val entries = get("/api/v1/entries/sgv.json?count=2")
        val deviceStatus = runCatching {
            get("/api/v1/devicestatus.json?count=1")
        }.getOrNull()
        NightscoutParser.parseReading(entries, deviceStatus)
    }

    suspend fun checkConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            get("/api/v1/status.json")
            Unit
        }
    }

    private fun get(pathAndQuery: String): String {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        require(normalizedBase.startsWith("https://")) {
            "Nightscout URL must use HTTPS"
        }

        val separator = if ('?' in pathAndQuery) '&' else '?'
        val token = accessToken?.trim()?.takeIf { it.isNotEmpty() }
        val url = buildString {
            append(normalizedBase)
            append(pathAndQuery)
            if (token != null) {
                append(separator)
                append("token=")
                append(URLEncoder.encode(token, StandardCharsets.UTF_8.name()))
            }
        }

        val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BandDrip/0.1")

            val status = connection.responseCode
            if (status !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw NightscoutException(status, detail?.take(240))
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

class NightscoutException(
    val statusCode: Int,
    detail: String?,
) : Exception("Nightscout HTTP $statusCode${detail?.let { ": $it" } ?: ""}")
