package org.banddrip.app.pro_nfc

import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Extracts the Xiaomi protobuf pairing credential from a user-selected Mi Fitness
 * diagnostic ZIP. The key is returned to the caller only in memory and is never
 * logged or persisted by this helper.
 */
object MiFitnessCredentialImporter {
    data class Result(
        val key: String,
        val fingerprint: String,
        val sourceEntry: String,
        val occurrences: Int,
    )

    private val encryptKeyRegex = Regex(
        """(?i)[\"']?encryptKey[\"']?\s*[:=]\s*[\"']?([0-9a-f]{32})[\"']?""",
    )
    private val authKeyRegex = Regex(
        """(?i)[\"']?(?:authKey|huamiAuthKey)[\"']?\s*[:=]\s*[\"']?([0-9a-f]{32})[\"']?""",
    )

    fun extract(zipStream: InputStream): Result {
        val encryptMatches = mutableListOf<Match>()
        val authMatches = mutableListOf<Match>()

        ZipInputStream(zipStream.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                val name = entry.name
                if (!name.endsWith(".log", ignoreCase = true) &&
                    !name.endsWith(".txt", ignoreCase = true) &&
                    !name.endsWith(".json", ignoreCase = true)
                ) continue

                zip.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        encryptKeyRegex.findAll(line).forEach { match ->
                            encryptMatches += Match(match.groupValues[1].lowercase(), name)
                        }
                        authKeyRegex.findAll(line).forEach { match ->
                            authMatches += Match(match.groupValues[1].lowercase(), name)
                        }
                    }
                }
            }
        }

        // Mi Fitness / Mi Health commonly logs Xiaomi protobuf keys as encryptKey.
        // Prefer the latest occurrence, matching Gadgetbridge's documented guidance.
        val candidates = if (encryptMatches.isNotEmpty()) encryptMatches else authMatches
        val latest = candidates.lastOrNull()
            ?: error("No 32-hex encryptKey/authKey was found in this Mi Fitness log ZIP")

        return Result(
            key = latest.key,
            fingerprint = sha256(latest.key).take(12),
            sourceEntry = latest.entry,
            occurrences = candidates.count { it.key == latest.key },
        )
    }

    private data class Match(val key: String, val entry: String)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
