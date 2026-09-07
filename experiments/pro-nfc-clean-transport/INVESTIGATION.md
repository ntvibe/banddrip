# Clean transport: evidence and decisions

## What already exists

Inspected repository branches and PRs #1–#8. The virtual MVP is on `scaffold/public-mvp` at `ffe328e`; hardware v2 is at `5c2d9cd`. PR #3 contains v2; #4 is the separate writer RPK; #5 merged an alarm probe; #6 and #7 concern weather. Those historical carriers are outside the current task and remain untouched.

The physically validated v2 source is `experiments/pro-nfc-watchface-v2/app/lua/main.lua`. It reads WatchDrip and BandDrip candidate files, renders glucose/delta/age/IOB, and refreshes every **15 seconds while active**. It pauses the timer on page pause and reads immediately on resume. Even if transport becomes instantaneous, the unchanged face cannot guarantee a few-second active-screen update. Reduce or replace that timer only after transport is proven. Preserve its source and known working artifact throughout.

Android `BandTransport` is an interface; `VirtualBandTransport` only serializes and emits JSON. `BandDripRelayService` on the inspected v2 branch instantiates that virtual implementation and stores packets locally. It does not send them to Xiaomi. The source ingestion and display architecture can be reused after a transport test passes.

The Vela app registers `system.interconnect` message callbacks, validates protocol version and readings, and calls `persistGlanceState`, writing `internal://files/glance.json`. Its manifest package is `org.banddrip.app`. This is working code structure, not proof of installation, background delivery, or shared filesystem visibility on the Pro Global.

## A: Gadgetbridge and native watchface resources

**PROVEN in inspected source:** Gadgetbridge's `MiBand10ProCoordinator` selects `BT_CLASSIC`. Xiaomi SPP support negotiates the transport. The user's successful pairing/vibration proves communication, but does not identify BLE versus SPP; record the actual coordinator/connection.

Gadgetbridge 0.93.0 is pinned at `a09e037374d0013ffc31978fc1d1ada2943280e4` for our build. Current Codeberg source was also inspected at `54fd799666a9a39e2331f8779293cd06c57e7648`.

- `XiaomiWatchfaceService`: type 4; list 0, set 1, delete 2, prepare installation 4. A ready response triggers the complete face upload, then selection.
- `XiaomiDataUploadService`: type 22/subtype 0; upload type 16 face, 32 firmware, 50 notification icon, 64 RPK. Prepare carries type, MD5, length. Payload framing/chunking adds length/checksums and supports resume offsets.
- Resume is a continuation of a transfer, **not** an independent overwrite of an arbitrary file in an installed face.
- There is no arbitrary destination path in this upload request. Merely sending JSON as upload type 16 does not establish that firmware will save it as `glance.json`.
- The notification service routes notification content/icons, not arbitrary Lua events. No direct custom-protobuf-to-Lua callback was found in Gadgetbridge.

**Important additional source evidence:** AstroBox's richer schema includes watchface operations absent from Gadgetbridge's implementation: 4/10 supported-data, 4/11 edit, 4/12 background-image result, 4/13 font result. MASS types 48 and 53 upload watchface images/fonts. These are legitimate independent resource updates, but they are typed image/font operations, not general filesystem APIs.

`EditRequest.literal` (field 16) contains `WatchFaceLiteral`: item IDs, repeated text strings, font and optional font size. This is the strongest small-packet native face lead. It is semantically suitable for custom displayed text, unlike encoding glucose into weather. However, the firmware's literal-support flag, required item IDs, font handling, and relationship to the current Lua container must be established. Source presence does not prove 3.200.130 implements it. A positive edit acknowledgment alone will not prove that Lua received or displayed text.

**DISPROVEN as a description of current Gadgetbridge:** “Gadgetbridge already exposes an arbitrary file writer for installed faces.” **NOT disproven:** an undocumented firmware mechanism or additional supported watchface edit operation.

