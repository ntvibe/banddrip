#!/usr/bin/env node
const fs = require('fs')
const path = require('path')
const { VvdManager, defaultSDKHome, defaultVvdHome } = require('@aiot-toolkit/emulator')

async function main() {
  const packageName = process.argv[2]
  const output = process.argv[3]
  if (!packageName || !output) {
    console.error('usage: run-and-capture-via-sdk.cjs <package> <output.png>')
    process.exit(2)
  }

  const vvdName = process.env.BANDDRIP_VVD_NAME || 'BandDrip_RealFirmwareLab'
  const manager = new VvdManager({ sdkHome: defaultSDKHome, vvdHome: defaultVvdHome })

  // Re-attach to the already-running VVD through Xiaomi's own SDK. This is the
  // same API documented by @aiot-toolkit/emulator for launching an app and
  // controlling/capturing the emulator display.
  const { emulatorInstance, getAgent } = await manager.startVvd({ vvdName })
  if (!emulatorInstance || typeof emulatorInstance.startApp !== 'function') {
    throw new Error('Xiaomi emulator SDK did not return a usable emulatorInstance.startApp()')
  }

  console.log(`Starting ${packageName} through Xiaomi emulatorInstance.startApp()`)
  await Promise.resolve(emulatorInstance.startApp(packageName))

  const agent = await getAgent()
  if (!agent) throw new Error('Xiaomi emulator SDK did not return a display agent')

  // Wake/activate the display through the SDK agent before capturing.
  if (typeof agent.sendMouse === 'function') {
    await agent.sendMouse({ x: 2, y: 2, buttons: 1 })
    await new Promise(r => setTimeout(r, 90))
    await agent.sendMouse({ x: 2, y: 2, buttons: 0 })
  }
  await new Promise(r => setTimeout(r, 1200))

  let image
  if (typeof agent.getScreenshot === 'function') {
    image = await agent.getScreenshot()
  } else {
    throw new Error('Xiaomi emulator SDK display agent has no getScreenshot()')
  }

  // SDK versions may return Buffer directly or wrap it.
  if (!Buffer.isBuffer(image)) {
    image = image && (image.image || image.data || image.buffer)
  }
  if (image && image.type === 'Buffer' && Array.isArray(image.data)) {
    image = Buffer.from(image.data)
  }
  if (!Buffer.isBuffer(image) || image.length === 0) {
    throw new Error(`Xiaomi SDK returned no screenshot bytes (${typeof image})`)
  }

  fs.mkdirSync(path.dirname(output), { recursive: true })
  fs.writeFileSync(output, image)
  console.log(JSON.stringify({ status: 'ok', packageName, vvdName, output, bytes: image.length }))
}

main().catch(error => {
  console.error(error && (error.stack || error.message) || error)
  process.exit(1)
})
