package org.banddrip.app.core

import org.banddrip.app.model.BandDripReading
import org.banddrip.app.source.GlucoseSource
import org.banddrip.app.transport.BandTransport

data class EngineSnapshot(
    val reading: BandDripReading?,
    val sourceId: String,
    val transportId: String,
    val errorMessage: String? = null,
)

class BandDripEngine {
    suspend fun refresh(
        source: GlucoseSource,
        transport: BandTransport,
        showIob: Boolean,
    ): EngineSnapshot {
        return try {
            val reading = source.latestReading()
            transport.sendSettings(showIob)
            if (reading != null) transport.sendReading(reading)
            EngineSnapshot(
                reading = reading,
                sourceId = source.id,
                transportId = transport.id,
            )
        } catch (error: Exception) {
            EngineSnapshot(
                reading = null,
                sourceId = source.id,
                transportId = transport.id,
                errorMessage = error.message ?: error::class.java.simpleName,
            )
        }
    }
}
