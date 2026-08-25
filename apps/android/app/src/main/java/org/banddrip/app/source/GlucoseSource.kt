package org.banddrip.app.source

import org.banddrip.app.model.BandDripReading

/**
 * Source adapter boundary for BandDrip.
 *
 * Implementations may read from Nightscout, xDrip+, Juggluco, or another source,
 * but the rest of the app only consumes normalized BandDripReading values.
 */
interface GlucoseSource {
    val id: String

    suspend fun latestReading(): BandDripReading?
}
