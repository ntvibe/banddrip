export const STALE_AFTER_MINUTES = 10

const TREND_ARROWS = {
  doubleUp: '⇈',
  singleUp: '↑',
  fortyFiveUp: '↗',
  flat: '→',
  fortyFiveDown: '↘',
  singleDown: '↓',
  doubleDown: '⇊',
  unknown: '?'
}

export function trendArrow(trend) {
  return TREND_ARROWS[trend] || TREND_ARROWS.unknown
}

export function ageMinutes(timestampMs, nowMs) {
  if (!timestampMs || timestampMs <= 0) return null

  const now = nowMs || Date.now()
  // Clock skew beyond 5 minutes is considered invalid rather than "fresh".
  if (timestampMs > now + 5 * 60 * 1000) return null

  return Math.max(0, Math.floor((now - timestampMs) / 60000))
}

export function isStale(timestampMs, nowMs) {
  const age = ageMinutes(timestampMs, nowMs)
  return age === null || age >= STALE_AFTER_MINUTES
}

export function formatDelta(delta, units) {
  if (delta === null || delta === undefined || Number.isNaN(delta)) return 'Δ —'

  const decimals = units === 'mmol/L' ? 1 : 0
  const rounded = Number(delta).toFixed(decimals)
  return `${delta > 0 ? '+' : ''}${rounded}`
}

export function formatGlucose(glucose, units) {
  if (glucose === null || glucose === undefined || Number.isNaN(glucose)) return '—'
  return units === 'mmol/L' ? Number(glucose).toFixed(1) : String(Math.round(glucose))
}

export function formatAge(timestampMs, nowMs) {
  const age = ageMinutes(timestampMs, nowMs)
  return age === null ? 'age —' : `${age}m ago`
}

export function formatIob(iobUnits) {
  if (iobUnits === null || iobUnits === undefined || Number.isNaN(iobUnits)) return 'IOB —'
  return `IOB ${Number(iobUnits).toFixed(3)} U`
}
