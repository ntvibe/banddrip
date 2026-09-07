"""Apply bounded diagnostics to pinned Gadgetbridge 0.93.0, not a transport driver."""
from pathlib import Path
import subprocess
import sys

root = Path(sys.argv[1])
pin = 'a09e037374d0013ffc31978fc1d1ada2943280e4'
assert subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip() == pin
p = root / 'app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/devices/xiaomi/XiaomiSupport.java'
s = p.read_text()
anchor = '    @Override\n    public void onTestNewFunction(@Nullable Bundle options) {\n        //sendCommand("test new function", 2, 29);\n    }'
assert anchor in s
s = s.replace(anchor, '''    private volatile boolean banddripProbeRunning;
    private volatile boolean banddripControl;
    private volatile boolean banddripFaces;
    private volatile boolean banddripApps;
    private volatile boolean banddripSlots;

    @Override
    public void onTestNewFunction(@Nullable Bundle options) {
        if (banddripProbeRunning) return;
        if (!getDevice().getName().startsWith("Xiaomi Smart Band 10 Pro")) return;
        banddripProbeRunning = true;
        banddripControl = banddripFaces = banddripApps = banddripSlots = false;
        LOG.info("BANDDRIP_PROBE START firmware={} transport={}",
                getDevice().getFirmwareVersion(), getCoordinator().getConnectionType());
        sendCommand("BandDrip control query", 2, 1);
        sendCommand("BandDrip watchface list", 4, 0);
        sendCommand("BandDrip supported face data", 4, 10);
        sendCommand("BandDrip RPK list", 20, 0);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            banddripProbeRunning = false;
            String result = "BandDrip: control=" + banddripControl + " faces=" + banddripFaces
                    + " apps=" + banddripApps + " slots=" + banddripSlots;
            LOG.info("BANDDRIP_PROBE END {} (response presence, not service completeness)", result);
            android.widget.Toast.makeText(getContext(), result, android.widget.Toast.LENGTH_LONG).show();
        }, 10000);
    }

    private void banddripObserve(final XiaomiProto.Command cmd) {
        if (!banddripProbeRunning) return;
        int t = cmd.getType(), st = cmd.getSubtype();
        if (t == 2 && st == 1) banddripControl = true;
        if (t == 4 && st == 0) banddripFaces = true;
        if (t == 4 && st == 10) banddripSlots = true;
        if (t == 20 && st == 0) banddripApps = true;
        if (t == 4 || t == 20) {
            // Keep raw face metadata in existing DEBUG logs, not the shareable summary.
            LOG.info("BANDDRIP_PROBE RX type={} subtype={} bytes={}", t, st, cmd.getSerializedSize());
            if (t == 20 && st == 0) LOG.info("BANDDRIP_PROBE RPK payloadPresent={} count={}",
                    cmd.hasRpk() && cmd.getRpk().hasRpkList(), cmd.getRpk().getRpkList().getRpkInfoCount());
        }
    }''')
needle = '        final AbstractXiaomiService service = mServiceMap.get(cmd.getType());'
assert needle in s
s = s.replace(needle, '        banddripObserve(cmd);\n' + needle)
p.write_text(s)

# Separate app identity: installation must not replace the user's working build.
p = root / 'app/build.gradle'
s = p.read_text()
needle = 'applicationId "nodomain.freeyourgadget.gadgetbridge"'
assert s.count(needle) == 1
p.write_text(s.replace(needle, 'applicationId "org.banddrip.gadgetbridge.probe"'))
p = root / 'app/src/mainline/res/values/strings.xml'
s = p.read_text()
assert 'com.getpebble.android.provider' in s
s = s.replace('com.getpebble.android.provider', 'org.banddrip.gadgetbridge.probe.pebble.provider')
p.write_text(s.replace('@string/application_name_generic', 'BandDrip GB Probe'))
print('Applied bounded read-only BandDrip diagnostics')
