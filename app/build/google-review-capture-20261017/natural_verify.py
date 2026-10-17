"""One final local decode/level/timing phase and visual review sheets."""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import math
from pathlib import Path
import re
import subprocess

from PIL import Image, ImageDraw
from natural_review import WORK, save


def run(args, timeout=240):
    result=subprocess.run([str(x) for x in args],capture_output=True,timeout=timeout,shell=False)
    if result.returncode:
        raise RuntimeError(result.stderr.decode('utf-8',errors='replace')[-1500:])
    return result


def review_frames(lang, target, duration):
    directory=WORK/f'visual-{lang}'
    directory.mkdir(exist_ok=True)
    raw=run(['ffmpeg','-v','error','-i',target,'-vf',"select='not(mod(n,75))',scale=200:466",
             '-fps_mode','vfr','-pix_fmt','rgb24','-f','rawvideo','-']).stdout
    size=200*466*3
    if len(raw)%size:
        raise RuntimeError('Incomplete visual frame')
    count=len(raw)//size
    sheets=[]
    for first in range(0,count,16):
        amount=min(16,count-first)
        sheet=Image.new('RGB',(800,math.ceil(amount/4)*488),'#132338')
        draw=ImageDraw.Draw(sheet)
        for offset in range(amount):
            index=first+offset
            im=Image.frombytes('RGB',(200,466),raw[index*size:(index+1)*size])
            x,y=(offset%4)*200,(offset//4)*488
            sheet.paste(im,(x,y+22))
            draw.text((x+5,y+4),f'{lang.upper()} {index*2.5:05.1f}s',fill='white')
        destination=directory/f'sequence-{first//16+1}.jpg'
        sheet.save(destination,quality=90)
        sheets.append(str(destination))
    return {'interval_seconds':2.5,'frames':count,'sheets':sheets}


def verify(lang):
    target=WORK/f'accessibility-review-{lang}.mp4'
    edit=json.loads((WORK/f'edit-{lang}.json').read_text(encoding='utf-8'))
    cues=json.loads((WORK/f'caption-cues-{lang}.json').read_text(encoding='utf-8'))
    probe=json.loads(run(['ffprobe','-v','error','-show_streams','-show_format','-of','json',target]).stdout)
    v=[s for s in probe['streams'] if s['codec_type']=='video']
    a=[s for s in probe['streams'] if s['codec_type']=='audio']
    assert len(v)==1 and len(a)==1
    assert v[0]['codec_name']=='h264' and v[0]['width']==720 and v[0]['height']==1680
    assert v[0]['pix_fmt']=='yuv420p' and v[0]['avg_frame_rate']=='30/1'
    assert a[0]['codec_name']=='aac' and a[0]['sample_rate']=='48000' and a[0]['channels']==1
    assert abs(float(probe['format']['duration'])-edit['seconds'])<.08
    assert int(v[0]['nb_frames'])==edit['frames']
    assert sum(p['frames'] for p in edit['pieces'])==edit['frames']
    assert edit['motion_speed']==1.0
    result=run(['ffmpeg','-hide_banner','-nostdin','-i',target,'-map','0:v:0','-map','0:a:0',
                '-af','silencedetect=noise=-45dB:d=1.4,loudnorm=I=-16:TP=-1.5:LRA=9:print_format=json',
                '-f','null','-'])
    log=result.stderr.decode('utf-8',errors='replace')
    (WORK/f'final-decode-{lang}.log').write_text(log,encoding='utf-8')
    blocks=re.findall(r'\{\s*"input_i"[\s\S]*?\}',log)
    levels=json.loads(blocks[-1])
    silence=[float(x) for x in re.findall(r'silence_duration: ([\d.]+)',log)]
    assert -18.0<=float(levels['input_i'])<=-14.0, levels
    assert float(levels['input_tp'])<=-1.0, levels
    assert not silence or max(silence)<=1.6, silence
    for i,cue in enumerate(cues):
        assert 0<=cue['start']<cue['end']<=edit['seconds']+.01
        assert len(cue['text'].splitlines())<=2
        if i:
            assert cues[i-1]['end']<=cue['start']+.01
    full_audio=json.loads((WORK/f'full-audio-review-{lang}.json').read_text(encoding='utf-8'))
    source_hash=hashlib.sha256((WORK/f'voiceover-{lang}.wav').read_bytes()).hexdigest()
    assert full_audio['audio_sha256']==source_hash
    assert full_audio['verdict']=='acceptable'
    frames=review_frames(lang,target,edit['seconds'])
    row={'language':lang,'file':target.name,'duration_seconds':float(probe['format']['duration']),
         'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),
         'video':'H.264 720x1680 30fps yuv420p','audio':'AAC mono 48000 Hz',
         'full_decode':'PASS','integrated_loudness_lufs':float(levels['input_i']),
         'true_peak_dbtp':float(levels['input_tp']),'silent_gaps_over_1_4_seconds':silence,
         'caption_cues':len(cues),'caption_timing':'PASS','motion_speed':1.0,
         'full_audio_assessment':full_audio,'visual_sequence':frames,'visual_review':'pending',
         'limitations':['Audio naturalness was assessed automatically over both complete narration tracks; no human listening is claimed.',
                       'Google Play approval is not guaranteed or obtained.']}
    save(WORK/f'verification-{lang}.json',row)
    print(json.dumps({key:row[key] for key in ['language','duration_seconds','full_decode',
          'integrated_loudness_lufs','true_peak_dbtp','silent_gaps_over_1_4_seconds','caption_cues']},ensure_ascii=True),flush=True)


if __name__=='__main__':
    with ThreadPoolExecutor(max_workers=2) as pool:
        list(pool.map(verify,['en','uk']))
