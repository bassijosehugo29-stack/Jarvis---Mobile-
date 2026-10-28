"""Voice auditions only. Does not change/build Android app or contact a synthesis API."""
from __future__ import annotations
import hashlib, json, os, pathlib, re, subprocess, sys, time, urllib.request, wave

ROOT=pathlib.Path(__file__).resolve().parent
CACHE=ROOT/'cache';OUTPUT=ROOT/'output'
TEXTS=[
 'Bom dia, senhor. Todos os sistemas estão operacionais.',
 'Às suas ordens, senhor.',
 'Entendido. Iniciando os procedimentos.',
 'Senhor, identifiquei uma atualização importante. Deseja que eu apresente os detalhes?',
]
PROFILES=[
 {'key':'A','name':'Executivo clássico','voice':'faber','length_scale':1.08,'noise_scale':0.55,'noise_w':0.70,'sentence_silence':0.24,
  'filter':'highpass=f=65,equalizer=f=170:t=q:w=0.8:g=1.0,acompressor=threshold=0.125:ratio=1.5:attack=15:release=120:makeup=1'},
 {'key':'B','name':'Cinematográfico','voice':'cadu','length_scale':1.12,'noise_scale':0.55,'noise_w':0.68,'sentence_silence':0.38,
  'filter':'highpass=f=65,equalizer=f=135:t=q:w=0.8:g=1.4,equalizer=f=2800:t=q:w=1:g=0.7,acompressor=threshold=0.125:ratio=1.8:attack=18:release=150:makeup=1,aecho=0.96:0.96:27|43:0.028|0.016'},
 {'key':'C','name':'Inteligência artificial sofisticada','voice':'jeff','length_scale':1.04,'noise_scale':0.50,'noise_w':0.65,'sentence_silence':0.25,
  'filter':'highpass=f=70,equalizer=f=2100:t=q:w=1:g=0.9,acompressor=threshold=0.125:ratio=1.7:attack=12:release=100:makeup=1,aecho=0.98:0.98:9:0.035'},
]

def get(url):
 request=urllib.request.Request(url,headers={'User-Agent':'JARVIS-Voice-Audition/1.0'})
 with urllib.request.urlopen(request,timeout=120) as response:return response.read()

