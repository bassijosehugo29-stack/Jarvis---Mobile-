#!/usr/bin/env python3
"""Download the exact optional model fixture without tokens or an inference API."""
from pathlib import Path
import hashlib,json,sys,urllib.request

root=Path(__file__).resolve().parents[1]
spec=json.loads((root/'app/src/main/assets/conversation-model.json').read_text())
target=Path(sys.argv[1] if len(sys.argv)>1 else '/tmp/jarvis-conversation-model.gguf')
assert spec['url'].startswith('https://huggingface.co/Qwen/')
target.parent.mkdir(parents=True,exist_ok=True)
def verified(path):
    if not path.is_file() or path.stat().st_size!=spec['bytes']: return False
    with path.open('rb') as stream:
        return hashlib.file_digest(stream,'sha256').hexdigest()==spec['sha256']
if not verified(target):
    partial=target.with_suffix('.partial')
    request=urllib.request.Request(spec['url'],headers={'User-Agent':'JARVIS-Mobile-test/0.5'})
    try:
        with urllib.request.urlopen(request,timeout=90) as response,partial.open('wb') as out:
            total=0
            while chunk:=response.read(1024*1024):
                total+=len(chunk)
                if total>spec['bytes']: raise ValueError('Model exceeds pinned size')
                out.write(chunk)
        if not verified(partial): raise ValueError('Model SHA-256/size mismatch')
        partial.replace(target)
    finally: partial.unlink(missing_ok=True)
print(f'Model fixture verified: {target.name}, {spec["bytes"]} bytes, SHA-256 {spec["sha256"]}')
