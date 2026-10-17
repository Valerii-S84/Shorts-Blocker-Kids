"""Assess complete generated soundtracks; this is automated, not human listening."""
import base64
import hashlib
import json
import os
from pathlib import Path

from natural_review import WORK, request_json, save


def main():
    key = os.environ.get('OPENAI_API_KEY', '').strip()
    if not key:
        raise RuntimeError('Windows user OpenAI key unavailable')
    plan = json.loads((WORK / 'plan.json').read_text(encoding='utf-8-sig'))
    for lang in ['en', 'uk']:
        path = WORK / f'voiceover-{lang}.wav'
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        destination = WORK / f'full-audio-review-{lang}.json'
        if destination.exists():
            previous = json.loads(destination.read_text(encoding='utf-8'))
            if previous.get('audio_sha256') == digest:
                print(lang + ': full review already exists', flush=True)
                continue
            raise RuntimeError('Soundtrack changed after review')
        duration = json.loads((WORK / f'audio-edits-{lang}.json').read_text())['seconds']
        instruction = (
            f'Listen critically to this COMPLETE {lang} product-demo narration, duration {duration:.2f} seconds. '
            'The user rejected earlier sleepy, artificial narration and long silent gaps. '
            'Evaluate actual audible sound from beginning to end, not technical format or a short sample. '
            'Judge natural conversational rhythm, syllable stretching, intelligibility, pronunciation, '
            'uncomfortable rushing, robotic delivery, level changes, abrupt edits, clicks and long pauses. '
            'Do not reward high energy or a smiling sales pitch; the target is ordinary clear friendly explanation. '
            'Ukrainian has longer words than English; do not label it slow based on word count alone. '
            'Compare what is actually spoken to the script, including the final sentence. '
            'Return JSON only: language, verdict (acceptable or needs_edit), naturalness_1_to_10, '
            'intelligibility_1_to_10, pace (natural/too_slow/too_fast/variable), '
            'coverage (one item per 20-second interval with interval and a phrase actually heard), '
            'defects (timestamp, severity, description), omitted_or_changed_phrases, '
            'recommended_uniform_tempo (1.0 if suitable, otherwise one value from 0.94 to 1.12), '
            'brief_reason. No claims of human listening or guaranteed listener satisfaction. Script:\n'
            + plan['scripts'][lang])
        payload = {'model': 'gpt-audio-1.5', 'modalities': ['text'],
            'messages': [{'role': 'user', 'content': [
                {'type': 'text', 'text': instruction},
                {'type': 'input_audio', 'input_audio': {'format': 'wav',
                    'data': base64.b64encode(path.read_bytes()).decode('ascii')}}]}],
            'max_completion_tokens': 2200}
        response = request_json('chat/completions', json.dumps(payload).encode(),
                                'application/json', key, timeout=300)
        raw = response['choices'][0]['message']['content'].strip()
        if raw.startswith('```'):
            raw = raw.split('\n', 1)[1].rsplit('```', 1)[0].strip()
        assessment = json.loads(raw)
        assessment['audio_sha256'] = digest
        assessment['review_method'] = 'Automated full audio listening via gpt-audio-1.5; not human review'
        assessment['duration_seconds'] = duration
        save(destination, assessment)
        print(json.dumps(assessment, ensure_ascii=True), flush=True)


if __name__ == '__main__':
    main()
