# Native Mi Fitness route: bytecode-backed installer

Investigation: 2026-09-13. Target remains Pro NFC Global **3.200.130**.
This is a one-time installer launcher, not a working glucose transport.
It uses stock Mi Fitness; no Gadgetbridge process, APK replacement, firmware modification or runtime ADB glucose relay is involved.

## What was actually inspected

Downloaded distribution APKs from Aptoide and decompiled with JADX 1.5.3:

| Version | SHA-256 |
| --- | --- |
| 3.56.1i | `d26f0b8d78370f3ba6257ac46fe2d31e15805ac5a3a5fea7ecba113676dcebf9` |
| 3.57.0i | `8797cf6993c07a7e1362d21f7b2c23a188922c1ded3bc4304d8fe6ecf9bbbc50` |

These identify the inspected samples. Their APK signing blocks have not been independently verified against an official Xiaomi certificate. No downloaded proprietary APK is redistributed here; the launcher uses the user's existing Mi Fitness installation.

Source provenance: [3.56.1i download metadata](https://ws75.aptoide.com/api/7/app/get/app_id=75424293), [3.57.0i distribution page](https://mi-wear.en.aptoide.com/app).

**PROVEN IN BYTECODE (not on target hardware):**

- Both versions contain `com.xiaomi.xms.wearable.ui.debug.ThirdAppDebugFragment`.
- The fragment's `onCreate` reads `isFromAdb` from its arguments. `bindView` accepts `packageName`, `versionCode`, and `filePath`, checks the file exists, and calls `doPrepareInstall`.
- `doPrepareInstall` checks device connectivity and calls `com.xiaomi.xms.wearable.extensions.DeviceModelExtKt.prepareInstallApp`.
- In inspected 3.57.0i, that method checks Bluetooth device type, creates type **20**, subtype **1**, with package/version/size, and calls `DeviceContact.call`. Reply status zero returns the expected slice length. Nonzero status becomes an error; missing expected payload becomes `-1`.
- `sendRpkFile` calls `DeviceContact.sendFile` with internal file-category arguments **4, 0**. Those arguments are API-level identifiers, not a claim that the wire MASS type differs from Gadgetbridge's type 64.
- The file-picker handler also reaches preparation, but hardcodes version **10**. This should be considered when diagnosing replacement/version failures.
- There is no region or package whitelist check in these inspected preparation method bodies. That does **not** establish that all deeper transport, SDK, or band checks are absent.

This narrows the patch hypothesis: the installer UI can be reached without re-signing the app. Bypassing its successful-prepare requirement would start a transfer without establishing a band receiver; it is not a justified fix.

## Small native experiment

1. Keep a backup of the working Gadgetbridge configuration. Use stock Mi Fitness paired/connected to the band for this experiment. Do not run both connection owners concurrently. Moving pairing between clients has not been validated by this launcher; do not reset the band just to use it.
2. Extract the `BandDrip-MiFitness-native-installer` Actions artifact on a computer with Android platform-tools. Connect your phone with authorized USB debugging.
3. Run `bash open-installer.sh`. It checks Mi Fitness version, loads its installed APK classes, and requests its existing installer screen. Unsupported versions stop before launch. Android hidden-API/OEM behavior can still reject the launch.
4. On the phone, enter `org.banddrip.app` and choose the existing BandDrip Writer Probe RPK from the phone's file picker. Verify the package against that RPK's manifest if using a different build.
5. Record the result, especially `prepareInstallApp`, `prepareInstallThirdApp`, and `sendRpkFile` log messages. Avoid sharing full logs containing authentication or health information.

This launcher does not automatically install, uninstall, unpair, change app data, or reconfigure the watch. It needs ADB only for this one-time screen launch. A successful activity-launch return is not a successful RPK installation.

If installation succeeds, the existing writer establishes file visibility. The next gate is Mi Fitness Binder/interconnect messages changing 123 to 147 without reinstalling the face. That gate is still pending; no real xDrip delivery is claimed.

## Emulator evidence and limits

[m0tral/MiWatchEmulator](https://github.com/m0tral/MiWatchEmulator) supplies a Windows emulator archive (~733 MB). Its README lists Watch S3 and Redmi Watch 4/5 configurations, with Band 8/9 Pro planned. It does not document a Pro 10 Global 3.200.130 image. [EasyFace](https://github.com/m0tral/EasyFace) identifies its emulator basis as Watch S3.

[oryonatan/xiaomi-band-development](https://github.com/oryonatan/xiaomi-band-development) documents a separate AIoT IDE emulator flow: ADB push, unzip, and `vapp` launch. That bypasses the Bluetooth installer dispatcher we need to prove. Its physical deployment example is an international Band 8 Pro.

An emulator can test the app runtime and synthetic state changes. Such a pass cannot establish that 3.200.130 implements type 20/1, background interconnect, or matching filesystem access. No exact-firmware emulation or physical test was performed in this continuation. The Windows emulator was inspected through its published documentation/release metadata, not executed.

## Reproducing source inspection

With the identified APK and JADX 1.5.3:

```sh
jadx -r --single-class com.xiaomi.xms.wearable.ui.debug.ThirdAppDebugFragment --single-class-output ThirdAppDebugFragment.java mifitness.apk
jadx -r --single-class com.xiaomi.xms.wearable.extensions.DeviceModelExtKt --single-class-output DeviceModelExtKt.java mifitness.apk
```

Launcher mechanism adapted from the MIT-licensed [deploy.sh](https://github.com/oryonatan/xiaomi-band-development/blob/develop/scripts/deploy.sh). Original license included as `UPSTREAM-LICENSE`. Added version checks, split APK class path, explicit launch-result checking, and fail-closed errors. Compilation is CI-tested; executing inside Android still requires a real or suitable emulated Android system.
