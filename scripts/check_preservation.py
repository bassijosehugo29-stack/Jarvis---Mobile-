#!/usr/bin/env python3
"""Enforce scope against the available main, without claiming CyberBot equivalence."""
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
baseline = json.loads((root / 'docs/PRESERVATION-BASELINE.json').read_text())
assert baseline['reference_commit'] == '50abb6eb0661e519ec8dc704d0c9b4e12f154f7b'

def git_blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()

unchanged = []
changed = []
for name, expected in baseline['files'].items():
    data = (root / name).read_bytes()
    if git_blob(data) == expected:
        unchanged.append(name)
    else:
        assert name in baseline['allowed_base_changes'], f'Out-of-scope change: {name}'
        changed.append(name)

# These files may change only by replacing the visible name, never by redesigning them.
screen = 'app/src/main/java/br/com/jarvis/mobile/ui/JarvisScreen.kt'
screen_text = (root / screen).read_text()
assert screen_text.count('Text("JARVIS",') == 2
screen_restored = screen_text.replace('Text("JARVIS", color = Muted', 'Text("JARVIS Mobile · 3.1.0", color = Muted')
screen_restored = screen_restored.replace('Text("JARVIS", fontSize = if (large)', 'Text("J.A.R.V.I.S.", fontSize = if (large)')
assert git_blob(screen_restored.encode()) == baseline['files'][screen], 'Only the two visible names may change in the screen'
for name, previous in [
    ('app/src/main/java/br/com/jarvis/mobile/actions/AndroidActions.kt', 'JARVIS Mobile'),
    ('app/src/main/res/values/strings.xml', 'JARVIS Mobile'),
]:
    data = (root / name).read_text()
    assert data.count('"JARVIS"' if name.endswith('.kt') else '>JARVIS<') == 1
    restored = data.replace('"JARVIS"', '"' + previous + '"') if name.endswith('.kt') else data.replace('>JARVIS<', '>' + previous + '<')
    assert git_blob(restored.encode()) == baseline['files'][name], f'Only the name may change: {name}'

manifest_path = 'app/src/main/AndroidManifest.xml'
manifest = (root / manifest_path).read_text()
restored = manifest.replace('android:icon="@mipmap/ic_launcher"', 'android:icon="@drawable/ic_jarvis"')
restored = restored.replace('        android:roundIcon="@mipmap/ic_launcher"\n', '')
assert git_blob(restored.encode()) == baseline['files'][manifest_path], 'Only launcher icons may change in the manifest'

android = '{http://schemas.android.com/apk/res/android}'
for version in (26, 33):
    icon = ET.parse(root / f'app/src/main/res/mipmap-anydpi-v{version}/ic_launcher.xml').getroot()
    assert icon.tag == 'adaptive-icon'
    assert icon.find('foreground').get(android + 'drawable') == '@drawable/jarvis_launcher_foreground'
    assert icon.find('background').get(android + 'drawable') == '@color/jarvis_icon_background'
    if version == 33:
        assert icon.find('monochrome').get(android + 'drawable') == '@drawable/jarvis_launcher_monochrome'

protected = [p for p in unchanged if p.startswith((
    'voice-engine/', 'voice-a/', 'app/src/main/java/br/com/jarvis/mobile/audio/',
    'app/src/main/java/br/com/jarvis/mobile/domain/', 'app/src/main/java/br/com/jarvis/mobile/conversation/',
    'app/src/main/java/br/com/jarvis/mobile/brain/', 'app/src/main/java/br/com/jarvis/mobile/hub/',
    'app/src/main/java/br/com/jarvis/mobile/security/', 'app/src/main/java/br/com/jarvis/mobile/experience/',
    'app/src/main/java/br/com/jarvis/mobile/ui/', 'app/src/main/assets/', 'app/src/main/cpp/'))]
report = {'reference': baseline['reference'], 'reference_commit': baseline['reference_commit'],
          'cyberbot_original_verified': False, 'cyberbot_zip_available': False,
          'unchanged_base_files': len(unchanged), 'allowed_modified_files': changed,
          'protected_unchanged_files': protected, 'screen_change': 'visible name only',
          'voice_and_effects': 'unchanged relative to available main; CyberBot original unavailable',
          'permissions': 'unchanged', 'result': 'success'}
out = root / 'build/verification'
out.mkdir(parents=True, exist_ok=True)
(out / 'preservation.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
print(f'Scope OK: {len(unchanged)} base files identical; {len(protected)} protected code/assets identical. CyberBot equivalence NOT verified.')
