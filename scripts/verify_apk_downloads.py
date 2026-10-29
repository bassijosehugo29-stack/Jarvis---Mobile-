#!/usr/bin/env python3
"""Verify actual published preview bytes; never claim CyberBot equivalence."""
from pathlib import Path
import hashlib
import json
import os
import re
import zipfile

root = Path('downloaded')
out = Path('build/download-verification')
out.mkdir(parents=True, exist_ok=True)
apks = list((root / 'mobile').rglob('*.apk'))
assert len(apks) == 1 and apks[0].name == 'JARVIS-teste-base-disponivel.apk'
proofs = list((root / 'reports').rglob('apk.json'))
assert len(proofs) == 1
proof = json.loads(proofs[0].read_text())
apk = apks[0]
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
assert digest == proof['sha256'] and apk.stat().st_size == proof['bytes']
assert proof['signature_verified'] is True and proof['zipalign_16k'] is True
assert proof['application_label'] == 'JARVIS'
assert proof['isolated_test_package'] == 'br.com.jarvis.mobile.preservacaopreview'
assert proof['cyberbot_original_verified'] is False
assert (root / 'mobile/LEIA-ANTES-DE-INSTALAR.txt').is_file()
with zipfile.ZipFile(apk) as archive:
    assert archive.testzip() is None
    assert {'AndroidManifest.xml', 'classes.dex'} <= set(archive.namelist())
url = os.environ['JARVIS_MOBILE_ARTIFACT_URL']
assert re.fullmatch(r'https://github\.com/[^/]+/[^/]+/actions/runs/\d+/artifacts/\d+', url)
report = {'download_verification': 'success', 'source': os.environ.get('GITHUB_SHA'),
          'run': os.environ.get('GITHUB_RUN_ID'), 'retention_requested_days': 90, 'login_required': True,
          'cyberbot_original_verified': False, 'file': apk.name, 'bytes': apk.stat().st_size,
          'sha256': digest, 'certificate_sha256': proof['certificate_sha256'], 'url': url,
          'signature_verified_before_upload': True, 'download_identical_to_signed_build': True,
          'package': proof['isolated_test_package'], 'label': 'JARVIS'}
(out / 'downloads.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
(out / 'SHA256SUMS').write_text(f'{digest}  {apk.name}\n')
guide = f'''# APK técnico da base disponível

**NÃO é o CyberBot comprado com fidelidade verificada.** Falta o ZIP original com a interface e seus recursos.

[Baixar teste técnico]({url}) — {apk.stat().st_size} bytes, dentro de um ZIP. Exige login no GitHub.

O CI baixou o artifact e confirmou que contém o mesmo APK assinado. Nome: JARVIS. Pacote isolado: `{proof['isolated_test_package']}`.

Não desinstale o CyberBot nem outras versões. A prévia tem armazenamento separado e não importa automaticamente seus dados. A voz da base disponível depende do motor já existente com assinatura compatível; nenhum novo motor foi distribuído.

Este arquivo não atende ao critério de equivalência visual ao CyberBot e não é o aplicativo final para a viagem.
'''
(out / 'DOWNLOADS.md').write_text(guide)
if os.environ.get('GITHUB_STEP_SUMMARY'):
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as summary:
        summary.write(guide)
print(json.dumps(report, ensure_ascii=False, indent=2))
