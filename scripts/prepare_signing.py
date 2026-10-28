#!/usr/bin/env python3
"""Optional private CI signing. Never log key bytes/passwords or cache the keystore."""
import base64, os
from pathlib import Path

encoded = os.environ.get('JARVIS_KEYSTORE_B64', '')
required = ['JARVIS_KEYSTORE_PASSWORD', 'JARVIS_KEY_ALIAS', 'JARVIS_KEY_PASSWORD']
if encoded:
    if not all(os.environ.get(key) for key in required):
        raise SystemExit('Assinatura fixa incompleta: configure os quatro Secrets documentados.')
    destination = Path(os.environ['RUNNER_TEMP']) / 'jarvis-private-signing.jks'
    destination.write_bytes(base64.b64decode(encoded, validate=True))
    destination.chmod(0o600)
    with open(os.environ['GITHUB_ENV'], 'a') as output:
        output.write('JARVIS_KEYSTORE_PATH=' + str(destination) + '\n')
    print('Assinatura fixa configurada por Secrets. Material privado não será publicado.')
else:
    print('Assinatura debug temporária: atualizações estáveis e OAuth dependem da configuração da chave fixa.')
