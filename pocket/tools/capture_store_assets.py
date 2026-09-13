"""Capture real emulator UI at the required screenshot viewport. No purchase is simulated."""
from pathlib import Path
import json, os, re, subprocess, time
import xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[2];O=R/'device-out/submission-screens';O.mkdir(parents=True,exist_ok=True)
PKG='dev.joseph.countersteppocket'
def adb(*args): return subprocess.check_output(['adb',*args],text=True,timeout=30)
def tree():
    adb('shell','uiautomator','dump','/sdcard/pocket-assets.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/pocket-assets.xml'))
def tap_node(n):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.attrib['bounds']))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.3)
def tap_text(text):
    for _ in range(8):
        found=next((n for n in tree().iter('node') if n.attrib.get('text')==text and n.attrib.get('clickable')=='true'),None)
        if found is not None:tap_node(found);return
        adb('shell','input','swipe','580','2000','580','650','300');time.sleep(.25)
    raise AssertionError('Visible control not found: '+text)
def shot(name):
    data=subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=20)
    assert data[:8]==b'\x89PNG\r\n\x1a\n'
    w=int.from_bytes(data[16:20],'big');h=int.from_bytes(data[20:24],'big');assert (w,h)==(1179,2556),(w,h)
    (O/name).write_bytes(data)
adb('shell','wm','size','1179x2556');adb('shell','wm','density','420')
adb('shell','pm','clear',PKG);adb('shell','am','start','-W','-n',PKG+'/.MainActivity');time.sleep(1)
assert any(n.attrib.get('text')=='2(x+3)' for n in tree().iter('node'));shot('01-free-practice.png')
n=next(n for n in tree().iter('node') if n.attrib.get('class')=='android.widget.EditText');tap_node(n)
adb('shell','input','text','2x+3');adb('shell','input','keyevent','4');time.sleep(.4)
tap_text('Check my step');shot('02-correct-the-constant.png')
tap_text('Test Store checks and setup')
assert any(n.attrib.get('text')=='No Test Store connected. Free practice works without it.' for n in tree().iter('node'))
shot('03-store-checks-unconfigured.png')
(O/'capture.json').write_text(json.dumps({'source_commit':os.environ['GITHUB_SHA'],'device':'Android API35 emulator, not a physical phone','width':1179,'height':2556,'app_generated_ui':True,'screenshots_edited_or_stretched':False,'store_connected':False,'purchase_executed':False},indent=2))
print('Captured three actual 1179x2556 emulator screenshots.')
