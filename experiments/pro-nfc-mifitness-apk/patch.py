from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import shutil

root = Path(sys.argv[1])
android = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', android[1:-1])
manifest = root / 'AndroidManifest.xml'
tree = ET.parse(manifest)
assert tree.getroot().get('package') == 'com.xiaomi.wearable'
app = tree.getroot().find('application')
assert app is not None
assert any(x.get(android+'name') == 'com.xiaomi.fitness.baseui.common.CommonBaseActivity' for x in app.findall('activity'))
assert not any(x.get(android+'name') == 'org.banddrip.mifitness.InstallerActivity' for x in app.findall('activity'))
activity = ET.SubElement(app, 'activity', {
    android+'name': 'org.banddrip.mifitness.InstallerActivity',
    android+'exported': 'true',
    android+'label': 'BandDrip Installer',
    android+'theme': '@android:style/Theme.Material.Light.NoActionBar',
})
intent = ET.SubElement(activity, 'intent-filter')
ET.SubElement(intent, 'action', {android+'name': 'android.intent.action.MAIN'})
ET.SubElement(intent, 'category', {android+'name': 'android.intent.category.LAUNCHER'})
tree.write(manifest, encoding='utf-8', xml_declaration=True)
target = root / 'smali_classes13/org/banddrip/mifitness'
target.mkdir(parents=True)
assert not (root / 'classes13.dex').exists()
shutil.copyfile(Path(__file__).with_name('InstallerActivity.smali'), target/'InstallerActivity.smali')
print('Added one launcher activity. Original Mi Fitness dex files remain untouched.')
