import file from '@system.file'

export const GLANCE_STATE_URI = 'internal://files/glance.json'

/**
 * Experimental bridge for a native/Lua BandDrip watch face.
 *
 * Xiaomi Vela maps internal://files into the Quick App's persistent sandbox.
 * Band 10 Lua watch-face research indicates that the native watch-face runtime
 * may be able to read the underlying NuttX path. Hardware validation is still
 * required; failure to persist here must never affect the normal RPK display.
 */
export function persistGlanceState(reading, showIob) {
  if (!reading) return

  const state = {
    version: 1,
    glucose: reading.glucose,
    units: reading.units,
    delta: reading.delta == null ? null : reading.delta,
    trend: reading.trend,
    glucoseTimestampMs: reading.glucoseTimestampMs,
    iobUnits: reading.iobUnits == null ? null : reading.iobUnits,
    iobTimestampMs: reading.iobTimestampMs == null ? null : reading.iobTimestampMs,
    showIob: showIob !== false,
    writtenAtMs: Date.now()
  }

  file.writeText({
    uri: GLANCE_STATE_URI,
    text: JSON.stringify(state),
    append: false,
    success: () => console.info('BandDrip glance state persisted'),
    fail: (data, code) => console.warn(`BandDrip glance state write failed: ${code} ${data || ''}`)
  })
}

export function persistGlanceSettings(reading, showIob) {
  if (!reading) return
  persistGlanceState(reading, showIob)
}
