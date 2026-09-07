package org.banddrip.app.source

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

data class NightscoutEndpoint(
    val baseUrl: String,
    val token: String?,
) {
    companion object {
        fun parse(input: String, explicitToken: String? = null): NightscoutEndpoint {
            val trimmed = input.trim()
            require(trimmed.isNotBlank()) { "Nightscout URL is empty" }
            val uri = URI.create(trimmed)
            require(uri.scheme.equals("https", ignoreCase = true)) { "Nightscout URL must use HTTPS" }
            require(!uri.host.isNullOrBlank()) { "Nightscout URL has no host" }

            val embeddedToken = parseQuery(uri.rawQuery)["token"]?.takeIf { it.isNotBlank() }
            val selectedToken = explicitToken?.trim()?.takeIf { it.isNotBlank() } ?: embeddedToken
            val port = if (uri.port == -1) "" else ":${uri.port}"
            val cleanPath = uri.path.orEmpty().trimEnd('/').takeIf { it != "/" }.orEmpty()
            val base = "${uri.scheme}://${uri.host}$port$cleanPath"
            return NightscoutEndpoint(baseUrl = base, token = selectedToken)
        }

        private fun parseQuery(rawQuery: String?): Map<String, String> {
            if (rawQuery.isNullOrBlank()) return emptyMap()
            return rawQuery.split('&').mapNotNull { item ->
                val pair = item.split('=', limit = 2)
                val key = decode(pair[0])
                if (key.isBlank()) return@mapNotNull null
                key to decode(pair.getOrElse(1) { "" })
            }.toMap()
        }

        private fun decode(value: String): String =
            URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }
}
