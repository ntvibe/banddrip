package org.banddrip.app.safety

import kotlin.time.Duration.Companion.minutes

object FreshnessPolicy {
    const val STALE_AFTER_MINUTES = 10L
    private val allowedFutureSkew = 5.minutes

    fun ageMinutes(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): Long? {
        if (timestampMs <= 0L) return null
        if (timestampMs > nowMs + allowedFutureSkew.inWholeMilliseconds) return null
        return ((nowMs - timestampMs).coerceAtLeast(0L)) / 60_000L
    }

    fun isStale(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
        val age = ageMinutes(timestampMs, nowMs) ?: return true
        return age >= STALE_AFTER_MINUTES
    }
}
