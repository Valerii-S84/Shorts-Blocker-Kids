"""One continuous OpenAI take per language, then full word alignment for editing."""
from pathlib import Path
import difflib
import hashlib
import json
import os
import re
import urllib.error
import urllib.request
import uuid
import wave

import numpy as np

BASE = Path(__file__).resolve().parent
WORK = BASE / 'natural-remaster'


def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def request_json(endpoint, data, content_type, key, timeout=240):
    request = urllib.request.Request('https://api.openai.com/v1/' + endpoint,
        data=data, headers={'Authorization': 'Bearer ' + key, 'Content-Type': content_type},
        method='POST')
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            payload = response.read(2_000_001)
    except urllib.error.HTTPError as error:
        raise RuntimeError(f'OpenAI {endpoint} failed: HTTP {error.code}; no automatic retry') from None
    except urllib.error.URLError:
        raise RuntimeError(f'OpenAI {endpoint} network failure; no automatic retry') from None
    if len(payload) > 2_000_000:
        raise RuntimeError('Oversized API response')
    return json.loads(payload)


def prepare_wave(source, target):
    with wave.open(str(source), 'rb') as wav:
        rate, channels, width = wav.getframerate(), wav.getnchannels(), wav.getsampwidth()
        if channels != 1 or width != 2:
            raise RuntimeError('Expected mono 16-bit synthesized WAV')
        samples = np.frombuffer(wav.readframes(wav.getnframes()), np.int16).copy()
    frame = max(1, rate // 50)
    padded = np.pad(samples.astype(np.float64), (0, (-len(samples)) % frame))
    rms = np.sqrt(np.mean(padded.reshape(-1, frame) ** 2, axis=1))
    active = np.flatnonzero(rms > 32768 * 10 ** (-48 / 20))
    if len(active) < 25:
        raise RuntimeError('Narration contains insufficient speech')
    first = max(0, int(active[0] * frame - rate * .16))
    last = min(len(samples), int((active[-1] + 1) * frame + rate * .4))
    cuts = []
    gaps = np.flatnonzero(np.diff(active) * frame > 1.4 * rate)
    for index in gaps:
        left = int((active[index] + 1) * frame + rate * .325)
        right = int(active[index + 1] * frame - rate * .325)
        if right > left:
            cuts.append((left, right))
    pieces, cursor = [], first
    for left, right in cuts:
        pieces.append(samples[cursor:left])
        cursor = right
    pieces.append(samples[cursor:last])
    edited = np.concatenate(pieces)
    with wave.open(str(target), 'wb') as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(rate)
        wav.writeframes(edited.tobytes())
    return {'source_seconds': len(samples) / rate, 'seconds': len(edited) / rate,
            'head_trim_seconds': first / rate, 'tail_trim_seconds': (len(samples) - last) / rate,
            'long_silent_gaps_shortened': [(a / rate, b / rate) for a, b in cuts],
            'speech_time_stretch': 1.0}


def transcribe(path, lang, key):
    stamp = path.with_suffix('.transcript.json')
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if stamp.exists():
        result = json.loads(stamp.read_text(encoding='utf-8'))
        if result.get('audio_sha256') == digest:
            return result
        raise RuntimeError('Audio changed after transcription; explicit new alignment required')
    boundary = 'sbk-' + uuid.uuid4().hex
    fields = [('model', 'whisper-1'), ('language', lang), ('response_format', 'verbose_json'),
              ('timestamp_granularities[]', 'word'), ('temperature', '0')]
    parts = [f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode()
             for name, value in fields]
    parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="narration.wav"\r\nContent-Type: audio/wav\r\n\r\n'.encode()
                 + path.read_bytes() + b'\r\n')
    parts.append(f'--{boundary}--\r\n'.encode())
    result = request_json('audio/transcriptions', b''.join(parts),
                          'multipart/form-data; boundary=' + boundary, key)
    if not result.get('words') or not result.get('text'):
        raise RuntimeError('Full transcription returned no word timestamps')
    result['audio_sha256'] = digest
    save(stamp, result)
    return result


