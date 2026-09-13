# Mi Fitness + BandDrip Installer P1

This is a modified **Mi Fitness 3.57.0i ARM64 APK**, with an additional launcher icon named **BandDrip Installer**. It opens the existing Xiaomi third-party-app installer in the Mi Fitness process. No computer, ADB launcher, Gadgetbridge or extra Android companion is needed to open this test.

It is an installer-access test, **not live glucose delivery**. Login, pairing, installer runtime behavior and RPK acceptance on firmware 3.200.130 are not yet physically validated.

## Before installing

The APK retains package `com.xiaomi.wearable` to preserve Mi Fitness's internal integration. It is signed with a new test certificate. **It cannot upgrade or coexist with the original Mi Fitness installation in the same Android profile.** Replacing stock Mi Fitness requires removing it, which removes its local app data. Preserve any needed data and pairing/authentication backup before choosing to replace it. This build does not modify your phone automatically.

Xiaomi login or other certificate-bound integrations may reject a re-signed APK. The patch does not bypass authentication checks. This is a personal engineering test build, not a Xiaomi-signed update. The CI signing key is disposable; another build may require reinstalling again.

## Test on the phone

1. Install the extracted `.apk` using Android's package installer.
2. Open the normal **Mi Fitness** icon, complete setup, and connect the band.
3. Open the new **BandDrip Installer** icon.
4. Enter the package name matching the existing Writer Probe RPK (`org.banddrip.app` for the BandDrip writer), then use **install third app** to select that RPK.
5. Record the result. A visible installer is not proof that the band implements RPK installation.

This patch adds one activity and manifest entry. It does not change the original Bluetooth commands, force installation after a rejected preparation response, or repurpose health/weather fields. It does not include an xDrip relay.

## Build and verification

Workflow downloads the pinned 3.57.0i base APK and matching ARM64, MDPI and English splits, validates their digests and signatures, merges them with APKEditor 1.4.9, adds the activity using Apktool 3.0.3, aligns/signs the APK and verifies it. All original DEX and native-library payloads must remain byte-identical. A build/signature pass is not a hardware/runtime pass.

Upstream base SHA-256: `8797cf6993c07a7e1362d21f7b2c23a188922c1ded3bc4304d8fe6ecf9bbbc50`.
APK distribution source: Aptoide app ID 75852908. No credentials or private signing material are committed.
