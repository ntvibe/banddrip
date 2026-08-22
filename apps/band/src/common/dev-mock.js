// Emulator-only development fixtures.
// Keep DEV_MOCK_MODE false in committed production code.
export const DEV_MOCK_MODE = false

export function createMockReading(ageMinutes) {
  const now = Date.now()
  const age = ageMinutes == null ? 3 : ageMinutes

  return {
    type: 'reading',
    glucose: 112,
    units: 'mg/dL',
    delta: 6,
    trend: 'fortyFiveDown',
    glucoseTimestampMs: now - age * 60 * 1000,
    iobUnits: 0.25,
    iobTimestampMs: now - 2 * 60 * 1000
  }
}

export const DEV_MOCK_SEQUENCE = [
  { label: 'fresh', ageMinutes: 3 },
  { label: 'stale', ageMinutes: 12 }
]
