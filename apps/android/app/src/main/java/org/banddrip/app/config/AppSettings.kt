package org.banddrip.app.config

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.banddrip.app.model.GlucoseUnits
import org.banddrip.app.model.Trend
import org.banddrip.app.source.NightscoutEndpoint

enum class SourceMode(val wireValue: String) {
    Mock("mock"),
    Nightscout("nightscout"),
    XDrip("xdrip"),
}

enum class XDripConnectionMode(val wireValue: String) {
    Broadcast("broadcast"),
    LocalServer("local-server"),
}

data class MockSettings(
    val glucose: Double = 112.0,
    val delta: Double? = 6.0,
    val ageMinutes: Int = 3,
    val iobUnits: Double? = 0.250,
    val iobAgeMinutes: Int = 2,
    val trend: Trend = Trend.FortyFiveDown,
    val units: GlucoseUnits = GlucoseUnits.MgDl,
    val autoCycle: Boolean = false,
    val intervalSeconds: Int = 5,
)

data class RelaySettings(
    val sourceMode: SourceMode = SourceMode.Mock,
    val showIob: Boolean = true,
    val backgroundEnabled: Boolean = false,
    val nightscoutUrl: String = "",
    val nightscoutToken: String = "",
    val nightscoutPollMinutes: Int = 1,
    val xdripConnectionMode: XDripConnectionMode = XDripConnectionMode.LocalServer,
    val xdripServerUrl: String = "http://127.0.0.1:17580",
    val xdripServerSecret: String = "",
    val mock: MockSettings = MockSettings(),
)

class AppSettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secrets = KeystoreSecretBox()

    fun load(): RelaySettings {
        val source = SourceMode.entries.firstOrNull {
            it.wireValue == prefs.getString(KEY_SOURCE, SourceMode.Mock.wireValue)
        } ?: SourceMode.Mock
        val units = GlucoseUnits.entries.firstOrNull {
            it.wireValue == prefs.getString(KEY_MOCK_UNITS, GlucoseUnits.MgDl.wireValue)
        } ?: GlucoseUnits.MgDl
        val trend = Trend.entries.firstOrNull {
            it.wireValue == prefs.getString(KEY_MOCK_TREND, Trend.FortyFiveDown.wireValue)
        } ?: Trend.FortyFiveDown
        val xdripMode = XDripConnectionMode.entries.firstOrNull {
            it.wireValue == prefs.getString(KEY_XDRIP_MODE, XDripConnectionMode.LocalServer.wireValue)
        } ?: XDripConnectionMode.LocalServer

        return RelaySettings(
            sourceMode = source,
            showIob = prefs.getBoolean(KEY_SHOW_IOB, true),
            backgroundEnabled = prefs.getBoolean(KEY_BACKGROUND_ENABLED, false),
            nightscoutUrl = prefs.getString(KEY_NS_URL, "").orEmpty(),
            nightscoutToken = secrets.decrypt(prefs.getString(KEY_NS_TOKEN, null)).orEmpty(),
            nightscoutPollMinutes = prefs.getInt(KEY_NS_POLL_MINUTES, 1).coerceIn(1, 30),
            xdripConnectionMode = xdripMode,
            xdripServerUrl = prefs.getString(KEY_XDRIP_SERVER_URL, "http://127.0.0.1:17580").orEmpty(),
            xdripServerSecret = secrets.decrypt(prefs.getString(KEY_XDRIP_SERVER_SECRET, null)).orEmpty(),
            mock = MockSettings(
                glucose = prefs.getString(KEY_MOCK_GLUCOSE, null)?.toDoubleOrNull() ?: 112.0,
                delta = prefs.getString(KEY_MOCK_DELTA, null)?.let { if (it == "null") null else it.toDoubleOrNull() } ?: 6.0,
                ageMinutes = prefs.getInt(KEY_MOCK_AGE, 3).coerceIn(0, 240),
                iobUnits = prefs.getString(KEY_MOCK_IOB, null)?.let { if (it == "null") null else it.toDoubleOrNull() } ?: 0.250,
                iobAgeMinutes = prefs.getInt(KEY_MOCK_IOB_AGE, 2).coerceIn(0, 240),
                trend = trend,
                units = units,
                autoCycle = prefs.getBoolean(KEY_MOCK_AUTO_CYCLE, false),
                intervalSeconds = prefs.getInt(KEY_MOCK_INTERVAL_SECONDS, 5).coerceIn(2, 60),
            ),
        )
    }

    fun save(settings: RelaySettings) {
        val normalizedNightscout = runCatching {
            NightscoutEndpoint.parse(settings.nightscoutUrl, settings.nightscoutToken.ifBlank { null })
        }.getOrNull()
        val storedNsUrl = normalizedNightscout?.baseUrl ?: settings.nightscoutUrl.trim()
        val storedNsToken = normalizedNightscout?.token ?: settings.nightscoutToken.trim().takeIf { it.isNotBlank() }
        val encryptedNsToken = storedNsToken?.let(secrets::encrypt)
        val encryptedXdripSecret = settings.xdripServerSecret.trim().takeIf { it.isNotBlank() }?.let(secrets::encrypt)

        prefs.edit()
            .putString(KEY_SOURCE, settings.sourceMode.wireValue)
            .putBoolean(KEY_SHOW_IOB, settings.showIob)
            .putBoolean(KEY_BACKGROUND_ENABLED, settings.backgroundEnabled)
            .putString(KEY_NS_URL, storedNsUrl)
            .putInt(KEY_NS_POLL_MINUTES, settings.nightscoutPollMinutes.coerceIn(1, 30))
            .putString(KEY_XDRIP_MODE, settings.xdripConnectionMode.wireValue)
            .putString(KEY_XDRIP_SERVER_URL, settings.xdripServerUrl.trim().trimEnd('/'))
            .putString(KEY_MOCK_GLUCOSE, settings.mock.glucose.toString())
            .putString(KEY_MOCK_DELTA, settings.mock.delta?.toString() ?: "null")
            .putInt(KEY_MOCK_AGE, settings.mock.ageMinutes.coerceIn(0, 240))
            .putString(KEY_MOCK_IOB, settings.mock.iobUnits?.toString() ?: "null")
            .putInt(KEY_MOCK_IOB_AGE, settings.mock.iobAgeMinutes.coerceIn(0, 240))
            .putString(KEY_MOCK_TREND, settings.mock.trend.wireValue)
            .putString(KEY_MOCK_UNITS, settings.mock.units.wireValue)
            .putBoolean(KEY_MOCK_AUTO_CYCLE, settings.mock.autoCycle)
            .putInt(KEY_MOCK_INTERVAL_SECONDS, settings.mock.intervalSeconds.coerceIn(2, 60))
            .apply()

        prefs.edit().apply {
            if (encryptedNsToken == null) remove(KEY_NS_TOKEN) else putString(KEY_NS_TOKEN, encryptedNsToken)
            if (encryptedXdripSecret == null) remove(KEY_XDRIP_SERVER_SECRET) else putString(KEY_XDRIP_SERVER_SECRET, encryptedXdripSecret)
        }.apply()
    }

    fun saveBandAuthKey(authKey: String) {
        val normalized = authKey.trim().lowercase()
        require(normalized.matches(Regex("[0-9a-f]{32}"))) { "Band AuthKey must be 32 hexadecimal characters" }
        prefs.edit().putString(KEY_BAND_AUTH_KEY, secrets.encrypt(normalized)).apply()
    }

    fun loadBandAuthKey(): String? = secrets.decrypt(prefs.getString(KEY_BAND_AUTH_KEY, null))

    fun hasBandAuthKey(): Boolean = !loadBandAuthKey().isNullOrBlank()

    fun clearBandAuthKey() {
        prefs.edit().remove(KEY_BAND_AUTH_KEY).apply()
    }

    fun setBackgroundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "banddrip-settings-v1"
        private const val KEY_SOURCE = "source"
        private const val KEY_SHOW_IOB = "show-iob"
        private const val KEY_BACKGROUND_ENABLED = "background-enabled"
        private const val KEY_NS_URL = "nightscout-url"
        private const val KEY_NS_TOKEN = "nightscout-token-encrypted"
        private const val KEY_XDRIP_SERVER_SECRET = "xdrip-server-secret-encrypted"
        private const val KEY_BAND_AUTH_KEY = "band-auth-key-encrypted"
        private const val KEY_NS_POLL_MINUTES = "nightscout-poll-minutes"
        private const val KEY_XDRIP_MODE = "xdrip-mode"
        private const val KEY_XDRIP_SERVER_URL = "xdrip-server-url"
        private const val KEY_MOCK_GLUCOSE = "mock-glucose"
        private const val KEY_MOCK_DELTA = "mock-delta"
        private const val KEY_MOCK_AGE = "mock-age"
        private const val KEY_MOCK_IOB = "mock-iob"
        private const val KEY_MOCK_IOB_AGE = "mock-iob-age"
        private const val KEY_MOCK_TREND = "mock-trend"
        private const val KEY_MOCK_UNITS = "mock-units"
        private const val KEY_MOCK_AUTO_CYCLE = "mock-auto-cycle"
        private const val KEY_MOCK_INTERVAL_SECONDS = "mock-interval-seconds"
    }
}

private class KeystoreSecretBox {
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = ByteArray(1 + cipher.iv.size + ciphertext.size)
        payload[0] = cipher.iv.size.toByte()
        System.arraycopy(cipher.iv, 0, payload, 1, cipher.iv.size)
        System.arraycopy(ciphertext, 0, payload, 1 + cipher.iv.size, ciphertext.size)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLength = payload[0].toInt() and 0xff
            require(ivLength in 12..32 && payload.size > ivLength + 1)
            val iv = payload.copyOfRange(1, 1 + ivLength)
            val ciphertext = payload.copyOfRange(1 + ivLength, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val KEY_ALIAS = "banddrip-secrets-v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
