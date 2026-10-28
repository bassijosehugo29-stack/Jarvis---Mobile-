#!/usr/bin/env python3
"""Verify APK bytes downloaded from the published Actions artifacts, then print stable URLs."""
from pathlib import Path
import hashlib
import json
import os
import re
import zipfile

root = Path('downloaded')
out = Path('build/download-verification')
out.mkdir(parents=True, exist_ok=True)
specs = [
    ('voice', 'JARVIS Voz A', 'JARVIS-Voz-A-4-preview.apk', 'voice-engine-apk.json', 'JARVIS_VOICE_ARTIFACT_URL'),
    ('mobile', 'JARVIS Mobile 4.0 · Prévia', 'JARVIS-Mobile-4.0.0-fase1-preview.apk', 'apk.json', 'JARVIS_MOBILE_ARTIFACT_URL'),
]
verified = []
for folder, title, name, proof_name, url_var in specs:
    apks = list((root / folder).rglob('*.apk'))
    assert len(apks) == 1 and apks[0].name == name, f'Unexpected APK contents: {folder}'
    proofs = list((root / 'reports').rglob(proof_name))
    assert len(proofs) == 1, f'Missing unique build proof: {proof_name}'
    proof = json.loads(proofs[0].read_text())
    apk = apks[0]
    digest = hashlib.sha256(apk.read_bytes()).hexdigest()
    assert digest == proof['sha256'] and apk.stat().st_size == proof['bytes'], 'Downloaded APK differs from verified build'
    assert proof['signature_verified'] is True and proof['zipalign_16k'] is True
    with zipfile.ZipFile(apk) as archive:
        assert archive.testzip() is None, 'Downloaded APK CRC failed'
        assert {'AndroidManifest.xml', 'classes.dex'} <= set(archive.namelist()), 'Invalid Android APK'
        if folder == 'voice':
            assert proof['same_certificate_as_main'] is True
            assert not any('uses-permission:' in p for p in proof['permissions'])
            model_hash = hashlib.sha256(archive.read('assets/voice-a/model.onnx')).hexdigest()
            assert model_hash == proof['voice_identity']['bundled_model_sha256']
    url = os.environ[url_var]
    assert re.fullmatch(r'https://github\.com/[^/]+/[^/]+/actions/runs/\d+/artifacts/\d+', url), 'Invalid artifact URL'
    verified.append({'title': title, 'file': name, 'bytes': apk.stat().st_size, 'sha256': digest,
                     'certificate_sha256': proof['certificate_sha256'], 'certificate_sha1': proof['certificate_sha1'],
                     'url': url, 'signature_verified_before_upload': True, 'download_identical_to_signed_build': True})
assert verified[0]['certificate_sha256'] == verified[1]['certificate_sha256'], 'APKs must share the signing certificate'
report = {'download_verification': 'success', 'source': os.environ.get('GITHUB_SHA'),
          'run': os.environ.get('GITHUB_RUN_ID'), 'retention_requested_days': 90, 'login_required': True,
          'container': 'ZIP, one APK per artifact', 'files': verified}
(out / 'downloads.json').write_text(json.dumps(report, ensure_ascii=False, indent=2))
(out / 'SHA256SUMS').write_text(''.join(f"{f['sha256']}  {f['file']}\n" for f in verified))
lines = ['# Baixar e instalar JARVIS no Samsung', '',
         'Os dois artifacts foram baixados novamente pelo CI. Seus APKs são idênticos aos arquivos assinados/verificados na compilação.', '',
         '**Abra os links no Chrome e entre na sua conta GitHub.** O download é um ZIP por aplicativo; não é um arquivo APK direto.', '',
         '| Instalar nesta ordem | Arquivo dentro do ZIP | Tamanho do APK |', '| --- | --- | ---: |']
for index, item in enumerate(verified, 1):
    lines.append(f"| {index}. [Baixar {item['title']}]({item['url']}) | {item['file']} | {item['bytes']} bytes |")
lines += ['', 'Em Meus Arquivos → Downloads, extraia os dois ZIPs. Instale a voz A primeiro e depois o assistente. Autorize a origem de instalação somente se o Android pedir.', '',
          'Prévia instalada ao lado da 3.1: não desinstale a versão atual nem o motor atual. Notas/configurações antigas ficam intactas na 3.1; a prévia usa armazenamento separado. Importar backup é opcional e manual.', '',
          'Os artifacts permanecem após o término da execução, com retenção solicitada de 90 dias, sujeita ao limite do repositório. A API informa a expiração efetiva. Após expirar, use Actions → Android - APK de teste → Run workflow → jarvis-4-fase1.', '',
          'JARVIS 4.0 fase 1, na branch jarvis-4-fase1. O motor de voz A mantém o modelo e o pitch natural aprovados. O aplicativo tem novo núcleo e integração ACTION_ASSIST; consulte o relatório da fase.', '',
          f"[Fontes correspondentes do motor GPL]({os.environ.get('JARVIS_VOICE_SOURCES_URL', '')})", '']
guide = '\n'.join(lines)
(out / 'DOWNLOADS.md').write_text(guide)
if os.environ.get('GITHUB_STEP_SUMMARY'):
    with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as summary:
        summary.write(guide)
print(json.dumps(report, ensure_ascii=False, indent=2))
