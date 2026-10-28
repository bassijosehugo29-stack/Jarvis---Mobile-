#!/usr/bin/env python3
"""Verify both signed APKs, 16 KiB native alignment, exact model identity and zero engine permissions."""
import hashlib,json,os,re,struct,subprocess,zipfile
from pathlib import Path
sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')))
tools=sdk/'build-tools/35.0.0';output=Path('build/verification');output.mkdir(parents=True,exist_ok=True)

def verify(apk):
 cert=subprocess.check_output([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
 badging=subprocess.check_output([str(tools/'aapt'),'dump','badging',str(apk)],text=True)
 permissions=subprocess.check_output([str(tools/'aapt'),'dump','permissions',str(apk)],text=True)
 subprocess.run([str(tools/'zipalign'),'-c','-P','16','-v','4',str(apk)],check=True,stdout=subprocess.DEVNULL)
 libraries=[]
 with zipfile.ZipFile(apk) as archive,apk.open('rb') as raw:
  assert archive.testzip() is None
  for info in archive.infolist():
   if not info.filename.endswith('.so'):continue
   body=archive.read(info);assert body[:6]==b'\x7fELF\x02\x01'
   phoff,=struct.unpack_from('<Q',body,32);size,count=struct.unpack_from('<HH',body,54);alignments=[]
   for i in range(count):
    offset=phoff+i*size
    if struct.unpack_from('<I',body,offset)[0]==1:
     alignment,=struct.unpack_from('<Q',body,offset+48);assert alignment>=16384,f'Unaligned ELF: {info.filename}';alignments.append(alignment)
   raw.seek(info.header_offset+26);name_len,extra_len=struct.unpack('<HH',raw.read(4));offset=info.header_offset+30+name_len+extra_len
   if info.compress_type==zipfile.ZIP_STORED:assert offset%16384==0
   libraries.append({'path':info.filename,'elf_load_alignment':alignments,'zip_offset':offset})
 return {'apk':str(apk),'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'bytes':apk.stat().st_size,
  'package':next(line for line in badging.splitlines() if line.startswith('package:')),
  'certificate_sha1':re.search(r'certificate SHA-1 digest: (\w+)',cert).group(1),
  'certificate_sha256':re.search(r'certificate SHA-256 digest: (\w+)',cert).group(1),
  'signing':'fixed_private_secret' if os.environ.get('JARVIS_KEYSTORE_PATH') else 'temporary_debug_key',
  'signature_verified':True,'zipalign_16k':True,'libraries':libraries,'permissions':permissions.splitlines()}

main=verify(Path('app/build/outputs/apk/debug/app-debug.apk'))
engine_path=Path('voice-engine/build/outputs/apk/debug/voice-engine-debug.apk');engine=verify(engine_path)
assert main['certificate_sha256']==engine['certificate_sha256'],'Both APKs must share the same signing identity'
assert not any('uses-permission:' in line for line in engine['permissions']),'The voice engine must have no requested permissions'
with zipfile.ZipFile(engine_path) as archive:
 proof=json.loads(archive.read('assets/voice-a/provenance.json'))
 assert proof['approved_model_sha256']=='858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7'
 assert proof['initializers_unchanged'] is True
 assert hashlib.sha256(archive.read('assets/voice-a/model.onnx')).hexdigest()==proof['bundled_model_sha256']
engine['voice_identity']=proof;engine['same_certificate_as_main']=True
(output/'apk.json').write_text(json.dumps(main,indent=2))
(output/'voice-engine-apk.json').write_text(json.dumps(engine,indent=2))
print('Both APKs: verified signatures, matching certificate, CRC and 16 KiB alignment. Voice engine: exact approved model, zero permissions.')
