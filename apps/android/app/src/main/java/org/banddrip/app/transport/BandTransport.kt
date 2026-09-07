package org.banddrip.app.transport

import org.banddrip.app.model.BandDripReading

interface BandTransport {
    val id: String

    suspend fun sendReading(reading: BandDripReading)
    suspend fun sendSettings(showIob: Boolean)
}
