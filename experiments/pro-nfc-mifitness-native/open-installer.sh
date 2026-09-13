#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v adb >/dev/null || { echo 'Android platform-tools (adb) required for this one-time setup.'; exit 1; }
test -f classes.dex || { echo 'Extract the complete Actions artifact beside this script.'; exit 1; }
adb get-state >/dev/null
version=$(adb shell dumpsys package com.xiaomi.wearable | tr -d '\r' | sed -n 's/^[[:space:]]*versionName=//p' | head -1)
case "$version" in
    3.56.1i|3.57.0i) ;;
    *) echo "Unverified Mi Fitness version: $version. Stopping without changes."; exit 1 ;;
esac
paths=$(adb shell pm path com.xiaomi.wearable | tr -d '\r' | sed 's/^package://' | paste -sd: -)
[[ "$paths" =~ ^[a-zA-Z0-9/_.=:+-]+$ ]] || { echo 'Unexpected APK path'; exit 1; }
adb push classes.dex /data/local/tmp/banddrip-native-installer.dex
adb shell "CLASSPATH=/data/local/tmp/banddrip-native-installer.dex app_process / BandDripNativeInstaller '$paths'"
echo 'Use Mi Fitness: enter org.banddrip.app, then select your BandDrip Writer Probe RPK.'
echo 'Record the installation result. Opening the installer is not proof of band support.'
