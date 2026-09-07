package org.banddrip.app.install

import org.junit.Assert.assertEquals
import org.junit.Test

class MiFitnessAuthKeyReaderTest {
    @Test
    fun `extracts known Mi Fitness auth key fields in log order`() {
        val log = """
            noise
            encryptKey = 0123456789ABCDEF0123456789ABCDEF
            token: fedcba9876543210fedcba9876543210
            authKey=00112233445566778899aabbccddeeff
            huamiAuthKey \"ffeeddccbbaa99887766554433221100\"
        """.trimIndent()

        assertEquals(
            listOf(
                "0123456789abcdef0123456789abcdef",
                "fedcba9876543210fedcba9876543210",
                "00112233445566778899aabbccddeeff",
                "ffeeddccbbaa99887766554433221100",
            ),
            MiFitnessAuthKeyReader.parseAuthKeys(log),
        )
    }

    @Test
    fun `ignores non hex tokens and wrong lengths`() {
        val log = """
            token = not-a-key
            authKey = 00112233
            token = 00112233445566778899aabbccddeeff00
        """.trimIndent()

        assertEquals(emptyList<String>(), MiFitnessAuthKeyReader.parseAuthKeys(log))
    }
}
