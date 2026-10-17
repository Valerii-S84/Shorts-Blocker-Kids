"""Publish the verified remaster and replace stale video handoff facts only."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

from natural_review import BASE, WORK, save

PACK=BASE.parents[2]/'dist/google-play-review-20261017'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest().upper()


def main():
    visual=json.loads((WORK/'visual-review.json').read_text(encoding='utf-8'))
    if visual.get('result')!='PASS':
        raise RuntimeError('Visual inspection has not been recorded')
    rows=[json.loads((WORK/f'verification-{lang}.json').read_text(encoding='utf-8')) for lang in ['en','uk']]
    for row in rows:
        if row['full_decode']!='PASS' or row['caption_timing']!='PASS':
            raise RuntimeError('A required media gate has not passed')
        if sha(WORK/row['file']).lower()!=row['sha256']:
            raise RuntimeError('Video changed after final verification')
        if row['sha256']!=visual['sha256'][row['language']]:
            raise RuntimeError('Visual inspection refers to a different video')
    backup=WORK/'previous-rejected-export'
    backup.mkdir(exist_ok=True)
    changed=[]
    for lang in ['en','uk']:
        for extension in ['mp4','srt']:
            name=f'accessibility-review-{lang}.{extension}'
            target=PACK/'video'/name
            if not (backup/name).exists():
                shutil.copy2(target,backup/name)
            shutil.copyfile(WORK/name,target)
            changed.append(target)
    quality={'prepared_date':'2026-10-17','purpose':'Locally remastered Google Play Accessibility demonstration',
        'generation':{'provider':'OpenAI','model':'gpt-4o-mini-tts','voice':'cedar',
                      'speed':1.04,'continuous_takes_per_language':1,
                      'scene_dependent_audio_time_stretch':False},
        'visual_review':visual,'videos':[],
        'limitations':['Audio listening assessment was automated over each complete narration track before final loudness normalization. No human listening is claimed.',
                       'Visual inspection used the chronological sequence at 2.5-second intervals plus key transition frames, not continuous video playback.',
                       'Google Play hosting, submission and approval have not been performed.']}
    for row in rows:
        quality['videos'].append({k:row[k] for k in ['language','file','duration_seconds','sha256',
            'full_decode','integrated_loudness_lufs','true_peak_dbtp','silent_gaps_over_1_4_seconds',
            'caption_cues','caption_timing','motion_speed']})
    quality['transcript_note']='Full ASR was used for alignment, not as proof of naturalness. The English final scope clause lacks ASR timestamps and is stated in the visible footer instead of untimed subtitles.'
    quality_path=PACK/'video/video-quality-report.json'
    save(quality_path,quality)
    changed.append(quality_path)
    readme=PACK/'READ-ME-FIRST.txt'
    text=readme.read_text(encoding='utf-8')
    lines=text.splitlines()
    for i,line in enumerate(lines):
        if line.startswith('СТАТУС ВІДЕО:'):
            lines[i]='СТАТУС ВІДЕО: ЛОКАЛЬНИЙ ПЕРЕМОНТАЖ ЗАВЕРШЕНО. EN — 85,83 с; UK — 112,70 с. Перевірено декодування, рівні звуку, субтитри та послідовність кадрів. Оцінка повних аудіодоріжок автоматична; людського прослуховування не було. Хостинг, Play Console і подання не виконані; схвалення Google не отримано.'
        elif 'Обидві версії містять 5 сцен' in line:
            lines[i]=line.split('Обидві версії містять')[0]+'Монтаж містить 10 розділів із підписами. Зайве очікування скорочено; швидкість реальних дій — 1×. Тривалість змонтована під окрему цілісну доріжку кожною мовою.'
        elif line.startswith('Інтерфейси застосунку, Android і YouTube'):
            lines[i]='Інтерфейси застосунку, Android і YouTube англійські в обох роликах. Формат: H.264, 720x1680, 30 кадрів/с, yuv420p; AAC, моно, 48 кГц. OpenAI gpt-4o-mini-tts, голос cedar, швидкість синтезу 1,04; по одному цілісному запису кожною мовою. Субтитри прив’язані до мовлення. Вступ пояснює призначення застосунку. Синтезоване озвучення позначено в кадрі.'
        elif line.startswith('Автоматична перевірка:'):
            lines[i]='Перевірка кінцевих MP4: повне декодування PASS; гучність EN −16,53 LUFS, UK −16,58 LUFS; true peak EN −1,46 dBTP, UK −1,47 dBTP. Тиші довше 1,4 с за порога −45 dB не виявлено. Повні WAV оцінено автоматично через gpt-audio-1.5; це не людське прослуховування. Кадри переглянуто послідовно з інтервалом 2,5 с та окремо на ключових переходах. Межі перевірки й контрольні суми: video/video-quality-report.json.'
    readme.write_text('\n'.join(lines)+'\n',encoding='utf-8')
    changed.append(readme)
    upload=PACK/'texts/video-upload-instructions.txt'
    text=upload.read_text(encoding='utf-8')
    start=text.index('CURRENT STATUS:')
    end=text.index('RECORDED CONTENT')
    replacement=('CURRENT STATUS: LOCAL REMASTER EXPORTED. EN 85.83 seconds; UK 112.70 seconds. '
        'Full decoding, levels and caption timing passed. Chronological frames were inspected at 2.5-second intervals and at key transitions. '
        'Complete narration tracks received automated audio listening assessment; no human listening is claimed. Hosting, anonymous link access and Play submission are not completed. Google approval is not obtained.\n\n'
        'FILES\nPrimary: video/accessibility-review-en.mp4 — English narration and synchronized captions.\n'
        'Additional: video/accessibility-review-uk.mp4 — Ukrainian narration and synchronized captions.\n'
        'Actual English phone UI in both. H.264 720x1680 30fps yuv420p; AAC mono 48kHz. '
        'OpenAI gpt-4o-mini-tts, cedar, synthesis speed 1.04, one continuous take per language. '
        'Idle waits were edited; recorded gestures play at normal speed. No scene-dependent speech acceleration. '
        'AI narration is disclosed in the picture. Matching SRT files are included.\n'
        'Measured integrated loudness: EN -16.53 LUFS, UK -16.58 LUFS. True peaks: EN -1.46 dBTP, UK -1.47 dBTP. '
        'No silent gap longer than 1.4s at -45dB. See video/video-quality-report.json for exact verification limits.\n\n')
    upload.write_text(text[:start]+replacement+text[end:],encoding='utf-8')
    changed.append(upload)
    manifest_path=PACK/'manifest.json'
    manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
    manifest['status']='LOCAL REMASTER EXPORTED - automated audio assessment; hosting and Play submission pending'
    manifest['recording']['audio']='Continuous OpenAI narration per language; AAC mono 48000Hz. AI speech is disclosed in the picture.'
    manifest['recording']['narration']={'provider':'OpenAI','model':'gpt-4o-mini-tts','voice':'cedar',
        'languages':['en','uk'],'synthesis_speed':1.04,'continuous_takes_per_language':1,
        'delivery':'Conversational script, short pauses, no scene-dependent acceleration'}
    for item,row in zip(manifest['videos'],rows):
        assert item['language']==row['language']
        item['duration_seconds']=row['duration_seconds']
    manifest['video_verification']={'render':'PASS for both','decode':'PASS for both',
        'visual_review':visual['method'],'chapter_count_per_video':10,
        'audio_review':'Automated full narration listening; no human listening claimed',
        'report':'video/video-quality-report.json'}
    entries={entry['file']:entry for entry in manifest['files']}
    for path in changed:
        relative=path.relative_to(PACK).as_posix()
        entries[relative]={'file':relative,'bytes':path.stat().st_size,'sha256':sha(path)}
    manifest['files']=list(entries.values())
    save(manifest_path,manifest)
    changed.append(manifest_path)
    for row in rows:
        assert sha(PACK/'video'/row['file']).lower()==row['sha256']
    for path in changed:
        assert path.resolve().is_relative_to(PACK.resolve())
    assert '173.9' not in upload.read_text(encoding='utf-8')
    assert '173,9' not in readme.read_text(encoding='utf-8')
    print(json.dumps({'published':[str(p.relative_to(PACK)) for p in changed],
                      'copied_media_hashes':'MATCH','handoff_metadata':'UPDATED'},ensure_ascii=True),flush=True)


if __name__=='__main__':
    main()
