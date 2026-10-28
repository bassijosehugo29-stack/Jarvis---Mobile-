#!/usr/bin/env python3
"""Build-time only: pin and verify the approved Faber weights and local engine binaries.
Nothing here runs on a user's device. The companion APK has no internet permission.
"""
from pathlib import Path
import hashlib,json,shutil,struct,tarfile,urllib.request,zipfile

ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'.voice-a-cache';GEN=ROOT/'voice-engine/src/main/generated'
AAR_URL='https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar'
AAR_SHA='633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96'
PACK_URL='https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-pt_BR-faber-medium.tar.bz2'
PACK_SHA='7add3f923ad6bc25ca8a192805fd1a64d1b3893e4611c4a9719545a825039a83'
MODEL_URL='https://huggingface.co/rhasspy/piper-voices/resolve/5041a7a49ae5b3a0f30e6837769eb03fdbb231bd/pt/pt_BR/faber/medium/pt_BR-faber-medium.onnx'
MODEL_SHA='858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7'

def fetch(url,path,sha):
 if path.exists() and hashlib.sha256(path.read_bytes()).hexdigest()==sha:return path
 request=urllib.request.Request(url,headers={'User-Agent':'JARVIS-Voice-A-Build/1.0'})
 temporary=path.with_suffix(path.suffix+'.part')
 with urllib.request.urlopen(request,timeout=120) as src,temporary.open('wb') as out:shutil.copyfileobj(src,out)
 assert hashlib.sha256(temporary.read_bytes()).hexdigest()==sha,'Dependency hash mismatch'
 temporary.replace(path);return path

def aligned(body,name):
 assert body[:6]==b'\x7fELF\x02\x01',f'Expected ELF64: {name}'
 phoff,=struct.unpack_from('<Q',body,32);size,count=struct.unpack_from('<HH',body,54)
 for i in range(count):
  off=phoff+i*size
  if struct.unpack_from('<I',body,off)[0]==1:assert struct.unpack_from('<Q',body,off+48)[0]>=16384,f'16KiB alignment required: {name}'

def fingerprint(model):
 # A converted container can add metadata. Every original initializer must remain byte-identical.
 return {t.name:hashlib.sha256(t.SerializeToString(deterministic=True)).hexdigest() for t in model.graph.initializer}

def main():
 import onnx
 CACHE.mkdir(exist_ok=True)
 aar=fetch(AAR_URL,CACHE/'sherpa.aar',AAR_SHA)
 bundle=fetch(PACK_URL,CACHE/'faber.tar.bz2',PACK_SHA)
 approved=fetch(MODEL_URL,CACHE/'approved-faber.onnx',MODEL_SHA)
 native=GEN/'jniLibs';native.mkdir(parents=True,exist_ok=True)
 with zipfile.ZipFile(aar) as z:
  for name in z.namelist():
   parts=Path(name).parts
   if len(parts)==3 and parts[0]=='jni' and parts[1] in {'arm64-v8a','x86_64'} and name.endswith('.so'):
    body=z.read(name);aligned(body,name);target=native/parts[1]/parts[2];target.parent.mkdir(exist_ok=True);target.write_bytes(body)
 unpack=CACHE/'model-pack';unpack.mkdir(exist_ok=True)
 with tarfile.open(bundle,'r:bz2') as tar:
  total=0
  for member in tar.getmembers():
   target=(unpack/member.name).resolve()
   assert unpack.resolve() in target.parents and not member.issym() and not member.islnk(),'Unsafe voice bundle'
   total+=member.size;assert total<=200*1024*1024
  tar.extractall(unpack,filter='data')
 models=list(unpack.rglob('*.onnx'));assert len(models)==1
 packaged=onnx.load(str(models[0]));original=onnx.load(str(approved))
 # Retain the exact approved graph/weights; only add the metadata the Android loader needs.
 original_hash=fingerprint(original)
 packaged_hash=fingerprint(packaged)
 assert original_hash==packaged_hash,'The Android bundle is not the approved Faber model'
 del original.metadata_props[:]
 for prop in packaged.metadata_props:
  item=original.metadata_props.add();item.key=prop.key;item.value=prop.value
 voice=GEN/'assets/voice-a';voice.mkdir(parents=True,exist_ok=True)
 onnx.save(original,str(voice/'model.onnx'))
 tokens=models[0].parent/'tokens.txt';assert tokens.is_file();shutil.copyfile(tokens,voice/'tokens.txt')
 data=next(unpack.rglob('espeak-ng-data'),None);assert data and data.is_dir(),'Missing offline phonemizer data'
 shutil.copytree(data,voice/'espeak-ng-data',dirs_exist_ok=True)
 result={'profile':'executive-a-v1','model':'pt_BR-faber-medium','language':'pt-BR','approved_model_sha256':MODEL_SHA,
  'bundled_model_sha256':hashlib.sha256((voice/'model.onnx').read_bytes()).hexdigest(),'initializers_unchanged':True,'initializers':len(original_hash),
  'sherpa_version':'1.13.8','aar_sha256':AAR_SHA,'bundle_sha256':PACK_SHA,'length_scale':1.08,'noise_scale':0.55,'noise_w':0.70,
  'sentence_silence_seconds':0.24,'pitch_shift':False,'network_permission':False,'model_source':MODEL_URL,'runtime_source':'https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8'}
 (voice/'provenance.json').write_text(json.dumps(result,indent=2))
 # License/corresponding-source references travel with the independent engine.
 licenses=GEN/'assets/licenses';licenses.mkdir(parents=True,exist_ok=True)
 for name in ['LICENSE','SHERPA-LICENSE','THIRD-PARTY.md']:
  shutil.copyfile(ROOT/'voice-engine'/name,licenses/name)
 report=ROOT/'build/verification';report.mkdir(parents=True,exist_ok=True)
 (report/'voice-a.json').write_text(json.dumps(result,indent=2))
 print('Approved Faber weights verified; offline voice assets prepared; native libraries aligned to 16 KiB.')

if __name__=='__main__':main()
