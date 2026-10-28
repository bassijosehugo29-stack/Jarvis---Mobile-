#!/usr/bin/env python3
"""CI/emulator only. Fresh fixtures, real am-start/proc/meminfo/gfxinfo. Never run against a user's phone."""
from pathlib import Path
import json,re,subprocess,time,os

out=Path('app/build/reports/performance');out.mkdir(parents=True,exist_ok=True)
package='br.com.jarvis.mobile'
def adb(*args,check=True):
 r=subprocess.run(['adb',*args],capture_output=True,text=True,timeout=45)
 if check and r.returncode:raise RuntimeError('adb failed: '+r.stderr[:200])
 return r.stdout
def install(apk):adb('install','-r',str(apk))
# Never allow the fresh-install benchmark to remove apps from a physical/personal device.
if os.environ.get('GITHUB_ACTIONS') != 'true':
 print('Medição comparativa ignorada: exclusiva do CI, sem remover qualquer aplicativo.')
 raise SystemExit(0)
if adb('shell','getprop','ro.kernel.qemu').strip() != '1':
 raise SystemExit('Refusing destructive fixture setup: target is not an Android emulator.')
def sample(label,app,voice):
 # Only the ephemeral CI emulator: remove synthetic fixtures to compare fresh installs.
 adb('uninstall',package,check=False);adb('uninstall','br.com.jarvis.voicea',check=False)
 install(voice);install(app)
 starts=[]
 for _ in range(3):
  adb('shell','am','force-stop',package)
  result=adb('shell','am','start','-W','-n',package+'/.MainActivity')
  starts.append({k:int(v) for k,v in re.findall(r'(ThisTime|TotalTime|WaitTime):\s*(\d+)',result)})
  time.sleep(.8)
 time.sleep(2)
 pid=adb('shell','pidof',package).strip().split()[0]
 def ticks():
  s=adb('shell','run-as',package,'cat','/proc/'+pid+'/stat',check=False)
  try:
   fields=s[s.rfind(')')+2:].split();return int(fields[11])+int(fields[12])
  except (ValueError,IndexError):return None
 hz=adb('shell','getconf','CLK_TCK',check=False).strip()
 hz=int(hz) if hz.isdigit() else None
 adb('shell','dumpsys','gfxinfo',package,'reset')
 before=ticks();start=time.monotonic();time.sleep(5);elapsed=time.monotonic()-start;after=ticks()
 mem=adb('shell','dumpsys','meminfo',package);gfx=adb('shell','dumpsys','gfxinfo',package,'framestats')
 (out/(label+'-meminfo.txt')).write_text(mem);(out/(label+'-gfxinfo.txt')).write_text(gfx)
 (out/(label+'-batterystats.txt')).write_text(adb('shell','dumpsys','batterystats',package,check=False))
 pss=re.search(r'TOTAL PSS:\s*(\d+)',mem)
 if not pss:pss=re.search(r'^\s*TOTAL\s+(\d+)',mem,re.M)
 frames=[];header=None
 for line in gfx.splitlines():
  if line.startswith('Flags,IntendedVsync,'):header=line.split(',');continue
  if header and re.match(r'^0,\d+',line):
   values=line.split(',')
   try:
    duration=(int(values[header.index('FrameCompleted')])-int(values[header.index('IntendedVsync')]))/1e6
    if 0<duration<1000:frames.append(duration)
   except (ValueError,IndexError):pass
 frames.sort()
 return {'cold_start_ms':starts,'idle_sample_seconds':elapsed,'pss_kb':int(pss.group(1)) if pss else None,
  'cpu_one_core_percent':((after-before)/hz/elapsed*100) if before is not None and after is not None and hz else None,
  'clock_ticks_hz':hz,'rendered_frames':len(frames),'frame_p95_ms':frames[int((len(frames)-1)*.95)] if frames else None}

report={'scope':'Fresh installs on API35 x86_64 emulator; three cold launches, five-second idle sample. Not Galaxy hardware.',
 'battery':'batterystats retained as emulator diagnostics only; physical power, mAh and battery life are not measured.',
 'current':sample('v4',Path('app/build/outputs/apk/debug/app-debug.apk'),Path('voice-engine/build/outputs/apk/debug/voice-engine-debug.apk'))}
baseline=list(Path('baseline').rglob('JARVIS-Mobile-3.1.0-debug.apk'));baseline_voice=list(Path('baseline-voice').rglob('JARVIS-Voz-A-1.0.0-debug.apk'))
if baseline and baseline_voice:report['baseline_3_1']=sample('v3-1',baseline[0],baseline_voice[0])
else:report['baseline_3_1']={'unavailable':'Previous artifact was not accessible; no baseline measurements invented.'}
(out/'startup-idle-comparison.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
