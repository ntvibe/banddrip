// Emulator-only development fixtures.
// Keep DEV_MOCK_MODE false and DEV_MOCK_FIXED_LABEL empty in committed production code.
export const DEV_MOCK_MODE = false
export const DEV_MOCK_FIXED_LABEL = ''

export const DEV_MOCK_SEQUENCE = [
  {
    label: 'fresh-default',
    glucose: 112,
    units: 'mg/dL',
    delta: 6,
    trend: 'fortyFiveDown',
    glucoseAgeMinutes: 3,
    iobUnits: 0.25,
    iobAgeMinutes: 2
  },
  {
    label: 'stale-glucose',
    glucose: 112,
    units: 'mg/dL',
    delta: 6,
    trend: 'fortyFiveDown',
    glucoseAgeMinutes: 12,
    iobUnits: 0.25,
    iobAgeMinutes: 2
  },
  {
    label: 'large-three-digit',
    glucose: 350,
    units: 'mg/dL',
    delta: 22,
    trend: 'singleUp',
    glucoseAgeMinutes: 1,
    iobUnits: 1.875,
    iobAgeMinutes: 1
  },
  {
    label: 'mmol-format',
    glucose: 6.2,
    units: 'mmol/L',
    delta: 0.3,
    trend: 'flat',
    glucoseAgeMinutes: 2,
    iobUnits: 0.125,
    iobAgeMinutes: 2
  },
  {
    label: 'stale-iob',
    glucose: 98,
    units: 'mg/dL',
    delta: -2,
    trend: 'flat',
    glucoseAgeMinutes: 2,
    iobUnits: 0.75,
    iobAgeMinutes: 12
  },
  {
    label: 'missing-delta',
    glucose: 104,
    units: 'mg/dL',
    delta: null,
    trend: 'flat',
    glucoseAgeMinutes: 1,
    iobUnits: 0,
    iobAgeMinutes: 1
  }
]

export function mockIndexForLabel(label) {
  if (!label) return 0
  const index = DEV_MOCK_SEQUENCE.findIndex(step => step.label === label)
  return index >= 0 ? index : 0
}

export function createMockReading(step) {
  const now = Date.now()
  return {
    type: 'reading',
    protocolVersion: 1,
    glucose: step.glucose,
    units: step.units,
    delta: step.delta,
    trend: step.trend,
    glucoseTimestampMs: now - step.glucoseAgeMinutes * 60 * 1000,
    iobUnits: step.iobUnits,
    iobTimestampMs: now - step.iobAgeMinutes * 60 * 1000
  }
}
