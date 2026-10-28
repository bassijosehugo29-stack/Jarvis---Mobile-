#!/usr/bin/env python3
"""Provide the separate GPL voice engine's source/build recipes and pinned GPL dependency sources."""
from pathlib import Path
import hashlib,json,urllib.request,zipfile

ROOT=Path(__file__).resolve().parents[1];CACHE=ROOT/'.voice-a-cache';CACHE.mkdir(exist_ok=True)
SOURCES=[('sherpa-onnx-source.zip','https://codeload.github.com/k2-fsa/sherpa-onnx/zip/11afbd009a7f8c08f4bcf2fc1b265d0df4670fbf',None),
 ('espeak-ng-source.zip','https://github.com/csukuangfj/espeak-ng/archive/ed530aa113046142eb5115cf2fc9157854d0ffe1.zip','e4e262cbe34f7fe21f91f1ba3397f2728e1f30eafbae7853f2b753a9ed13f0dd')]
manifest=[]
for name,url,expected in SOURCES:
 target=CACHE/name
 if not target.exists():
  with urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'JARVIS-Voice-A-Source/1.0'}),timeout=120) as source:target.write_bytes(source.read())
 digest=hashlib.sha256(target.read_bytes()).hexdigest()
 if expected:assert digest==expected
 manifest.append({'file':name,'url':url,'sha256':digest})
out=ROOT/'build/voice-source';out.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(out/'JARVIS-Voz-A-fontes.zip','w',zipfile.ZIP_DEFLATED) as archive:
 for file in sorted((ROOT/'voice-engine').rglob('*')):
  if file.is_file() and not any(part in {'build','generated'} for part in file.relative_to(ROOT).parts):archive.write(file,str(file.relative_to(ROOT)))
 for name in ['build.gradle.kts','gradle.properties','gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.properties','scripts/prepare_voice_a.py','scripts/bootstrap-wrapper.sh','scripts/bootstrap-wrapper.ps1']:
  archive.write(ROOT/name,name)
 archive.writestr('settings.gradle.kts','pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositories { google(); mavenCentral() } }\nrootProject.name="JARVIS Voz A"\ninclude(":voice-engine")\n')
 for name,_,_ in SOURCES:archive.write(CACHE/name,'third-party/'+name)
 archive.writestr('third-party/sources.json',json.dumps(manifest,indent=2))
 archive.writestr('BUILD.txt','JDK 17, Android SDK 35, Gradle 8.11.1. python -m pip install onnx==1.18.0 ; python scripts/prepare_voice_a.py ; ./gradlew :voice-engine:assembleDebug .\nThe script verifies pinned native binaries/models. To rebuild native binaries from corresponding source, unpack third-party/sherpa-onnx-source.zip and espeak-ng-source.zip; use the Android/AAR build workflows and CMake files in that exact Sherpa source. The espeak-ng archive hash matches cmake/espeak-ng-for-piper.cmake. Standard compiler/Android SDK and permissively licensed dependencies are obtained per the pinned upstream recipes.\n')
print('Independent voice-engine corresponding source bundle created.')
