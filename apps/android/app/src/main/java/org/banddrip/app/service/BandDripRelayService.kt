package org.banddrip.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.banddrip.app.config.AppSettingsStore
import org.banddrip.app.config.RelaySettings
import org.banddrip.app.config.SourceMode
import org.banddrip.app.config.XDripConnectionMode
import org.banddrip.app.core.BandDripEngine
import org.banddrip.app.core.RelayStateStore
import org.banddrip.app.source.GlucoseSource
import org.banddrip.app.source.MockGlucoseSource
import org.banddrip.app.source.NightscoutSource
import org.banddrip.app.source.XDripHttpSource
import org.banddrip.app.source.XDripSource
import org.banddrip.app.transport.VirtualBandTransport

class BandDripRelayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsStore: AppSettingsStore
    private lateinit var stateStore: RelayStateStore
    private lateinit var transport: VirtualBandTransport
    private val engine = BandDripEngine()
    private lateinit var mockSource: MockGlucoseSource
    private var loopJob: Job? = null
    private var nextDueAtMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        settingsStore = AppSettingsStore(this)
        stateStore = RelayStateStore(this)
        transport = VirtualBandTransport { stateStore.savePacket(it) }
        mockSource = MockGlucoseSource(configProvider = { settingsStore.load().mock })
        createNotificationChannel()
        enterForeground("Starting relay…")
        stateStore.setServiceRunning(true)
        startLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        enterForeground(summaryTextSafe())
        when (intent?.action) {
            ACTION_STOP -> {
                settingsStore.setBackgroundEnabled(false)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REFRESH_NOW, ACTION_PROCESS_XDRIP, ACTION_SETTINGS_CHANGED -> {
                nextDueAtMs = 0L
                scope.launch { runOnce(force = true) }
            }
        }
        startLoop()
        return START_STICKY
    }

    override fun onDestroy() {
        stateStore.setServiceRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLoop() {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch {
            while (isActive) {
                try {
                    val settings = settingsStore.load()
                    if (!settings.backgroundEnabled) {
                        stateStore.setStatus("Background relay disabled")
                        delay(2_000)
                        continue
                    }

                    val now = System.currentTimeMillis()
                    if (now >= nextDueAtMs) {
                        runOnce(force = false)
                        nextDueAtMs = now + intervalMs(settings)
                    }
                    delay(1_000)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    val message = safeError(error)
                    stateStore.setStatus("Relay recovered from error: $message")
                    updateNotification("Relay error · retrying")
                    delay(5_000)
                }
            }
        }
    }

    private suspend fun runOnce(force: Boolean) {
        try {
            val settings = settingsStore.load()
            if (!settings.backgroundEnabled && !force) return

            val source = sourceFor(settings)
            if (source == null) {
                stateStore.setStatus("Selected source is not configured")
                updateNotification("Source needs configuration")
                return
            }

            val snapshot = engine.refresh(source, transport, settings.showIob)
            if (snapshot.errorMessage != null) {
                stateStore.setStatus("${snapshot.sourceId}: ${snapshot.errorMessage}")
                updateNotification("${snapshot.sourceId}: connection error")
                return
            }

            val reading = snapshot.reading
            if (reading != null) {
                stateStore.saveReading(reading, "${snapshot.sourceId} connected · reading received")
                updateNotification("${snapshot.sourceId}: ${displayGlucose(reading.glucose)} ${reading.units.wireValue}")
            } else {
                stateStore.setStatus("${snapshot.sourceId}: connected, waiting for glucose")
                updateNotification("${snapshot.sourceId}: waiting for glucose")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val message = safeError(error)
            stateStore.setStatus("Source error: $message")
            updateNotification("Source error · check BandDrip")
        }
    }

    private fun sourceFor(settings: RelaySettings): GlucoseSource? = when (settings.sourceMode) {
        SourceMode.Mock -> mockSource
        SourceMode.Nightscout -> settings.nightscoutUrl.takeIf { it.isNotBlank() }?.let {
            NightscoutSource(
                baseUrl = it,
                accessToken = settings.nightscoutToken.ifBlank { null },
            )
        }
        SourceMode.XDrip -> when (settings.xdripConnectionMode) {
            XDripConnectionMode.Broadcast -> XDripSource(this)
            XDripConnectionMode.LocalServer -> XDripHttpSource(
                serverUrl = settings.xdripServerUrl,
                secret = settings.xdripServerSecret.ifBlank { null },
            )
        }
    }

    private fun intervalMs(settings: RelaySettings): Long = when (settings.sourceMode) {
        SourceMode.Mock -> if (settings.mock.autoCycle) {
            settings.mock.intervalSeconds.coerceIn(2, 60) * 1_000L
        } else {
            30_000L
        }
        SourceMode.Nightscout -> settings.nightscoutPollMinutes.coerceIn(1, 30) * 60_000L
        SourceMode.XDrip -> when (settings.xdripConnectionMode) {
            XDripConnectionMode.Broadcast -> 30_000L
            XDripConnectionMode.LocalServer -> 60_000L
        }
    }

    private fun displayGlucose(value: Double): String = if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        "%.1f".format(value)
    }

    private fun safeError(error: Throwable): String =
        error.message?.trim()?.takeIf { it.isNotBlank() }?.take(220) ?: error::class.java.simpleName

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BandDrip background relay",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps the selected glucose source connected to BandDrip in the background"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun enterForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        return builder
            .setContentTitle("BandDrip relay active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun summaryTextSafe(): String = runCatching {
        val settings = settingsStore.load()
        when (settings.sourceMode) {
            SourceMode.Mock -> "Mock source"
            SourceMode.Nightscout -> "Nightscout source"
            SourceMode.XDrip -> when (settings.xdripConnectionMode) {
                XDripConnectionMode.Broadcast -> "xDrip broadcast source"
                XDripConnectionMode.LocalServer -> "xDrip local server"
            }
        }
    }.getOrElse { "BandDrip relay" }

    companion object {
        const val ACTION_REFRESH_NOW = "org.banddrip.action.REFRESH_NOW"
        const val ACTION_PROCESS_XDRIP = "org.banddrip.action.PROCESS_XDRIP"
        const val ACTION_SETTINGS_CHANGED = "org.banddrip.action.SETTINGS_CHANGED"
        const val ACTION_STOP = "org.banddrip.action.STOP_RELAY"
        private const val CHANNEL_ID = "banddrip-relay"
        private const val NOTIFICATION_ID = 7801

        fun start(context: Context, action: String = ACTION_REFRESH_NOW) {
            val intent = Intent(context, BandDripRelayService::class.java).setAction(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, BandDripRelayService::class.java).setAction(ACTION_STOP))
        }
    }
}
