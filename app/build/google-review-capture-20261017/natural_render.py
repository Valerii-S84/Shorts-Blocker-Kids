"""Edit authentic phone footage around the continuous OpenAI narration."""
from concurrent.futures import ThreadPoolExecutor
import json
import math
import os
from pathlib import Path
import subprocess

from PIL import Image, ImageDraw, ImageFont
from natural_review import WORK, BASE, align, save

FPS = 30
FONT = ImageFont.truetype(str(Path(os.environ['WINDIR']) / 'Fonts/segoeui.ttf'), 32)
DRAW = ImageDraw.Draw(Image.new('RGB', (720, 240)))


def run(args, timeout=240):
    result = subprocess.run([str(x) for x in args], cwd=WORK, capture_output=True,
                            timeout=timeout, shell=False)
    if result.returncode:
        raise RuntimeError(result.stderr.decode('utf-8', errors='replace')[-1800:])
    return result.stdout


def piece(source, start, end, frames=None):
    return {'source': source, 'source_start': start, 'source_end': end,
            'frames': frames if frames is not None else round((end - start) * FPS)}


def fitted(pieces, frames, pad_index=-1):
    difference = frames - sum(p['frames'] for p in pieces)
    if difference < -1:
        raise RuntimeError('Scene would require speeding up recorded actions')
    pieces[pad_index]['frames'] += difference
    return pieces


def make_timeline(a, lang):
    p = [round(t * FPS) for t in a['paragraph_starts']]
    end = math.ceil(a['seconds'] * FPS)
    choose = next(w['start'] for w in a['words'] if w['text'].strip('.,') ==
                  ('choose' if lang == 'en' else 'обираємо'))
    select = round(choose * FPS)
    chapter_labels = {
        'en': ['Parent setup', 'Create a parent PIN', 'Choose protected apps',
               'Why Accessibility is needed', 'Read the full disclosure',
               'Decline: service remains off', 'Reopen and give consent',
               'Enable the Android permission', 'Regular video: plays normally', 'Shorts: access is blocked'],
        'uk': ['Батьківське налаштування', 'Створення батьківського PIN', 'Вибір застосунків',
               'Навіщо потрібен дозвіл', 'Повне пояснення на екрані',
               'Відмова: службу не ввімкнено', 'Повторне відкриття та згода',
               'Дозвіл у налаштуваннях Android', 'Звичайне відео відтворюється', 'Shorts: доступ заблоковано']}
    windows = [(0,p[1]),(p[1],select),(select,p[2]),(p[2],p[4]),(p[4],p[5]),
               (p[5],p[6]),(p[6],p[7]),(p[7],p[8]),(p[8],p[9]),(p[9],end)]
    clips = [
        [piece('final-opening.mp4',0,4.5)],
        [piece('final-opening.mp4',6.4,7.1)],
        [piece('final-consent.mp4',2,6.25)],
        [piece('final-consent.mp4',6.25,23.5)],
        [piece('final-consent.mp4',23.5,26.5)],
        [piece('final-consent.mp4',65.2,67.0),piece('final-consent.mp4',73.85,75.35),
         piece('final-consent.mp4',79.1,80.4),piece('final-consent.mp4',84.1,85.65),
         piece('final-consent.mp4',89.1,90.4)],
        [piece('final-consent.mp4',95,97.3),piece('final-consent.mp4',109.9,111.3),
         piece('final-consent.mp4',121.7,123.7)],
        [], [], []]
    grant_seconds = (p[8]-p[7])/FPS
    on_word = next(w['start'] for w in a['words'] if w['start'] >= p[7]/FPS and
                   w['text'].strip('.,') == ('shows' if lang == 'en' else 'показує'))
    removed = max(0, 12.05-(on_word-p[7]/FPS), 13.4-grant_seconds)
    if removed:
        clips[7] = [piece('en-grant-final.mp4',0,4),piece('en-grant-final.mp4',4+removed,13.4)]
    else:
        clips[7] = [piece('en-grant-final.mp4',0,13.4)]
    normal_seconds = (p[9]-p[8])/FPS
    clips[8] = [piece('long-video-en.mp4',0,min(10.9,normal_seconds))]
    clips[9] = [piece('shorts-first.mp4',10,10+min(6.5,(end-p[9])/FPS))]
    chapters, flattened = [], []
    for index, (start, stop) in enumerate(windows):
        current = fitted(clips[index], stop-start, 0 if index in [5,6] else -1)
        for item in current:
            item['chapter'] = index
            item['mask_below'] = 596 if index == 8 else None
            flattened.append(item)
        chapters.append({'start': start/FPS,'end':stop/FPS,'title':chapter_labels[lang][index]})
    return {'language':lang,'frames':end,'seconds':end/FPS,'pieces':flattened,
            'chapters':chapters,'motion_speed':1.0,'edits':'Only static waits shortened; some static screens held to match narration'}


