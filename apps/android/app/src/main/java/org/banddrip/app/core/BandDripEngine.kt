package org.banddrip.app.core

import kotlinx.coroutines.CancellationException
import org.banddrip.app.model.BandDripReading
import org.banddrip.app.safety.ReadingValidator
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
            if (reading != null) {
                ReadingValidator.requireValid(reading)
                transport.sendReading(reading)
            }
            EngineSnapshot(
                reading = reading,
                sourceId = source.id,
                transportId = transport.id,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            EngineSnapshot(
                reading = null,
                sourceId = source.id,
                transportId = transport.id,
                errorMessage = safeErrorMessage(error),
            )
        }
    }

    private fun safeErrorMessage(error: Throwable): String {
        val message = error.message?.trim().orEmpty()
        return if (message.isNotBlank()) message.take(280) else error::class.java.simpleName
    }
}
