#!/usr/bin/env node
import fs from 'node:fs'
import path from 'node:path'

const fixtureId = process.argv[2]
const projectDir = process.argv[3] || 'apps/band'
const baseDir = process.argv[4] || '/tmp/banddrip-render-base'

const fixtures = {
  'fresh-default': {
    glucoseText: '112', trendText: '↘', deltaText: '+6', ageText: '3m ago',
    iobText: 'IOB 0.250 U', showIob: true, stale: false
  },
  'stale-glucose': {
    glucoseText: '112', trendText: '↘', deltaText: '+6', ageText: '12m ago',
    iobText: 'IOB 0.250 U', showIob: true, stale: true
  },
  'large-three-digit': {
    glucoseText: '350', trendText: '↑', deltaText: '+22', ageText: '1m ago',
    iobText: 'IOB 1.875 U', showIob: true, stale: false
  },
  'mmol-format': {
    glucoseText: '6.2', trendText: '→', deltaText: '+0.3', ageText: '2m ago',
    iobText: 'IOB 0.125 U', showIob: true, stale: false
  },
  'stale-iob': {
    glucoseText: '98', trendText: '→', deltaText: '-2', ageText: '2m ago',
    iobText: 'IOB —', showIob: true, stale: false
  },
  'missing-delta': {
    glucoseText: '104', trendText: '→', deltaText: 'Δ —', ageText: '1m ago',
    iobText: 'IOB 0.000 U', showIob: true, stale: false
  }
}

if (!fixtures[fixtureId]) {
  console.error(`Unknown firmware render fixture: ${fixtureId}`)
  process.exit(2)
}

const basePage = path.join(baseDir, 'index.ux')
const baseManifest = path.join(baseDir, 'manifest.json')
if (!fs.existsSync(basePage) || !fs.existsSync(baseManifest)) {
  console.error(`Missing render baseline under ${baseDir}`)
  process.exit(2)
}

const source = fs.readFileSync(basePage, 'utf8')
const template = source.match(/<template>[\s\S]*?<\/template>/)?.[0]
const style = source.match(/<style>[\s\S]*?<\/style>/)?.[0]
if (!template || !style) {
  console.error('Could not extract production template/style from index.ux')
  process.exit(2)
}

const state = fixtures[fixtureId]
const script = `<script>\nexport default {\n  private: ${JSON.stringify({
  devMockMode: true,
  hasReading: true,
  ...state
}, null, 4)}\n}\n</script>`

// The production template and CSS are copied unchanged. Only the runtime script
// is replaced so visual CI tests Vela rendering without phone transport/storage.
const targetPage = path.join(projectDir, 'src/pages/index/index.ux')
fs.writeFileSync(targetPage, `${template}\n\n${style}\n\n${script}\n`)

const manifest = JSON.parse(fs.readFileSync(baseManifest, 'utf8'))
manifest.features = []
manifest.config = {
  ...(manifest.config || {}),
  logLevel: 'log'
}
manifest.router = {
  entry: 'pages/index',
  pages: {
    'pages/index': {
      component: 'index'
    }
  }
}
delete manifest.minAPILevel
if (manifest.minPlatformVersion == null) manifest.minPlatformVersion = 1000

fs.writeFileSync(path.join(projectDir, 'src/manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`)
fs.writeFileSync(path.join(projectDir, 'src/app.ux'), `<script>\nexport default {\n  onCreate() { console.log('BandDrip firmware render created') },\n  onDestroy() { console.log('BandDrip firmware render destroyed') }\n}\n</script>\n`)

console.log(`Prepared ${fixtureId}: exact production template/style + emulator-safe fixture runtime`)