def wrap(text):
    lines, current = [], ''
    for word in text.split():
        candidate = (current+' '+word).strip()
        if current and DRAW.textlength(candidate,font=FONT)>648:
            lines.append(current)
            current=word
        else:
            current=candidate
    if current:
        lines.append(current)
    return lines


def captions(words, duration):
    groups, current = [], []
    for word in words:
        candidate = ' '.join(w['text'] for w in current+[word])
        if current and (len(wrap(candidate))>2 or word['end']-current[0]['start']>4.8):
            groups.append(current)
            current=[]
        current.append(word)
        if word['text'].endswith(('.', '!', '?')):
            groups.append(current)
            current=[]
    if current:
        groups.append(current)
    result=[]
    for index, group in enumerate(groups):
        start=max(0,group[0]['start']-.06)
        next_start=groups[index+1][0]['start']-.06 if index+1<len(groups) else duration
        stop=min(duration,next_start,max(w['end'] for w in group)+.18)
        if stop <= start:
            raise RuntimeError('Invalid subtitle timing')
        result.append({'start':start,'end':stop,'text':'\n'.join(wrap(' '.join(w['text'] for w in group)))})
    return result


def stamp(t, ass=False):
    milliseconds=round(t*1000)
    h,m=divmod(milliseconds,3600000)
    m,s=divmod(m,60000)
    s,ms=divmod(s,1000)
    return f'{h}:{m:02}:{s:02}.{ms//10:02}' if ass else f'{h:02}:{m:02}:{s:02},{ms:03}'


def write_captions(lang, cues, timeline):
    srt='\n\n'.join(f'{i+1}\n{stamp(c["start"])} --> {stamp(c["end"])}\n{c["text"]}' for i,c in enumerate(cues))+'\n'
    (WORK/f'accessibility-review-{lang}.srt').write_text(srt,encoding='utf-8')
    header='''[Script Info]
ScriptType: v4.00+
PlayResX: 720
PlayResY: 1680
WrapStyle: 2
ScaledBorderAndShadow: yes
[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Caption,Segoe UI,32,&H00FFFFFF,&H00FFFFFF,&H001C1921,&H00000000,0,0,0,0,100,100,0,0,1,0,0,5,36,36,0,1
Style: Heading,Segoe UI,23,&H00DAE8A0,&H00DAE8A0,&H001C1921,&H00000000,-1,0,0,0,100,100,0,0,1,0,0,5,28,28,0,1
Style: Footer,Segoe UI,17,&H00C4B8AE,&H00C4B8AE,&H001C1921,&H00000000,0,0,0,0,100,100,0,0,1,0,0,5,20,20,0,1
[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
'''
    events=[]
    def event(start,end,style,y,text):
        escaped=text.replace('\n',r'\N')
        events.append(f'Dialogue: 0,{stamp(start,True)},{stamp(end,True)},{style},,0,0,0,,{{\\pos(360,{y})}}{escaped}')
    for cue in cues:
        event(cue['start'],cue['end'],'Caption',1545,cue['text'])
    for i,chapter in enumerate(timeline['chapters']):
        event(chapter['start'],chapter['end'],'Heading',1473,chapter['title'])
        if i in [0,1,2]:
            note='Private PIN entry omitted' if lang=='en' else 'Введення приватного PIN вирізано'
        elif i==8:
            note='Personal content below the player is masked' if lang=='en' else 'Особистий вміст під програвачем приховано'
        elif i==9:
            note='YouTube only · Private feed transitions omitted' if lang=='en' else 'Лише YouTube · Особисту стрічку вирізано'
        else:
            note='Actual phone recording · Idle waits shortened' if lang=='en' else 'Запис із телефона · Зайве очікування скорочено'
        event(chapter['start'],chapter['end'],'Footer',1620,note)
    event(0,timeline['seconds'],'Footer',1650,'AI narration · OpenAI' if lang=='en' else 'Синтезоване озвучення · OpenAI')
    (WORK/f'captions-{lang}.ass').write_text(header+'\n'.join(events)+'\n',encoding='utf-8')


