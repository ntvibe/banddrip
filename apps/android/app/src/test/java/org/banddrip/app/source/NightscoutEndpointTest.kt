package org.banddrip.app.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NightscoutEndpointTest {
    @Test
    fun `full tracking url extracts token and strips query`() {
        val endpoint = NightscoutEndpoint.parse(
            "https://example.nightscout.site/?token=track-abc123",
        )

        assertEquals("https://example.nightscout.site", endpoint.baseUrl)
        assertEquals("track-abc123", endpoint.token)
    }

    @Test
    fun `separate token overrides embedded token`() {
        val endpoint = NightscoutEndpoint.parse(
            "https://example.nightscout.site/?token=old-token",
            "new-token",
        )

        assertEquals("https://example.nightscout.site", endpoint.baseUrl)
        assertEquals("new-token", endpoint.token)
    }

    @Test
    fun `clean url remains clean with no token`() {
        val endpoint = NightscoutEndpoint.parse("https://example.nightscout.site/")
        assertEquals("https://example.nightscout.site", endpoint.baseUrl)
        assertNull(endpoint.token)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `nightscout rejects plain http`() {
        NightscoutEndpoint.parse("http://example.nightscout.site")
    }
}
