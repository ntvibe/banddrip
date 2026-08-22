package org.banddrip.app.source

import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.banddrip.app.model.BandDripReading

class XDripHttpSource(
    private val serverUrl: String,
    private val secret: String? = null,
) : GlucoseSource {
    override val id: String = "xdrip-http"

    override suspend fun latestReading(): BandDripReading? = withContext(Dispatchers.IO) {
        val body = get("/sgv.json?brief_mode=Y")
        NightscoutParser.parseReading(body)?.copy(source = id)
    }

    suspend fun checkConnection(): Result<BandDripReading?> = withContext(Dispatchers.IO) {
        runCatching { latestReading() }
    }

    fun sanitizedDescription(): String = normalizedBase()

    private fun get(pathAndQuery: String): String {
        val base = normalizedBase()
        val uri = URI.create(base)
        require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
            "xDrip server URL must use http:// or https://"
        }

        val connection = URI.create(base + pathAndQuery).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 4_000
            connection.readTimeout = 4_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "BandDrip/0.1")
            secret?.trim()?.takeIf { it.isNotEmpty() }?.let {
                connection.setRequestProperty("api-secret", sha1(it))
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw XDripHttpException(status, detail?.take(240))
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizedBase(): String {
        val raw = serverUrl.trim().trimEnd('/')
        require(raw.isNotBlank()) { "xDrip server URL is empty" }
        return if (raw.contains("://")) raw else "http://$raw"
    }

    private fun sha1(value: String): String = MessageDigest.getInstance("SHA-1")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

class XDripHttpException(
    val statusCode: Int,
    detail: String?,
) : Exception("xDrip HTTP $statusCode${detail?.let { ": $it" } ?: ""}")
