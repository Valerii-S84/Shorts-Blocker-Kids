"""Generate timed OpenAI narration and stage verified MP4s without replacing originals.

Run with OPENAI_API_KEY configured. Publish staged files separately after audio QA.
Only public narration text is sent to OpenAI; videos and the parent PIN stay local.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import hashlib
import json
import math
import os
from pathlib import Path
import subprocess
import sys
import urllib.error
import urllib.request

BASE = Path(__file__).resolve().parent
PACK = BASE.parents[2] / 'dist/google-play-review-20261017'
AUDIO = BASE / 'narration'
FFMPEG = Path(os.environ['LOCALAPPDATA']) / 'Microsoft/WinGet/Links/ffmpeg.exe'
FFPROBE = FFMPEG.with_name('ffprobe.exe')

def run(args, timeout=180):
    result = subprocess.run(list(map(str,args)), capture_output=True, timeout=timeout, shell=False)
    if result.returncode:
        raise RuntimeError('Local media command failed: ' + result.stderr.decode('utf-8',errors='replace')[-1400:])
    return result.stdout

def probe(path):
    return json.loads(run([FFPROBE,'-v','error','-show_streams','-show_format','-of','json',path]))

def load_plan():
    plan = json.loads((BASE/'narration-plan.json').read_text(encoding='utf-8-sig'))
    duration = float(plan.get('duration_seconds',plan.get('source_duration_seconds',0)))
    if not math.isfinite(duration) or not 0 < duration < 600:
        raise ValueError('Invalid narration duration')
    if plan.get('model') != 'gpt-4o-mini-tts' or plan.get('voice') not in ('marin','cedar'):
        raise ValueError('Expected the selected OpenAI model and voice')
    cursor = 0
    for cue in plan['cues']:
        if not (cursor <= cue['start'] < cue['end'] <= duration):
            raise ValueError('Narration cues overlap or exceed the video')
        cursor = cue['end']
        for lang in ('en','uk'):
            text = cue['text_'+lang]
            if not isinstance(text,str) or not 1 <= len(text) <= 1800:
                raise ValueError('Invalid narration text')
    return plan,duration

def speech(job):
    lang,index,cue,plan,key = job
    instructions = (
        'Speak natural native Ukrainian, like a friendly Ukrainian presenter explaining a phone demonstration to a colleague. '
        'Pronounce English brand names naturally as written. '
        if lang == 'uk' else
        'Speak natural conversational English, like a friendly presenter explaining a phone demonstration to a colleague. '
    ) + ('Use a clear, confident, lively speaking voice with an audible smile. '
         'Normal speaking volume, medium-brisk everyday pace, varied expressive intonation and flowing connected phrases. '
         'Crisp words, brief natural breaths. Avoid breathy whispering, sleepy delivery, solemn documentary or theatrical narration. '
         'Read the supplied text exactly. No music, no extra introductions or commentary.')
    instructions = plan.get('instructions', {}).get(lang, instructions)
    payload = {'model':plan['model'],'voice':plan['voice'],'input':cue['text_'+lang],
               'instructions':instructions,'response_format':'wav',
               'speed':plan.get('speed', 1.0)}
    raw = json.dumps(payload,ensure_ascii=False).encode('utf-8')
    signature = hashlib.sha256(raw).hexdigest()
    wave = AUDIO/f'{lang}-{index:02}.wav'
    stamp = wave.with_suffix('.json')
    if wave.is_file() and stamp.is_file():
        cached = json.loads(stamp.read_text(encoding='utf-8'))
        if cached.get('request_sha256') == signature and cached.get('wav_sha256') == hashlib.sha256(wave.read_bytes()).hexdigest():
            return lang,index,wave
    request = urllib.request.Request('https://api.openai.com/v1/audio/speech',data=raw,
        headers={'Authorization':'Bearer '+key,'Content-Type':'application/json'},method='POST')
    try:
        with urllib.request.urlopen(request,timeout=120) as response:
            audio = response.read(25_000_001)
    except urllib.error.HTTPError as error:
        # API error bodies can contain a key fragment; never log them.
        raise RuntimeError(f'OpenAI speech request failed with HTTP {error.code}; no automatic retry') from None
    except urllib.error.URLError:
        raise RuntimeError('OpenAI speech network request failed; no automatic retry') from None
    if len(audio) > 25_000_000 or len(audio) < 100 or audio[:4] != b'RIFF' or audio[8:12] != b'WAVE':
        raise RuntimeError('OpenAI did not return a bounded WAV audio file')
    temporary = wave.with_suffix('.wav.part')
    temporary.write_bytes(audio)
    os.replace(temporary,wave)
    stamp.write_text(json.dumps({'request_sha256':signature,'wav_sha256':hashlib.sha256(audio).hexdigest(),
        'model':plan['model'],'voice':plan['voice'],'language':lang},indent=2)+'\n',encoding='utf-8')
    print(f'Speech ready: {lang} cue {index}',flush=True)
    return lang,index,wave

def video_hash(path):
    return run([FFMPEG,'-v','error','-i',path,'-map','0:v:0','-c','copy','-f','hash','-hash','sha256','-']).decode().strip()

def mix(lang,plan,duration):
    source = PACK/f'video/accessibility-review-{lang}.mp4'
    target = AUDIO/f'accessibility-review-{lang}-voiced.mp4'
    inputs,filters,parts = [],[],[]
    for index,cue in enumerate(plan['cues'],1):
        wave = AUDIO/f'{lang}-{index:02}.wav'
        measured = float(probe(wave)['format']['duration'])
        slot = cue['end']-cue['start']-.1
        tempo = max(1,measured/slot)
        if tempo > 1.18:
            raise RuntimeError(f'{lang} cue {index} is too long for its scene; shorten its text before retrying')
        inputs += ['-i',wave]
        label = f'a{index}'
        filters.append(f'[{index-1}:a]aresample=48000,atempo={tempo:.6f},adelay={round(cue["start"]*1000)}:all=1[{label}]')
        parts.append(f'[{label}]')
    filters.append(''.join(parts)+f'amix=inputs={len(parts)}:duration=longest:normalize=0,apad,atrim=duration={duration},loudnorm=I=-16:TP=-1.5:LRA=7[out]')
    track = AUDIO/f'voiceover-{lang}.wav'
    run([FFMPEG,'-hide_banner','-loglevel','error','-nostdin','-y',*inputs,
        '-filter_complex',';'.join(filters),'-map','[out]','-ar','48000','-ac','1','-c:a','pcm_s16le',track])
    run([FFMPEG,'-hide_banner','-loglevel','error','-nostdin','-y','-i',source,'-i',track,
        '-map','0:v:0','-map','1:a:0','-c:v','copy','-c:a','aac','-b:a','160k','-t',duration,
        '-metadata:s:a:0','language='+('eng' if lang=='en' else 'ukr'),
        '-metadata','comment=',
        '-movflags','+faststart',target])
    info = probe(target)
    audio_streams = [s for s in info['streams'] if s['codec_type']=='audio']
    if len(audio_streams)!=1 or audio_streams[0]['codec_name']!='aac' or abs(float(info['format']['duration'])-duration)>.1:
        raise RuntimeError('Voiced MP4 format does not match the target')
    if video_hash(source)!=video_hash(target):
        raise RuntimeError('Video packets changed during audio muxing')
    run([FFMPEG,'-v','error','-i',target,'-map','0:v:0','-map','0:a:0','-f','null','-'])
    return {'language':lang,'file':str(target),'duration_seconds':float(info['format']['duration']),
            'video_unchanged':True,'decode':'PASS','audio_codec':'aac',
            'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}

def main():
    global AUDIO
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--prepare-only',action='store_true')
    parser.add_argument('--preview-only',action='store_true')
    parser.add_argument('--natural', action='store_true',
                        help='Generate the continuous narration and align the new edit')
    args=parser.parse_args()
    if args.natural:
        from natural_review import generate_and_align
        generate_and_align()
        return
    plan,duration=load_plan()
    if args.prepare_only:
        print(f'PLAN=PASS; languages=2; cues={len(plan["cues"])}; duration={duration}')
        return
    key=os.environ.get('OPENAI_API_KEY','').strip()
    if not key:
        raise RuntimeError('OPENAI_API_KEY is not configured; no API request made')
    if args.preview_only:
        AUDIO=AUDIO/'preview-cedar'
    AUDIO.mkdir(exist_ok=True)
    if args.preview_only:
        for lang in ('en','uk'):
            speech((lang,3,plan['cues'][2],plan,key))
        print('PREVIEWS_READY=en,uk',flush=True)
        return
    jobs=[(lang,index,cue,plan,key) for lang in ('en','uk') for index,cue in enumerate(plan['cues'],1)]
    # Fail on an invalid key before submitting the remaining paid requests.
    speech(jobs[0])
    pool=ThreadPoolExecutor(max_workers=2)
    futures=[pool.submit(speech,job) for job in jobs[1:]]
    try:
        for future in as_completed(futures):
            future.result()
    except Exception:
        for future in futures:
            future.cancel()
        pool.shutdown(wait=True,cancel_futures=True)
        raise
    else:
        pool.shutdown(wait=True)
    rows=[mix(lang,plan,duration) for lang in ('en','uk')]
    (AUDIO/'narration-validation.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(rows,indent=2),flush=True)

if __name__=='__main__':
    try:
        main()
    except (RuntimeError,ValueError,OSError,KeyError,subprocess.TimeoutExpired) as error:
        print('NARRATION ERROR: '+str(error),file=sys.stderr)
        raise SystemExit(1)
