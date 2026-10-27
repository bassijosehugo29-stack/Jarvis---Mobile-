#!/usr/bin/env python3
"""Offline structural checks only; not a Kotlin compiler or replacement for Android Lint."""
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
errors = []

def check(condition, description):
    print(('OK   ' if condition else 'FAIL ') + description)
    if not condition:
        errors.append(description)

required = [
    'settings.gradle.kts', 'build.gradle.kts', 'gradle.properties', 'gradlew',
    'gradlew.bat', 'gradle/wrapper/gradle-wrapper.properties',
    'scripts/bootstrap-wrapper.sh', 'scripts/bootstrap-wrapper.ps1',
    'app/build.gradle.kts', 'app/proguard-rules.pro',
    'app/src/main/AndroidManifest.xml',
    'app/src/main/java/br/com/jarvis/mobile/MainActivity.kt',
    'app/src/main/java/br/com/jarvis/mobile/JarvisViewModel.kt',
    '.github/workflows/android-debug.yml', 'README.md',
    'docs/APK-PELO-CELULAR.md', 'docs/ARQUITETURA.md', 'docs/TESTES.md', 'docs/VALIDATION.md',
]
check(all((ROOT / item).is_file() for item in required), 'Arquivos essenciais presentes')
xmls = list((ROOT / 'app/src/main').rglob('*.xml'))
for path in xmls:
    try:
        ET.parse(path)
        check(True, f'XML válido: {path.relative_to(ROOT)}')
    except ET.ParseError as exc:
        check(False, f'XML inválido: {path}: {exc}')

android = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
permissions = {el.get(android + 'name') for el in manifest.findall('uses-permission')}
check(permissions == {'android.permission.RECORD_AUDIO', 'com.android.alarm.permission.SET_ALARM'},
      'Permissões limitadas a microfone e intent de alarme')
check(not manifest.findall('.//service') and not manifest.findall('.//receiver'),
      'Sem serviço de escuta ou receiver de background nesta versão')
for activity in manifest.findall('.//activity'):
    name = activity.get(android + 'name')
    full = 'br.com.jarvis.mobile' + name if name.startswith('.') else name
    source = ROOT / 'app/src/main/java' / (full.replace('.', '/') + '.kt')
    check(source.is_file(), f'Activity declarada possui fonte: {full}')

for path in (ROOT / 'app/src').rglob('*.kt'):
    source = path.read_text()
    package = re.search(r'^package ([\w.]+)', source, re.M)
    check(package is not None and path.parent.as_posix().endswith(package[1].replace('.', '/')),
          f'Pacote Kotlin: {path.relative_to(ROOT)}')
    check('TODO(' not in source and 'NotImplementedError(' not in source,
          f'Sem implementação pendente: {path.name}')

build = (ROOT / 'app/build.gradle.kts').read_text()
check('compileSdk = 35' in build and 'targetSdk = 35' in build and 'minSdk = 26' in build,
      'SDKs coerentes com documentação')
workflow = (ROOT / '.github/workflows/android-debug.yml').read_text()
check('testDebugUnitTest lintDebug assembleDebug' in workflow and
      'app/build/outputs/apk/debug/app-debug.apk' in workflow and 'actions/upload-artifact@v4' in workflow,
      'Workflow inclui testes, lint, montagem e artefato APK')
check('contents: read' in workflow and 'workflow_dispatch:' in workflow,
      'Workflow manual e sem permissão de escrita no repositório')
check('publish' not in workflow and 'release' not in workflow,
      'Workflow sem etapa de publicação')
for script in ['gradlew', 'scripts/bootstrap-wrapper.sh']:
    result = subprocess.run(['sh', '-n', str(ROOT / script)], capture_output=True)
    check(result.returncode == 0, f'Sintaxe shell: {script}')

for file in [ROOT / 'README.md', *list((ROOT / 'docs').glob('*.md'))]:
    for target in re.findall(r'\]\(([^)]+)\)', file.read_text()):
        if not target.startswith(('https://', 'http://', '#')):
            check((file.parent / target.split('#')[0]).exists(),
                  f'Link local existe: {file.name} -> {target}')

print(f'\n{len(errors)} erro(s) estrutural(is). Compilação, lint e testes Android NÃO foram executados por este script.')
sys.exit(1 if errors else 0)
