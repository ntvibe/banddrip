#!/usr/bin/env bash
set -euo pipefail

SERIAL="${EMU_SERIAL:-emulator-5554}"
PROJECT_DIR="${1:-apps/band}"
ADB=(adb -s "$SERIAL")
APP_ROOT="/data/quickapp/app"

MANIFEST="$PROJECT_DIR/src/manifest.json"
if [ ! -f "$MANIFEST" ]; then
  echo "Missing manifest: $MANIFEST" >&2
  exit 1
fi

PKG=$(node -e "const fs=require('fs'); console.log(JSON.parse(fs.readFileSync(process.argv[1],'utf8')).package)" "$MANIFEST")
RPK=$(find "$PROJECT_DIR/dist" -maxdepth 1 -type f -name '*.rpk' | sort | tail -n 1)

if [ -z "$RPK" ] || [ ! -f "$RPK" ]; then
  echo "No RPK found under $PROJECT_DIR/dist" >&2
  exit 1
fi

"${ADB[@]}" get-state >/dev/null

echo "Deploying $(basename "$RPK") as $PKG to $SERIAL"
"${ADB[@]}" shell "mkdir -p $APP_ROOT"
"${ADB[@]}" push "$RPK" "$APP_ROOT/$PKG.rpk"
"${ADB[@]}" shell "rm -rf $APP_ROOT/$PKG"
"${ADB[@]}" shell "mkdir -p $APP_ROOT/$PKG"
"${ADB[@]}" shell "unzip -o $APP_ROOT/$PKG.rpk -d $APP_ROOT/$PKG"

# NuttX nsh supports the commands below but not GNU-style ls flags/redirection reliably.
echo "Installed app files:"
"${ADB[@]}" shell "ls $APP_ROOT/$PKG"

echo "Launching through the Vela Quick App runtime..."
"${ADB[@]}" shell "vapp app/$PKG &"
sleep 4

echo "Guest process snapshot after launch:"
"${ADB[@]}" shell "ps" || true

echo "BandDrip deploy command completed."
