"""Record native builds/tests separately. No store calls, invented passes, or cloud deployment."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,sys,time,zipfile
import xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[2];O=R/'device-out';O.mkdir(exist_ok=True)
def counts(root):
    files=list((R/root).rglob('TEST-*.xml'));assert files,root
    out={k:sum(int(ET.parse(p).getroot().attrib.get(k,0))for p in files)for k in ['tests','failures','errors','skipped']}
    assert not any(out[k]for k in ['failures','errors','skipped']),out
    return out
def save(name,value):(O/name).write_text(json.dumps(value,indent=2)+'\n')
def adb(*args):return subprocess.check_output(['adb',*args],text=True,timeout=20)
def tree():
    adb('shell','uiautomator','dump','/sdcard/pocket-ui.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/pocket-ui.xml'))
mode=sys.argv[1]
if mode=='build':
    jvm=counts('pocket/app/build/test-results/testDebugUnitTest');assert jvm['tests']==98,jvm
    apk=R/'pocket/app/build/outputs/apk/debug/app-debug.apk';shutil.copy2(apk,O/'Counterstep-Pocket-0.3-preview.apk')
    with zipfile.ZipFile(O/'Counterstep-Pocket-source.zip','w',zipfile.ZIP_DEFLATED)as z:
        for p in (R/'pocket').rglob('*'):
            if p.is_file()and'build'not in p.parts and'.gradle'not in p.parts and p.name!='local.properties'and p.suffix.lower()not in {'.ttf','.otf','.woff','.woff2','.ttc','.pyc'}:z.write(p,p.relative_to(R))
    save('build-verification.json',{'status':'build_and_jvm_passed','source_commit':os.environ['GITHUB_SHA'],'workflow_run':os.environ['GITHUB_RUN_ID'],'jvm_tests':jvm,'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'device_validation':'separate later step','purchase_executed':False})
elif mode=='restart':
    pkg='dev.joseph.countersteppocket'
    # Only the disposable emulator's fixture app data is reset.
    adb('shell','pm','clear',pkg);adb('shell','am','start','-W','-n',pkg+'/.MainActivity');time.sleep(1)
    node=next(n for n in tree().iter('node')if n.attrib.get('class')=='android.widget.EditText')
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));adb('shell','input','text','2x+');adb('shell','input','keyevent','4');time.sleep(.5)
    assert any(n.attrib.get('text')=='2x+'for n in tree().iter('node'))
    old=adb('shell','pidof',pkg).strip();assert old
    adb('shell','am','force-stop',pkg);adb('shell','am','start','-W','-n',pkg+'/.MainActivity');time.sleep(.5)
    new=adb('shell','pidof',pkg).strip();assert new and new!=old
    after=tree();assert any(n.attrib.get('text')=='2x+'for n in after.iter('node'))
    (O/'restarted-draft-screen.png').write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=20))
    # The work record may be below the fold. Inspect it through actual swipes, not a hidden-view assertion.
    for _ in range(4):
        if any(n.attrib.get('text')=='No response yet.'for n in after.iter('node')):break
        adb('shell','input','swipe','540','1500','540','500','350');time.sleep(.2);after=tree()
    assert any(n.attrib.get('text')=='No response yet.'for n in after.iter('node'))
    save('process-restart.json',{'status':'passed','old_process':old,'new_process':new,'draft_preserved':'2x+','no_attempt_invented':True})
    (O/'restarted-ui.xml').write_bytes(ET.tostring(after));(O/'restarted-screen.png').write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=20))
elif mode=='finish':
    jvm=counts('pocket/app/build/test-results/testDebugUnitTest');native=json.loads((O/'instrumented-tests.json').read_text());assert native['status']=='passed';device=native['tests'];assert jvm['tests']==98 and device['tests']==19 and not any(device[k]for k in ['failures','errors','skipped']),(jvm,device)
    report=json.loads((O/'build-verification.json').read_text());report.update(status='passed',android_instrumented_tests=device,process_restart=json.loads((O/'process-restart.json').read_text()),android_api=35,device='Android emulator, not physical Redmi hardware',test_store_configured=False,purchase_executed=False,scope='Native Android repair, draft, scrolling, lifecycle, export-intent and unconfigured-store checks; 98 JVM scenarios include billing state/expiry cases, not real SDK purchases. No learning-outcome or payment-success claim.')
    save('verification.json',report)
else:raise SystemExit('Expected build, restart or finish')
