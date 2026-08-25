package org.banddrip.app.transport

import org.banddrip.app.model.BandDripReading
import org.banddrip.app.protocol.ProtocolCodec

class VirtualBandTransport(
    private val onPacket: (String) -> Unit = {},
) : BandTransport {
    override val id: String = "virtual"

    var lastPacket: String? = null
        private set

    override suspend fun sendReading(reading: BandDripReading) {
        emit(ProtocolCodec.encodeReading(reading))
    }

    override suspend fun sendSettings(showIob: Boolean) {
        emit(ProtocolCodec.encodeSettings(showIob))
    }

    private fun emit(packet: String) {
        lastPacket = packet
        onPacket(packet)
    }
}
