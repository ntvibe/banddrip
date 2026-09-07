import { ageMinutes } from './reading.js'

export function validateReading(reading, nowMs) {
  if (!reading || reading.type !== 'reading') return false

  if (typeof reading.glucose !== 'number' || !Number.isFinite(reading.glucose) || reading.glucose <= 0) {
    return false
  }

  if (reading.units !== 'mg/dL' && reading.units !== 'mmol/L') return false
  if (ageMinutes(reading.glucoseTimestampMs, nowMs) === null) return false

  if (reading.delta !== null && reading.delta !== undefined) {
    if (typeof reading.delta !== 'number' || !Number.isFinite(reading.delta)) return false
  }

  if (reading.iobUnits !== null && reading.iobUnits !== undefined) {
    if (typeof reading.iobUnits !== 'number' || !Number.isFinite(reading.iobUnits) || reading.iobUnits < 0) {
      return false
    }
    if (ageMinutes(reading.iobTimestampMs, nowMs) === null) return false
  }

  return true
}
