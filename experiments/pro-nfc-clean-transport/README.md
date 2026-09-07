# Pro NFC clean transport investigation

Target: user's Xiaomi Smart Band 10 Pro NFC Global, firmware **3.200.130**.
Investigation date: 2026-09-07. No clean live transport has yet been demonstrated on this device.

## First physical test

Start with **BandDripTransportP0.face**, using your existing Gadgetbridge installation.
It has its own face ID, **1909073001**, and does not overwrite BandDripProV2 (1908261002).

1. Download the `BandDripTransportP0-face` artifact from this PR's successful Actions run and extract the `.face`.
2. Open it with the currently paired Gadgetbridge and install it as another watchface.
3. Keep the screen awake for the first 8 seconds. Photograph the two result pages; they cycle every 8 seconds while active.
4. Select your original v2 face afterward.

The face reads `ro.build.version`, captures `help`, and lists `/dev/uorb` names. It writes only its own temporary files under `/data/banddrip-p0-*`, then removes them where supported. It never opens uORB device nodes, loads modules, changes firmware, or sends network requests. `NOT SEEN` means no match in the captured, size-limited output, not proof that a capability is absent. Shell execution errors are caught at the Lua level, but the device remains the final authority on runtime behavior.

This is a **prerequisite probe**, not the requested phone-sent `123 -> 147` test. Local shell output is not phone-to-band transport evidence.

## Phone protocol diagnostic

The separate `BandDrip-Gadgetbridge-P0` artifact contains a debug APK, the exact upstream source revision, patch, and corresponding source archive. Package: `org.banddrip.gadgetbridge.probe`. It cannot upgrade or replace stock Gadgetbridge.

Use this second test only after the face test, or if you already have the original failed-install debug log. That original log may spare you migrating a connection.

1. Export a local backup from existing Gadgetbridge. Keep it private: it may contain authentication material.
2. Disconnect the band in existing Gadgetbridge and force-stop that app. Do not remove the pairing or reset the band.
3. Install the diagnostic APK. Import the local backup if accepted by this build, or add the existing band using your existing authentication key. Do not share that key.
4. Connect. From the band's device-card menu choose **Test new function**.
5. Wait 10 seconds and record the result toast. If it disappears too fast, repeat while screen-recording.
6. For detailed analysis, extract only `BANDDRIP_PROBE` log lines. Full Gadgetbridge logs contain unrelated personal/device information.
7. Disconnect and force-stop the probe, then reopen your original Gadgetbridge.

The probe sends only:

| Query | Type/subtype | Purpose |
|---|---|---|
| Battery | 2/1 | Application-layer connection control |
| Watchface list | 4/0 | Known working service control |
| Supported watchface data | 4/10 | Native editing/capability lead |
| Quick App list | 20/0 | Distinguish a response from silence |

The toast booleans represent **response presence**, not a declaration that a complete service exists. A type-20 response with a missing list payload is not a valid empty app list; the log reports payload presence separately. Replies can be unsolicited or delayed, so ambiguous results require repetition with an otherwise idle connection. No install is attempted by this button.

| Observation | Interpretation / next action |
|---|---|
| Control and faces true, apps false | Type 20/0 silence during this window; investigate original 20/1 install log. Does not prove all type-20 subcommands absent. |
| Apps true, valid list payload, count 0 | Service answers; there are no installed apps. Trace install prepare status next. |
| Control false, faces false | Connection/session test failed; do not infer firmware capability. |
| Slots true | 4/10 answers. Does not prove arbitrary text editing or Lua access. |
| Lua shell output readable, insmod seen | Native receiver prerequisite exists; still need exact firmware binary/symbols before loading a module. |
| curl seen | Investigate stock network-channel response to a local synthetic endpoint. No internet assumption. |

## Engineering findings and route ranking

See [INVESTIGATION.md](INVESTIGATION.md) for source-level evidence, limitations, alternatives, and the next binary success criteria.