def normalize(text):
    text = text.casefold().replace('’', "'")
    for pattern, replacement in [(r'шортс', 'shorts'), (r'блокер', 'blocker'),
                                 (r'кід[сз]', 'kids'), (r'ютуб', 'youtube')]:
        text = re.sub(pattern, replacement, text)
    return re.findall(r'[^\W_]+', text, flags=re.UNICODE)


def align(script, transcript, duration):
    reference, units, paragraph_first = [], [], []
    for paragraph in script.split('\n\n'):
        paragraph_first.append(len(units))
        for word in paragraph.split():
            tokens = normalize(word)
            units.append({'text': word, 'token_first': len(reference), 'token_last': len(reference) + len(tokens) - 1})
            reference.extend(tokens)
    heard, heard_times = [], []
    for word in transcript['words']:
        for token in normalize(word['word']):
            heard.append(token)
            heard_times.append((float(word['start']), float(word['end'])))
    matcher = difflib.SequenceMatcher(None, reference, heard, autojunk=False)
    ratio = matcher.ratio()
    if ratio < .94:
        raise RuntimeError(f'Transcript differs from script: similarity {ratio:.4f}')
    mapping = {}
    for block in matcher.get_matching_blocks():
        mapping.update({block.a + offset: block.b + offset for offset in range(block.size)})
    anchors = sorted(mapping)
    for unit in units:
        indexes = [mapping[t] for t in range(unit['token_first'], unit['token_last'] + 1) if t in mapping]
        if indexes:
            unit['start'], unit['end'] = heard_times[indexes[0]][0], heard_times[indexes[-1]][1]
        else:
            before = [t for t in anchors if t < unit['token_first']]
            after = [t for t in anchors if t > unit['token_last']]
            left = heard_times[mapping[before[-1]]][1] if before else 0.0
            right = heard_times[mapping[after[0]]][0] if after else duration
            unit['start'], unit['end'] = left, max(left + .05, right)
        del unit['token_first'], unit['token_last']
    starts = [units[index]['start'] for index in paragraph_first]
    starts[0] = 0.0
    return {'similarity': ratio, 'paragraph_starts': starts, 'seconds': duration,
            'words_per_minute': len(units) * 60 / duration, 'words': units,
            'transcribed_text': transcript['text']}


def generate_and_align():
    import add_openai_narration as existing
    key = os.environ.get('OPENAI_API_KEY', '').strip()
    if not key:
        raise RuntimeError('OpenAI key missing from this process')
    plan = json.loads((WORK / 'plan.json').read_text(encoding='utf-8-sig'))
    if plan['model'] != 'gpt-4o-mini-tts' or plan['voice'] != 'cedar':
        raise RuntimeError('Unexpected speech configuration')
    existing.AUDIO = WORK / 'audio'
    existing.AUDIO.mkdir(exist_ok=True)
    cue = {'text_' + lang: plan['scripts'][lang] for lang in ('en', 'uk')}
    for text in cue.values():
        if not 100 < len(text) <= 4096:
            raise RuntimeError('Narration must fit a single TTS request')
    for lang in ('en', 'uk'):
        existing.speech((lang, 1, cue, plan, key))
        source, target = existing.AUDIO / f'{lang}-01.wav', WORK / f'voiceover-{lang}.wav'
        edits = prepare_wave(source, target)
        save(WORK / f'audio-edits-{lang}.json', edits)
        print(f'{lang}: continuous OpenAI take ready; {edits["seconds"]:.2f}s', flush=True)
        transcript = transcribe(target, lang, key)
        aligned = align(plan['scripts'][lang], transcript, edits['seconds'])
        save(WORK / f'alignment-{lang}.json', aligned)
        print(json.dumps({'language': lang, 'duration': round(edits['seconds'], 2),
                          'words_per_minute': round(aligned['words_per_minute'], 1),
                          'similarity': round(aligned['similarity'], 4),
                          'paragraph_starts': aligned['paragraph_starts']}, ensure_ascii=False), flush=True)
