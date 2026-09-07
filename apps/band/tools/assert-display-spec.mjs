import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const specPath = fileURLToPath(new URL('../../../packages/display-spec/band10-v1.json', import.meta.url))
const uxPath = fileURLToPath(new URL('../src/pages/index/index.ux', import.meta.url))
const spec = JSON.parse(readFileSync(specPath, 'utf8'))
const ux = readFileSync(uxPath, 'utf8').toLowerCase()

const expected = [
  `width: ${spec.designWidth}px;`,
  `height: ${spec.designHeight}px;`,
  `padding-top: ${spec.contentTop}px;`,
  `width: ${spec.glucoseRowWidth}px;`,
  `height: ${spec.glucoseRowHeight}px;`,
  `font-size: ${spec.glucoseFontSize}px;`,
  `font-weight: ${spec.glucoseFontWeight};`,
  `font-size: ${spec.trendFontSize}px;`,
  `font-weight: ${spec.trendFontWeight};`,
  `margin-left: ${spec.trendMarginLeft}px;`,
  `margin-top: ${spec.metaMarginTop}px;`,
  `font-size: ${spec.metaFontSize}px;`,
  `font-size: ${spec.separatorFontSize}px;`,
  `margin-left: ${spec.separatorMargin}px;`,
  `margin-right: ${spec.separatorMargin}px;`,
  `margin-top: ${spec.iobMarginTop}px;`,
  `font-size: ${spec.iobFontSize}px;`,
  `font-weight: ${spec.iobFontWeight};`,
  `background-color: ${spec.colors.background.toLowerCase()};`,
  `color: ${spec.colors.primary.toLowerCase()};`,
  `color: ${spec.colors.stale.toLowerCase()};`,
  `color: ${spec.colors.delta.toLowerCase()};`,
  `color: ${spec.colors.separator.toLowerCase()};`,
  `color: ${spec.colors.age.toLowerCase()};`,
  `color: ${spec.colors.iob.toLowerCase()};`,
  'text-decoration: line-through;',
]

const missing = expected.filter(token => !ux.includes(token.toLowerCase()))
if (missing.length > 0) {
  console.error('Vela UI drifted from packages/display-spec/band10-v1.json')
  for (const token of missing) console.error(`  missing: ${token}`)
  process.exit(1)
}

console.log(`Display spec v${spec.version} matches Vela ${spec.designWidth}x${spec.designHeight} layout`)
