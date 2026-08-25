package org.banddrip.app.safety

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshnessPolicyTest {
    private val now = 1_800_000_000_000L

    @Test
    fun nineMinutesIsFresh() {
        assertFalse(FreshnessPolicy.isStale(now - 9 * 60_000L, now))
    }

    @Test
    fun tenMinutesIsStale() {
        assertTrue(FreshnessPolicy.isStale(now - 10 * 60_000L, now))
    }

    @Test
    fun invalidTimestampIsStale() {
        assertTrue(FreshnessPolicy.isStale(0L, now))
    }

    @Test
    fun excessiveFutureClockSkewIsRejected() {
        assertNull(FreshnessPolicy.ageMinutes(now + 6 * 60_000L, now))
    }
}