Sources: [Gadgetbridge Xiaomi services](https://codeberg.org/Freeyourgadget/Gadgetbridge/src/commit/a09e037374d0013ffc31978fc1d1ada2943280e4/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/xiaomi/services), [full watchface schema](https://github.com/AstralSightStudios/AstroBox-NG-Module-Pb/blob/03a92010056dd41af114f6f46fd612104b27bd7b/protos/xiaomi/wear_watch_face.proto), [AstroBox watchface API](https://github.com/AstralSightStudios/AstroBox-NG-Module-Core/blob/95541fda829ef9fcb010e387d44782c9601fa9e0/src/device/watchface.rs).

## B/C: RPK and Mi Fitness

`XiaomiRpkService.installRpk` sends type 20/subtype 1 with package, version code and size. Only a prepare response interpreted as ready starts MASS type 64. Nonzero status returns without transfer; silence also prevents transfer. Parsing the RPK and hiding the install button happens upstream of proof that the band accepted installation. An empty app-manager screen is not evidence of a valid empty-list response.

**Exact cause on 3.200.130: unresolved.** No device log, firmware image or firmware hash from this device was supplied. The existing UI observation cannot discriminate missing service, rejected preparation, wrong transport/session, or client behavior. The P0 requests are designed to narrow this, not claim certainty.

Strong public evidence exists for type-20 silence on **3.201.016**, reproduced by four clients, including Mi Fitness 3.56.1i's `ThirdAppDebugFragment` and `prepareInstallApp`. The report distinguishes transport ACK from missing application response and an 8-second timeout (`-6`). It also reports container rejection when smuggling RPK through face upload. These are another owner's findings, not our reproductions or evidence for 3.200.130.

There is a material source disagreement: that report finds QuickJS and resource-loader `install_quickapp` references in the Global image; m0tral describes the AiotJS app engine as removed. Both can be compatible with residual interpreter/loader code without a functioning third-party app service. Neither establishes full RPK capability on our firmware. A later “AstroBox works globally” issue comment provides no exact model/build/log and is insufficient to overturn the detailed report.

Xiaomi's own FAQ documents Mi Fitness About → Debug → Third-Party Apps → Install third app for a developer app build. Public WatchDrip SDK code binds action `com.xiaomi.wearable.XMS_WEARABLE_SERVICE` to `com.mi.health`, falling back to `com.xiaomi.wearable`. Its `IWearableInterface` exposes installed-app checks, app launch and message send/listen. These are concrete phone-side interfaces; they cannot create a missing firmware dispatcher.

Full protobuf definitions show type 20/4 launch, 20/7 phone app status, 20/8 phone-to-wear message, 20/9 wear-to-phone message. Package identity includes a fingerprint. This is separate from the RPK manifest being parseable and from a successful installation. Region flags, APK certificates, RPK verification and device-side implementation must not be conflated.

No exact installed Mi Fitness APK was provided or decompiled in this investigation. No verified patch point for its region/signature gates is claimed. Modifying Mi Fitness is therefore not currently the selected solution.

Sources: [firmware-specific investigation and comments](https://github.com/atc1441/MiBand10-BES2700iMP-BEST1503-Hacking/issues/3), [official developer installation FAQ](https://iot.mi.com/vela/quickapp/en/guide/other/faq.html), [SDK Binder client](https://github.com/miguelavh/Watchdrip-Xiaomi/blob/main/SDK/src/main/java/com/xiaomi/xms/wearable/ApiClient.java), [third-party schema](https://github.com/AstralSightStudios/AstroBox-NG-Module-Pb/blob/03a92010056dd41af114f6f46fd612104b27bd7b/protos/xiaomi/wear_thirdparty_app.proto).

## D: What WatchDrip actually does

The public older bridge `miguelavh/Watchdrip-Xiaomi` polls `http://localhost:29863/info.json?graph=1` every 10 seconds, compares `bg.time`, launches `com.application.watch.watchdrip`, then sends the JSON through `XiaomiWatchHelper.sendMessageToWear` three times with delays. The helper uses the Mi Fitness Binder SDK, not a standalone raw-band connection. Current public `bigdigital/watchdrip` contains `XiaomiWearService` and calls it from updated glucose data, integrating the separate bridge pattern.

The Lua graph face opens `//data/quickapp/files/com.thatguysservice.huami_xdrip/info.json`, falling back to `//data/quickapp/files/com.application.watch.watchdrip/info.json`. That proves the face author's intended read path, backed by the user's prior physical/community evidence on other devices. It does not prove our target's filesystem mapping.

The inspected repositories did not supply the Xiaomi Quick App writer's complete source. `miguelavh/WatchdripService` is a Zepp project, not that missing Xiaomi RPK. The blog instructs installing the WatchDrip+ band app via modified Mi Fitness. Thus the Android message path and Lua read endpoint are source-traceable; the intervening Xiaomi RPK write implementation remains a gap. BandDrip already implements the equivalent writer with `system.file`.

Reusable: normalized reading contract, source triggers, SDK launch/message sequence and known file names. Not reusable without proof: RPK installation, background operation, or battery/reconnect behavior on Pro Global.

Sources: [Android bridge](https://github.com/miguelavh/Watchdrip-Xiaomi/blob/main/app/src/main/java/com/application/watch/watchdrip/MyService.java), [integrated WatchDrip service](https://github.com/bigdigital/watchdrip/blob/main/app/src/main/java/com/thatguysservice/huami_xdrip/services/XiaomiWearService.java), [Lua reader](https://github.com/miguelavh/LUA_Watchdrip_Watchface_graph_Xiaomi_band9/blob/main/app/lua/main.lua), [author setup instructions](https://bigdigital.home.blog/2024/11/28/xiaomi-89-support/).

## E: Additional clean mechanisms

1. **Native glucose synchronization:** schema type 20/subtype 20, payload `BloodGlucose` with uint64 timestamp, float value, uint32 status, optional alert. This is genuine glucose semantics. Units, timestamp units, status mapping, device support and Lua exposure are unresolved. It cannot carry all BandDrip fields as presently defined. Do not reinterpret status as IOB or delta. No test values are sent until those semantics are known.
2. **Native network channel:** AstroBox implements L2 channel 7 for network packets, system subtype 92 network capability synchronization, DHCP and an IP stack/proxy. A Lua shell `curl` could in principle fetch a phone-local synthetic endpoint through this channel. The band has no need for direct internet if the phone terminates the request locally. This is a concrete code lead, not proof on our band; Gadgetbridge does not currently implement that network bridge. Never assume the band's `127.0.0.1` is the phone's loopback. Polling/background shell lifetime and battery could be unfavorable.
3. **Dedicated native receiver:** `m0tral/MiWatchNativeApps` demonstrates Lua-packaged NuttX ELF modules, with profiles for CN 3.101.043 and Global 3.201.016. Canopus provides a related char-device architecture. These are not ready-to-use phone-to-band BandDrip transports. They rely on exact firmware ABI/symbol addresses and need a protocol registration or dedicated socket service. No 3.200.130 profile was found. P0 detects prerequisites only; it intentionally does not load those binaries.

Sources: [network bridge implementation](https://github.com/AstralSightStudios/AstroBox-NG-Module-Core/tree/95541fda829ef9fcb010e387d44782c9601fa9e0/src/device/xiaomi/components/network), [native app code and supported firmware](https://github.com/m0tral/MiWatchNativeApps), [Canopus](https://github.com/AstralSightStudios/Canopus).

## Ranked candidates, not validated architectures

There is not enough hardware evidence to claim 2–3 working architectures. These are ranked investigation candidates. Latency and battery descriptions are engineering expectations, **not measurements**.

| Priority / candidate | Reliability | Latency potential | Battery | Reverse engineering | Xiaomi dependency | Update resilience | Normal-user complexity |
|---|---|---|---|---|---|---|---|
| 1: Gadgetbridge → native face text editing | Unknown until exact face/firmware test | Small packets, potentially seconds | Likely low per update; flash writes unknown | Moderate: literal item/schema/runtime | Initial pairing only if GB sufficient | Better than native address hooks, still undocumented | One GB integration + compatible face |
| 2: Gadgetbridge → RPK message → file → Lua | Unknown on target; established pattern elsewhere | Potentially seconds; face timer adds delay | App wakeups/file writes require measurement | Moderate after install works; protocol known | Can avoid Mi Fitness in operation if implemented in GB | Runtime and sandbox changes remain risks | Install app and face, one companion |
| 3: Gadgetbridge → dedicated native receiver → owned file/Lua | Unknown; native-module precedent | Event-driven seconds plausible | Potentially low, implementation-dependent | High; exact firmware ABI and receive hook | No runtime cloud expected | Poor without firmware identity/ABI compatibility work | Hardest provisioning/recovery |
| Alternative: local network bridge → Lua fetch | Unknown on target | Depends on polling/wake behavior | Likely worse if frequent polling | Substantial phone stack, less firmware ABI work if stock curl works | Can be entirely local | Medium/unknown | More background components |
| Mi Fitness SDK / patched APK → RPK | Same device-side blocker as RPK route | Potentially seconds | Two phone services + band app | Patch maintenance in addition to transport | Xiaomi app, provisioning/cloud restrictions possible | Proprietary update coupling | Most fragile phone setup |

**Decision:** prioritize native watchface text editing through Gadgetbridge, with the RPK response diagnostic in the same hardware session. It is the smallest source-backed, semantically clean packet route found. Keep native modules as the fallback, not the starting point. This is a choice of the next experiment, not a promise that literal editing reaches our Lua face.

## Smallest actual transport experiment after P0

For the literal route: read exact installed-face metadata and establish literal capability/item IDs for a separate test face; preserve existing style/image fields; send 4/11 with one literal string `BANDDRIP_TEST_123`, `set_current=false`, no image deletion/upload; capture 4/11 result and inspect the face. Then send `BANDDRIP_TEST_147` without a face install. Require both visible updates, correct order and measured phone-send-to-visible latency. No response or unsupported literal result redirects to the next candidate. Do not send guessed IDs to the validated v2 face.

For the RPK route: require a valid app-list response and trace prepare-install status, MASS start and final install result. Only then use the existing writer probe. A static 123 written on app startup proves file visibility but not phone transport; follow with SDK/protobuf messages changing 123 to 147.

For native fallback: obtain the exact firmware image/hash, resolve only necessary ABI entries, first prove inert module load/unload, then register a bounded BandDrip-specific receiver writing one owned state file. No borrowed addresses from 3.201.016. Firmware flashing is not part of P0.

Before production: measure screen-off delivery, reconnect, source timestamp retention, stale aging independent of the phone, IOB freshness, update atomicity and battery. Do not connect real xDrip or build a larger app until 123 → 147 succeeds.
