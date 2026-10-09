#!/usr/bin/env python3
"""Offline structural smoke checks. Not an Android build or real platform verification."""
from pathlib import Path
from xml.etree import ElementTree as ET
root = Path(__file__).resolve().parents[1]
a = '{http://schemas.android.com/apk/res/android}'
m = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
filters = m.find('./application/activity').findall('intent-filter')
found = {(act.get(a+'name'), d.get(a+'mimeType')) for f in filters for act in f.findall('action') for d in f.findall('data')}
for pair in [('android.intent.action.SEND','text/plain'),('android.intent.action.SEND','image/*'),('android.intent.action.SEND_MULTIPLE','image/*')]:
    assert pair in found, pair
s = (root/'app/src/main/java/dev/onetap/images/download/MediaStoreSaver.kt').read_text()
for keyword in ('IS_PENDING','RELATIVE_PATH','decodeFile','resolver.delete'):
    assert keyword in s, keyword
assert (root/'.github/workflows/android.yml').is_file()
print('PASS: structural smoke checks')
