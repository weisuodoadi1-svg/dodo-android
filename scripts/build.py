#!/usr/bin/env python3
"""Build the published overlay against the exact supported base APK."""
from pathlib import Path
import argparse, hashlib, json, os, re, shutil, struct, subprocess, urllib.request, zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', ANDROID[1:-1])

def run(*args):
    subprocess.run([str(x) for x in args], check=True)

def setup():
    sdk = Path(os.environ['ANDROID_HOME'])
    java_home = os.environ.get('JAVA_HOME')
    java = Path(java_home) / 'bin/java' if java_home else Path(shutil.which('java') or '')
    javac = Path(java_home) / 'bin/javac' if java_home else Path(shutil.which('javac') or '')
    bt = sdk / 'build-tools/35.0.0'
    android = sdk / 'platforms/android-35/android.jar'
    for p in [java, javac, bt/'lib/d8.jar', bt/'lib/apksigner.jar', android]:
        if not p.is_file():
            raise SystemExit(f'Missing build dependency: {p}')
    tools = ROOT / '.tools'
    tools.mkdir(exist_ok=True)
    for name, spec in json.loads((ROOT/'tools.lock.json').read_text()).items():
        dest = tools / name
        if not dest.exists():
            with urllib.request.urlopen(spec['url'], timeout=90) as response:
                data = response.read()
            if hashlib.sha256(data).hexdigest() != spec['sha256']:
                raise SystemExit(f'Download checksum mismatch: {name}')
            dest.write_bytes(data)
        if hashlib.sha256(dest.read_bytes()).hexdigest() != spec['sha256']:
            raise SystemExit(f'Tool checksum mismatch: {name}')
    return java, javac, bt, android, tools

