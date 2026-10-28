#!/usr/bin/env python3
"""CI/emulator only. Real am-start/proc/meminfo/gfxinfo. Never delete apps on a user's phone."""
from pathlib import Path
import hashlib,json,re,subprocess,time,os
out=Path('app/build/reports/performance');out.mkdir(parents=True,exist_ok=True)
shots=Path('app/build/reports/screenshots');shots.mkdir(parents=True,exist_ok=True)
package='br.com.jarvis.mobile'
def adb(*args,check=True):
 r=subprocess.run(['adb',*args],capture_output=True,text=True,timeout=45)
 if check and r.returncode:raise RuntimeError('adb failed: '+r.stderr[:200])
 return r.stdout
if os.environ.get('GITHUB_ACTIONS') != 'true':
 print('Medição comparativa ignorada: exclusiva do CI, sem remover qualquer aplicativo.')
 raise SystemExit(0)
if adb('shell','getprop','ro.kernel.qemu').strip() != '1':
 raise SystemExit('Refusing destructive fixture setup: target is not an Android emulator.')
def capture(label,seconds=5):
 pid=adb('shell','pidof',package).strip().split()[0]
 def ticks():
  s=adb('shell','run-as',package,'cat','/proc/'+pid+'/stat',check=False)
  try:
   fields=s[s.rfind(')')+2:].split();return int(fields[11])+int(fields[12])
  except (ValueError,IndexError):return None
 hz=adb('shell','getconf','CLK_TCK',check=False).strip();hz=int(hz) if hz.isdigit() else None
 adb('shell','dumpsys','gfxinfo',package,'reset')
 before=ticks();start=time.monotonic();time.sleep(seconds);elapsed=time.monotonic()-start;after=ticks()
 mem=adb('shell','dumpsys','meminfo',package);gfx=adb('shell','dumpsys','gfxinfo',package,'framestats')
 (out/(label+'-meminfo.txt')).write_text(mem);(out/(label+'-gfxinfo.txt')).write_text(gfx)
 pss=re.search(r'TOTAL PSS:\s*(\d+)',mem)
 if not pss:pss=re.search(r'^\s*TOTAL\s+(\d+)',mem,re.M)
 frames=[];header=None
 for line in gfx.splitlines():
  line=line.strip()
  if line.startswith('Flags,') and 'IntendedVsync' in line and 'FrameCompleted' in line:header=line.split(',');continue
  if header and re.match(r'^0,\d+',line):
   values=line.split(',')
   try:
    duration=(int(values[header.index('FrameCompleted')])-int(values[header.index('IntendedVsync')]))/1e6
    if 0<duration<1000:frames.append(duration)
   except (ValueError,IndexError):pass
 frames.sort()
 return {'sample_seconds':elapsed,'pss_kb':int(pss.group(1)) if pss else None,
  'cpu_one_core_percent':((after-before)/hz/elapsed*100) if before is not None and after is not None and hz else None,
  'clock_ticks_hz':hz,'rendered_frames':len(frames),'frame_p50_ms':frames[int((len(frames)-1)*.5)] if frames else None,
  'frame_p95_ms':frames[int((len(frames)-1)*.95)] if frames else None,
  'frame_backend':'real Android gfxinfo framestats; unfinished/invalid timestamps excluded'}
def sample(label,app,voice):
 # Ephemeral CI emulator only. Clear synthetic fixtures for comparable fresh installs.
 adb('uninstall',package,check=False);adb('uninstall','br.com.jarvis.voicea',check=False)
 adb('install','-r',str(voice));adb('install','-r',str(app))
 starts=[]
 for _ in range(3):
  adb('shell','am','force-stop',package)
  result=adb('shell','am','start','-W','-n',package+'/.MainActivity')
  starts.append({k:int(v) for k,v in re.findall(r'(ThisTime|TotalTime|WaitTime):\s*(\d+)',result)})
  time.sleep(.8)
 time.sleep(2)
 value=capture(label)
 value['cold_start_ms']=starts
 (out/(label+'-batterystats.txt')).write_text(adb('shell','dumpsys','batterystats',package,check=False))
 return value
report={'scope':'Fresh installs on API35 x86_64 emulator. Actual compositor, no Compose test clock. Not Galaxy hardware.',
 'battery':'Virtual batterystats retained for diagnostics only; physical power, mAh, temperature and battery life are not measured.',
 'current':sample('v4',Path('app/build/outputs/apk/debug/app-debug.apk'),Path('voice-engine/build/outputs/apk/debug/voice-engine-debug.apk'))}
# Capture a complete real MainActivity frame after initialization.
with (shots/'jarvis-v4-main-real.png').open('wb') as f:
 subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True,timeout=15)
state_results=[]
for phase,mode in [(p,'CINEMATIC') for p in ['INITIALIZING','IDLE','LISTENING','PROCESSING','RESPONDING','ERROR']]+[('IDLE','SILENT')]:
 adb('shell','am','force-stop',package)
 adb('shell','am','start','-W','-n',package+'/.diagnostics.CoreShowcaseActivity','--es','phase',phase,'--es','mode',mode)
 time.sleep(1.4)
 value=capture('state-'+phase.lower()+'-'+mode.lower(),2.2)
 value.update(phase=phase,mode=mode,signal='synthetic, explicitly labelled visual demonstration')
 state_results.append(value)
 with (shots/('jarvis-v4-real-'+phase.lower()+'-'+mode.lower()+'.png')).open('wb') as f:
  subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True,timeout=15)
report['visual_states']=state_results
report['assist_entry_probe']=adb('shell','am','start','-W','-a','android.intent.action.ASSIST','-p',package,check=False)
# Baseline is a pinned, previously verified pair. No source ZIP or invented measurement.
baseline=list(Path('baseline').rglob('JARVIS-Mobile-3.1.0-debug.apk'));voice=list(Path('baseline-voice').rglob('JARVIS-Voz-A-1.0.0-debug.apk'))
if baseline and voice:
 assert hashlib.sha256(baseline[0].read_bytes()).hexdigest()=='a39ca717f4dd8751eb5447eb38e73adff636cfbc4487c862567d9e39bc8de38a'
 assert hashlib.sha256(voice[0].read_bytes()).hexdigest()=='fb617256ce003217858cb52a426e1770687d886272eef7b1e97f526b27be3f45'
 report['baseline_3_1']=sample('v3-1',baseline[0],voice[0])
else:report['baseline_3_1']={'unavailable':'Previous artifact inaccessible; no baseline numbers invented.'}
(out/'startup-idle-comparison.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
