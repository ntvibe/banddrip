import test from 'node:test'
import assert from 'node:assert/strict'

import {
  STALE_AFTER_MINUTES,
  ageMinutes,
  formatAge,
  formatDelta,
  formatGlucose,
  formatIob,
  isStale,
  trendArrow
} from '../src/common/reading.js'
import { isSupportedPacket, PROTOCOL_VERSION } from '../src/common/protocol.js'
import { validateReading } from '../src/common/validation.js'

const now = 1_800_000_000_000
const minute = 60_000

function reading(overrides = {}) {
  return {
    protocolVersion: PROTOCOL_VERSION,
    type: 'reading',
    glucose: 112,
    units: 'mg/dL',
    glucoseTimestampMs: now - 3 * minute,
    delta: 6,
    trend: 'fortyFiveDown',
    iobUnits: 0.25,
    iobTimestampMs: now - 2 * minute,
    ...overrides
  }
}

test('stale boundary is exactly 10 minutes', () => {
  assert.equal(STALE_AFTER_MINUTES, 10)
  assert.equal(isStale(now - 9 * minute, now), false)
  assert.equal(isStale(now - 10 * minute, now), true)
})

test('age is exact whole elapsed minutes and rejects excessive future skew', () => {
  assert.equal(ageMinutes(now - 12 * minute, now), 12)
  assert.equal(formatAge(now - 12 * minute, now), '12m ago')
  assert.equal(ageMinutes(now + 6 * minute, now), null)
})

test('formats mg/dL, mmol/L, delta and IOB deterministically', () => {
  assert.equal(formatGlucose(112.4, 'mg/dL'), '112')
  assert.equal(formatGlucose(6.24, 'mmol/L'), '6.2')
  assert.equal(formatDelta(6, 'mg/dL'), '+6')
  assert.equal(formatDelta(-3, 'mg/dL'), '-3')
  assert.equal(formatDelta(0.3, 'mmol/L'), '+0.3')
  assert.equal(formatDelta(null, 'mg/dL'), 'Δ —')
  assert.equal(formatIob(0.25), 'IOB 0.250 U')
})

test('maps every supported trend to a glanceable symbol', () => {
  assert.equal(trendArrow('doubleUp'), '⇈')
  assert.equal(trendArrow('singleUp'), '↑')
  assert.equal(trendArrow('fortyFiveUp'), '↗')
  assert.equal(trendArrow('flat'), '→')
  assert.equal(trendArrow('fortyFiveDown'), '↘')
  assert.equal(trendArrow('singleDown'), '↓')
  assert.equal(trendArrow('doubleDown'), '⇊')
  assert.equal(trendArrow('unexpected'), '?')
})

test('protocol version guard rejects incompatible packets', () => {
  assert.equal(isSupportedPacket(reading()), true)
  assert.equal(isSupportedPacket(reading({ protocolVersion: 2 })), false)
  assert.equal(isSupportedPacket(null), null)
})

test('wearable validation accepts good reading and rejects corrupt data', () => {
  assert.equal(validateReading(reading(), now), true)
  assert.equal(validateReading(reading({ glucose: 0 }), now), false)
  assert.equal(validateReading(reading({ glucose: Number.NaN }), now), false)
  assert.equal(validateReading(reading({ units: 'banana' }), now), false)
  assert.equal(validateReading(reading({ glucoseTimestampMs: 0 }), now), false)
  assert.equal(validateReading(reading({ delta: Number.POSITIVE_INFINITY }), now), false)
  assert.equal(validateReading(reading({ iobUnits: -0.1 }), now), false)
})
