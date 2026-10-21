#!/usr/bin/env python3
from pathlib import Path
import argparse, os, shutil, zipfile
from build import ROOT, setup, run

p = argparse.ArgumentParser()
p.add_argument('--keystore', required=True, type=Path)
p.add_argument('--alias', required=True)
a = p.parse_args()
if not a.keystore.is_file() or 'DODO_STORE_PASSWORD' not in os.environ:
    p.error('Local keystore and DODO_STORE_PASSWORD required')
java, javac, bt, android, tools = setup()
work = ROOT/'build/android-tests'
if work.exists():
    shutil.rmtree(work)
classes, dex = work/'classes', work/'dex'
classes.mkdir(parents=True); dex.mkdir()
run(javac, '-source', '8', '-target', '8', '-cp', android, '-d', classes, *sorted((ROOT/'tests/android').glob('*.java')))
run(java, '-cp', bt/'lib/d8.jar', 'com.android.tools.r8.D8', '--lib', android, '--output', dex, *sorted(classes.rglob('*.class')))
apk = work/'tests.apk'
run(bt/'aapt', 'package', '-f', '-M', ROOT/'tests/android/AndroidManifest.xml', '-I', android, '-F', apk)
with zipfile.ZipFile(apk, 'a') as z:
    z.write(dex/'classes.dex', 'classes.dex')
run(java, '-jar', bt/'lib/apksigner.jar', 'sign', '--ks', a.keystore.resolve(), '--ks-key-alias', a.alias, '--ks-pass', 'env:DODO_STORE_PASSWORD', apk)
print(apk)