def prepare(base, out, tools, java):
    version = json.loads((ROOT/'version.json').read_text())
    raw = base.read_bytes()
    if hashlib.sha256(raw).hexdigest() != version['base_sha256']:
        raise SystemExit('Unsupported base APK. See version.json for the required SHA-256.')
    run(java, '-jar', tools/'apktool.jar', 'd', '-f', base, '-o', out)
    shutil.copytree(ROOT/'res', out/'res', dirs_exist_ok=True)
    shutil.copytree(ROOT/'assets', out/'assets', dirs_exist_ok=True)
    shutil.copyfile(ROOT/'THIRD_PARTY_NOTICES.txt', out/'assets/local-zft-licenses.txt')
    for relative, patches in json.loads((ROOT/'resource-patches.json').read_text()).items():
        path = out / relative
        tree = ET.parse(path)
        root = tree.getroot()
        for patch in patches:
            replacement = ET.fromstring(patch)
            for existing in list(root):
                if existing.tag == replacement.tag and existing.get('name') == replacement.get('name'):
                    root.remove(existing)
            root.append(replacement)
        tree.write(path, encoding='utf-8', xml_declaration=True)
    manifest = out/'AndroidManifest.xml'
    tree = ET.parse(manifest)
    app = tree.getroot().find('application')
    app.set(ANDROID+'icon', '@drawable/dodo_owner')
    app.set(ANDROID+'roundIcon', '@drawable/dodo_owner')
    for activity in app.findall('activity'):
        if activity.get(ANDROID+'name') == 'com.zhongfutong.main.WelcomeView':
            activity.set(ANDROID+'theme', '@style/DodoLaunchTheme')
    for name in ['AiActivity', 'StockActivity', 'NativeChat']:
        attributes = {ANDROID+'name': 'local.zft.'+name, ANDROID+'exported': 'false', ANDROID+'theme': '@android:style/Theme.Material.Light.NoActionBar'}
        if name == 'NativeChat':
            attributes[ANDROID+'windowSoftInputMode'] = 'adjustResize'
        ET.SubElement(app, 'activity', attributes)
    tree.write(manifest, encoding='utf-8', xml_declaration=True)
    config = out/'apktool.yml'
    s = config.read_text()
    s = re.sub(r'(?m)^  versionCode:.*$', '  versionCode: '+str(version['code']), s)
    s = re.sub(r'(?m)^  versionName:.*$', '  versionName: '+version['name'], s)
    config.write_text(s)
    # Keep only the base APK's public signing block, required to decode its unchanged payload.
    eocd = raw.rfind(b'PK\x05\x06')
    central = struct.unpack_from('<I', raw, eocd+16)[0]
    if raw[central-16:central] != b'APK Sig Block 42':
        raise SystemExit('Base APK signing block not found')
    size = struct.unpack_from('<Q', raw, central-24)[0]
    if size < 24 or size+8 > central:
        raise SystemExit('Invalid signing block size')
    block = raw[central-size-8:central]
    carrier = block + struct.pack('<4s4H2IH', b'PK\x05\x06', 0, 0, 0, 0, 0, len(block), 0)
    (out/'assets/local-zft-signer.apk').write_bytes(carrier)
    counts = {'boot': 0, 'verify': 0, 'install': 0}
    for name in ['ProxyApplication.smali', 'ProxyComponentFactory.smali', 'ProxyComponentFactory$FileBootstrap.smali']:
        p = out/'smali/com/yqsh/protector/shell'/name
        s = p.read_text()
        s, n = re.subn(r'Lcom/yqsh/protector/shell/JniBridge;->initApp\(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;\)V', r'Llocal/zft/ChatHome;->boot(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V', s)
        counts['boot'] += n
        s, n = re.subn(r'    invoke-static \{p0\}, Lcom/yqsh/protector/shell/JniBridge;->verifySignature\(Landroid/content/Context;\)V', '    nop', s)
        counts['verify'] += n
        if name == 'ProxyApplication.smali':
            target = '    invoke-virtual {p1}, Landroid/app/Application;->onCreate()V'
            if s.count(target) != 1:
                raise SystemExit('Unexpected application entry point')
            s = s.replace(target, target+'\n\n    invoke-static {p1}, Llocal/zft/ChatHome;->install(Landroid/app/Application;)V')
            counts['install'] += 1
        p.write_text(s)
    if counts != {'boot': 2, 'verify': 3, 'install': 1}:
        raise SystemExit(f'Unexpected base shell structure: {counts}')
    return version

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base-apk', required=True, type=Path)
    parser.add_argument('--keystore', required=True, type=Path)
    parser.add_argument('--alias', required=True)
    args = parser.parse_args()
    if not args.keystore.is_file() or 'DODO_STORE_PASSWORD' not in os.environ:
        parser.error('A local keystore and DODO_STORE_PASSWORD environment variable are required')
    java, javac, bt, android, tools = setup()
    build = ROOT/'build'
    if build.exists():
        shutil.rmtree(build)
    build.mkdir()
    decoded = build/'decoded'
    version = prepare(args.base_apk.resolve(), decoded, tools, java)
    classes, dex = build/'classes', build/'dex'
    classes.mkdir(); dex.mkdir()
    asm = os.pathsep.join(str(tools/n) for n in ['asm.jar', 'asm-commons.jar'])
    run(javac, '-cp', asm, '-d', build, ROOT/'scripts/Relocate.java')
    deps = build/'chat-deps.jar'
    run(java, '-cp', str(build)+os.pathsep+asm, 'Relocate', deps, tools/'slf4j.jar', tools/'websocket.jar')
    run(javac, '-source', '8', '-target', '8', '-cp', str(android)+os.pathsep+str(deps), '-d', classes, *sorted((ROOT/'src').rglob('*.java')))
    run(java, '-cp', bt/'lib/d8.jar', 'com.android.tools.r8.D8', '--min-api', '23', '--lib', android, '--output', dex, *sorted(classes.rglob('*.class')), deps)
    unsigned, aligned = build/'unsigned.apk', build/'aligned.apk'
    run(java, '-jar', tools/'apktool.jar', 'b', decoded, '-o', unsigned)
    with zipfile.ZipFile(unsigned, 'a', zipfile.ZIP_DEFLATED) as z:
        if 'classes3.dex' in z.namelist():
            raise SystemExit('Base APK already contains classes3.dex')
        z.write(dex/'classes.dex', 'classes3.dex')
    run(bt/'zipalign', '-f', '-P', '16', '4', unsigned, aligned)
    dist = ROOT/'dist'; dist.mkdir(exist_ok=True)
    output = dist/f"dodo-{version['name']}.apk"
    run(java, '-jar', bt/'lib/apksigner.jar', 'sign', '--ks', args.keystore.resolve(), '--ks-key-alias', args.alias, '--ks-pass', 'env:DODO_STORE_PASSWORD', '--out', output, aligned)
    run(java, '-jar', bt/'lib/apksigner.jar', 'verify', output)
    (dist/'SHA256SUMS').write_text(hashlib.sha256(output.read_bytes()).hexdigest()+'  '+output.name+'\n')
    print(output)

if __name__ == '__main__':
    main()