def download():
 CACHE.mkdir(exist_ok=True);OUTPUT.mkdir(exist_ok=True)
 api=json.loads(get('https://huggingface.co/api/models/rhasspy/piper-voices'))
 revision=api['sha'];assert re.fullmatch('[0-9a-f]{40}',revision)
 base=f'https://huggingface.co/rhasspy/piper-voices/resolve/{revision}'
 voices=json.loads(get(f'{base}/voices.json'))
 manifest={'repository':'rhasspy/piper-voices','revision':revision,'profiles':PROFILES,'texts':TEXTS,'models':[],'download_used_internet':True}
 for p in PROFILES:
  key=f"pt_BR-{p['voice']}-medium";entry=voices[key]
  prefix=f"pt/pt_BR/{p['voice']}/medium"
  card=get(f'{base}/{prefix}/MODEL_CARD').decode()
  assert re.search(r'License:\s*CC0',card,re.I),f'Unsupported dataset license for {key}'
  (OUTPUT/f"MODEL_CARD-{p['voice']}.txt").write_text(card,encoding='utf-8')
  files=[]
  for suffix in ['.onnx','.onnx.json']:
   remote=f'{prefix}/{key}{suffix}';data=get(f'{base}/{remote}')
   expected=entry['files'][remote]
   assert len(data)==expected['size_bytes']
   assert hashlib.md5(data).hexdigest()==expected['md5_digest']
   target=CACHE/(key+suffix);target.write_bytes(data)
   files.append({'name':target.name,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),'upstream_md5_verified':True,'url':f'{base}/{remote}'})
  config=json.loads((CACHE/(key+'.onnx.json')).read_text())
  assert config['language']['code']=='pt_BR' and config['num_speakers']==1
  manifest['models'].append({'voice':key,'language':'pt_BR','dataset_license_as_declared':'CC0','card_url':f'{base}/{prefix}/MODEL_CARD','files':files,'sample_rate':config['audio']['sample_rate']})
  print('Downloaded and verified',key,flush=True)
 (OUTPUT/'provenance.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False))
 (OUTPUT/'frases.txt').write_text('\n'.join(TEXTS)+'\n')

def ffmpeg(source,target,filters):
 subprocess.run(['ffmpeg','-nostdin','-hide_banner','-loglevel','error','-y','-i',str(source),'-af',filters,'-ar','22050','-ac','1','-c:a','pcm_s16le',str(target)],check=True)

def measure(path):
 import numpy as np
 with wave.open(str(path),'rb') as w:
  assert w.getnchannels()==1 and w.getsampwidth()==2 and w.getframerate()==22050
  values=np.frombuffer(w.readframes(w.getnframes()),dtype='<i2').astype(float)/32768
  assert len(values)>22050 and np.isfinite(values).all()
  return {'seconds':round(len(values)/22050,3),'peak':round(float(np.abs(values).max()),5),'rms':round(float(np.sqrt(np.mean(values**2))),5),'bytes':path.stat().st_size,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()}

def render():
 import numpy as np
 from piper.voice import PiperVoice
 OUTPUT.mkdir(exist_ok=True);(OUTPUT/'originais-sem-efeitos').mkdir(exist_ok=True);(OUTPUT/'frases-separadas').mkdir(exist_ok=True)
 links=json.loads(subprocess.check_output(['ip','-j','link','show']))
 routes=json.loads(subprocess.check_output(['ip','-j','route','show']))
 assert {link['ifname'] for link in links}=={'lo'} and routes==[], 'Synthesis must run with no network interfaces/routes'
 manifest=json.loads((OUTPUT/'provenance.json').read_text())
 manifest['offline_verified']={'interfaces':[link['ifname'] for link in links],'routes':routes,'namespace':os.readlink('/proc/self/ns/net'),'synthesis_api_calls':0,'location':'GitHub hosted Linux CPU runner, not Samsung phone'}
 manifest['runtime']={'piper-tts':'1.2.0','onnxruntime':'1.22.1','neural_model_family':'Piper / VITS','pitch_shift_applied':False,'reference_cloning':False}
 manifest['rendered']=[]
 for profile in PROFILES:
  start=time.monotonic();name=f"pt_BR-{profile['voice']}-medium"
  voice=PiperVoice.load(CACHE/(name+'.onnx'),use_cuda=False)
  assert voice.session.get_providers()==['CPUExecutionProvider']
  parts=[]
  for i,text in enumerate(TEXTS,1):
   target=OUTPUT/'originais-sem-efeitos'/f"{profile['key']}-{i}.wav"
   with wave.open(str(target),'wb') as out:
    voice.synthesize(text,out,length_scale=profile['length_scale'],noise_scale=profile['noise_scale'],noise_w=profile['noise_w'],sentence_silence=profile['sentence_silence'])
   with wave.open(str(target),'rb') as src:
    assert src.getframerate()==22050
    data=np.frombuffer(src.readframes(src.getnframes()),dtype='<i2').copy()
   # Trim excess silence but never alter text or pitch. Keep short boundaries.
   signal=np.flatnonzero(np.abs(data.astype(np.int32))>160)
   assert len(signal)>1000
   data=data[max(0,int(signal[0])-1102):min(len(data),int(signal[-1])+2205)]
   parts+=[data,np.zeros(int(22050*1.05),dtype='<i2')]
  base=OUTPUT/'originais-sem-efeitos'/f"{profile['key']}-comparacao.wav"
  samples=np.concatenate(parts[:-1])
  with wave.open(str(base),'wb') as out:
   out.setnchannels(1);out.setsampwidth(2);out.setframerate(22050);out.writeframes(samples.tobytes())
  effected=CACHE/f"{profile['key']}-effected.wav";ffmpeg(base,effected,profile['filter'])
  # Match loudness so a louder sample is not automatically perceived as better.
  cmd=['ffmpeg','-nostdin','-hide_banner','-i',str(effected),'-af','loudnorm=I=-20:TP=-1.5:LRA=9:print_format=json','-f','null','-']
  result=subprocess.run(cmd,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE,text=True,check=True)
  stats=json.loads(result.stderr[result.stderr.rfind('{'):])
  norm='loudnorm=I=-20:TP=-1.5:LRA=9:linear=true:measured_I={input_i}:measured_TP={input_tp}:measured_LRA={input_lra}:measured_thresh={input_thresh}:offset={target_offset}'.format(**stats)
  final=OUTPUT/f"JARVIS-{profile['key']}.wav";ffmpeg(effected,final,norm)
  subprocess.run(['ffmpeg','-nostdin','-hide_banner','-loglevel','error','-y','-i',str(final),'-c:a','libmp3lame','-b:a','128k',str(OUTPUT/f"JARVIS-{profile['key']}.mp3")],check=True)
  for i in range(1,5):
   ffmpeg(OUTPUT/'originais-sem-efeitos'/f"{profile['key']}-{i}.wav",OUTPUT/'frases-separadas'/f"{profile['key']}-frase-{i}.wav",profile['filter']+',loudnorm=I=-20:TP=-1.5:LRA=9')
  info={'key':profile['key'],'profile':profile['name'],'voice':name,'audio':measure(final),'render_seconds':round(time.monotonic()-start,3),'normalization_target_lufs':-20,'text_exact_inputs':TEXTS,'filter':profile['filter']}
  assert info['audio']['peak']<0.999 and info['audio']['rms']>0.01
  manifest['rendered'].append(info)
  print('Rendered',profile['key'],info['audio'],flush=True)
 (OUTPUT/'provenance.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False))
 readme='''JARVIS — AUDIÇÃO DE VOZ, SEM ALTERAÇÃO DO APK

A: Executivo clássico — Faber / pt-BR.
B: Cinematográfico — Cadu / pt-BR.
C: Inteligência artificial sofisticada — Jeff / pt-BR.

Cada MP3/WAV principal contém as quatro frases, na ordem solicitada, sem locução extra. Originais sem efeitos e frases separadas estão nas subpastas para comparação.

Modelos públicos Piper/VITS de português brasileiro, dataset declarado CC0 nos cartões anexados. Perfis sonoros desenhados para esta audição, sem treinamento/clonagem de uma pessoa do vídeo. Não há promessa de voz exclusiva. Não se fez pitch shift: são vozes-base masculinas distintas, com ritmo e tratamento leve. Música não foi adicionada.

Internet usada somente para instalar ferramentas e baixar modelos. A síntese foi executada com a rede bloqueada por namespace Linux; prova em provenance.json. Não foi usada API de síntese ou chave paga. Reprodução dos arquivos baixados funciona sem internet. Qualidade, timbre, gênero percebido e naturalidade devem ser aprovados ouvindo os arquivos.

As vozes não foram integradas ao JARVIS e não foram testadas no Galaxy. Um motor Android compatível com Piper/ONNX pode oferecer síntese local, mas desempenho/integração e licenças do motor precisam ser validados após a escolha. Android TTS nativo é outra opção gratuita, de timbre dependente das vozes instaladas; não foi ouvido nem medido neste Linux.

63407.mp4 não estava disponível no ambiente. Nenhuma afirmação de análise auditiva desse vídeo.
'''
 (OUTPUT/'LEIA-ME.txt').write_text(readme,encoding='utf-8')

if __name__=='__main__':
 {'download':download,'render':render}[sys.argv[1]]()
