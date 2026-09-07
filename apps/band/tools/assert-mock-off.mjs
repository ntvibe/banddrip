import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const here = path.dirname(fileURLToPath(import.meta.url))
const mockFile = path.resolve(here, '../src/common/dev-mock.js')
const source = fs.readFileSync(mockFile, 'utf8')

if (!/export\s+const\s+DEV_MOCK_MODE\s*=\s*false\b/.test(source)) {
  console.error('Refusing release: DEV_MOCK_MODE must be false in src/common/dev-mock.js')
  process.exit(1)
}

console.log('Release guard passed: emulator mock mode is OFF.')
