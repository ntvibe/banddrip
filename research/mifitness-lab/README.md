# Mi Fitness reverse-engineering lab

Purpose: compare Xiaomi's official Mi Fitness builds with m0tral's modded builds and trace the Xiaomi Smart Band 10 Pro / NFC (M2552B1, p67) developer, watchface, third-party-app, and file-transfer paths.

## Keep proprietary APKs out of Git

Do **not** commit Xiaomi or m0tral APK binaries here. Keep them in a local `originals/` folder or another private artifact store. This repo stores only reproducible scripts, notes, hashes, and derived findings.

The current official 3.57.0i XAPK is stored privately in Nikola's ChatGPT Library under `/BandDrip/research/mifitness-lab/` so it can be rematerialized for future analysis without publishing the proprietary binary.

## Target comparison

1. Official Mi Fitness 3.33.6i
2. m0tral Mi Fitness 3.33.6i v141/v142
3. Official Mi Fitness 3.56.1i / 3.57.0i
4. Any BandDrip experimental patch built from those versions

## Acquired official 3.57.0i input

- XAPK size: `204358672` bytes
- XAPK SHA-256: `96b99cc0c0c62aa27e9abd45517bd563eab8a7e92a7ff9eeb9c3df5a82245c83`
- Base APK: `com.xiaomi.wearable.apk`
- Base APK size: `136230656` bytes
- Base APK SHA-256: `8797cf6993c07a7e1362d21f7b2c23a188922c1ded3bc4304d8fe6ecf9bbbc50`
- XAPK also contains ARM64, English-language, and hdpi split APKs.

The XAPK hash matches APKPure's published 3.57.0i hash. A one-shot GitHub Actions workflow was used only to fetch the public APKPure download endpoint; the workflow was deleted after the artifact was retrieved.

## Questions

- Where is `ThirdAppDebugFragment` exposed/hidden?
- What gates `isSupportThirdPartyApp` and `isSupportDeviceDebugMode`?
- How does current Mi Fitness recognize M2552B1 / p67?
- Does P67 use `THIRDPARTY_APP`, or a newer service/path?
- Which device-capability bits are checked before `prepareInstallApp`?
- Can WATCH_FACE / MASS / another existing service provide a small mutable data path to a Lua watchface?

## Current confirmed facts

- m0tral 3.33.6i builds contain `ThirdAppDebugFragment`, `isSupportThirdPartyApp`, `thirdparty_app`, and `prepareInstallApp` code paths.
- Official Mi Fitness 3.57.0i still contains `ThirdAppDebugFragment`, `ThirdPartyApp`, `THIRDPARTY_APP`, `prepareInstallApp`, RPK/sendRpkFile-related strings, watchface handling, and device-debug capability strings.
- Plain string scans do **not** find `M2552B1`, `M2558B1`, or `p67gl` in either the m0tral builds or official 3.57.0i. Generic `p67` matches are noisy/mostly incidental, so device support is likely encoded through IDs/capabilities/server data rather than a simple plaintext model whitelist.
- Public P67 research tested official Mi Fitness 3.56.1i's `ThirdAppDebugFragment`: transport ACK succeeded, but `prepareInstallApp` timed out at the application layer on global Band 10 Pro firmware.
- That means the phone-side debug machinery exists; the key questions are UI/capability gating and the watch firmware's available service handlers.

## Local workflow

Put APKs in `originals/`, then run:

```bash
python scan_apk.py originals/*.apk > report.json
```

For deeper work, use JADX/APKTool/Ghidra locally when available. Start from targeted strings/classes rather than decompiling blindly:

- `ThirdAppDebugFragment`
- `prepareInstallApp`
- `isSupportThirdPartyApp`
- `isSupportDeviceDebugMode`
- `thirdparty_app`
- `watchface`
- `MASS`
- `M2552B1`
- `p67`

## Safety

Do not cross-flash CN firmware or test firmware patches on the primary NFC/payment band. APK-side analysis and watchface experiments come first.