def encode_piece(lang, index, item):
    destination=WORK/f'picture-{lang}-{index:02}.mp4'
    duration=item['source_end']-item['source_start']
    frames=item['frames']
    filters='crop=720:1440:0:54,setsar=1'
    if item['mask_below'] is not None:
        filters+=',drawbox=x=0:y=596:w=720:h=844:color=0x132338:t=fill'
    # Phone capture is sparse VFR. Fill its original timeline before trimming:
    # input seeking followed by STARTPTS otherwise moves the next changed frame early.
    first, last = round(item['source_start']*FPS), round(item['source_end']*FPS)
    filters+=f',fps=30:start_time=0,trim=start_frame={first}:end_frame={last},setpts=PTS-STARTPTS,tpad=stop_mode=clone:stop_duration={max(0,frames/FPS-duration)+.2},trim=end_frame={frames},setpts=PTS-STARTPTS'
    run(['ffmpeg','-v','error','-nostdin','-y','-i',BASE/item['source'],
         '-vf',filters,'-an','-frames:v',str(frames),
         '-c:v','libx264','-threads','2','-preset','veryfast','-crf','18','-pix_fmt','yuv420p',destination])
    return destination


def render(lang):
    plan=json.loads((WORK/'plan.json').read_text(encoding='utf-8-sig'))
    a=json.loads((WORK/f'alignment-{lang}.json').read_text(encoding='utf-8'))
    if lang=='en':
        # ASR has no timed words for this clause. Its scope is clearly stated in the footer.
        displayed=plan['scripts'][lang].replace('This recording demonstrates YouTube; the other','The other')
        transcript=json.loads((WORK/f'voiceover-{lang}.transcript.json').read_text(encoding='utf-8'))
        a=align(displayed,transcript,a['seconds'])
    timeline=make_timeline(a,lang)
    cues=captions(a['words'],timeline['seconds'])
    write_captions(lang,cues,timeline)
    save(WORK/f'edit-{lang}.json',timeline)
    save(WORK/f'caption-cues-{lang}.json',cues)
    paths=[encode_piece(lang,index,item) for index,item in enumerate(timeline['pieces'])]
    concat=WORK/f'concat-{lang}.txt'
    concat.write_text(''.join("file '"+p.name+"'\n" for p in paths),encoding='utf-8')
    picture=WORK/f'picture-{lang}.mp4'
    run(['ffmpeg','-v','error','-nostdin','-y','-f','concat','-safe','0','-i',concat,
         '-an','-c:v','copy',picture])
    target=WORK/f'accessibility-review-{lang}.mp4'
    run(['ffmpeg','-v','error','-nostdin','-y','-i',picture,'-i',WORK/f'voiceover-{lang}.wav',
         '-vf',f'pad=720:1680:0:0:color=0x132338,ass=captions-{lang}.ass',
         '-af','loudnorm=I=-16:TP=-1.5:LRA=9,aresample=48000,apad',
         '-map','0:v:0','-map','1:a:0','-c:v','libx264','-threads','2','-preset','veryfast',
         '-crf','18','-pix_fmt','yuv420p','-c:a','aac','-b:a','160k','-ac','1',
         '-t',str(timeline['seconds']),'-metadata:s:a:0','language='+('eng' if lang=='en' else 'ukr'),
         '-metadata','comment=OpenAI synthetic narration; authentic phone footage; idle waits edited.',
         '-movflags','+faststart',target],timeout=360)
    print(json.dumps({'language':lang,'file':target.name,'seconds':timeline['seconds'],
                      'chapters':len(timeline['chapters']),'subtitles':len(cues)},ensure_ascii=True),flush=True)


if __name__=='__main__':
    with ThreadPoolExecutor(max_workers=2) as pool:
        list(pool.map(render,['en','uk']))
