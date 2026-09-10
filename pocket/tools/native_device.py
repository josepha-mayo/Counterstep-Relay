"""Execute the actual AndroidJUnitRunner and retain raw outcomes before uninstall cleanup."""
from pathlib import Path
import hashlib,json,re,subprocess,time
R=Path(__file__).resolve().parents[2];O=R/'device-out';O.mkdir(exist_ok=True)
PKG='dev.joseph.countersteppocket'
def adb(*args,timeout=30):
    return subprocess.check_output(['adb',*args],text=True,timeout=timeout)
def save(name,data):(O/name).write_text(json.dumps(data,indent=2)+'\n')
def parse(raw):
    bundle={};starts=[];outcomes=[];final=[]
    for line in raw.replace('\r','').splitlines():
        if line.startswith('INSTRUMENTATION_STATUS: '):
            pair=line[len('INSTRUMENTATION_STATUS: '):].split('=',1)
            if len(pair)==2:bundle[pair[0]]=pair[1]
        elif line.startswith('INSTRUMENTATION_STATUS_CODE: '):
            code=int(line.split(':',1)[1]);identity=(bundle.get('class'),bundle.get('test'))
            if code==1:starts.append(identity)
            elif all(identity):outcomes.append({'class':identity[0],'test':identity[1],'code':code,'declared_tests':int(bundle.get('numtests',0))})
            elif code!=2:raise AssertionError('Unattributed instrumentation result: '+str(code))
            bundle={}
        elif line.startswith('INSTRUMENTATION_CODE: '):final.append(int(line.split(':',1)[1]))
    keys=[(o['class'],o['test'])for o in outcomes]
    assert final==[-1],final
    assert len(starts)==len(set(starts))==len(outcomes)==len(set(keys))==19,(starts,outcomes)
    assert set(starts)==set(keys)
    assert all(o['declared_tests']==19 for o in outcomes),outcomes
    counts={'tests':len(outcomes),'failures':sum(o['code']!=0 for o in outcomes),'errors':sum(o['code']==-1 for o in outcomes),'skipped':sum(o['code']in[-3,-4]for o in outcomes)}
    return counts,outcomes
if __name__=='__main__':
    # The earlier run never obtained focused windows. Wake this disposable emulator explicitly.
    adb('shell','svc','power','stayon','true')
    adb('shell','settings','put','system','screen_off_timeout','2147483647')
    adb('shell','input','keyevent','KEYCODE_WAKEUP')
    adb('shell','wm','dismiss-keyguard')
    adb('shell','input','keyevent','KEYCODE_HOME')
    (O/'power-before-tests.txt').write_text(adb('shell','dumpsys','power'))
    (O/'window-before-tests.txt').write_text(adb('shell','dumpsys','window'))
    for relative in ['pocket/app/build/outputs/apk/debug/app-debug.apk','pocket/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']:
        text=adb('install','-r',str(R/relative),timeout=90);assert 'Success'in text,text
    adb('shell','am','start','-W','-n',PKG+'/.MainActivity');time.sleep(1)
    adb('shell','wm','dismiss-keyguard')
    (O/'before-tests.png').write_bytes(subprocess.check_output(['adb','exec-out','screencap','-p'],timeout=20))
    report={'status':'running','runner':'adb shell am instrument -w -r / AndroidJUnitRunner','scope':'The unchanged 19 native instrumentation test methods; no provider credentials or transaction.'}
    try:
        result=subprocess.run(['adb','shell','am','instrument','-w','-r','-e','class',PKG+'.DeviceWorkflowTest',PKG+'.test/androidx.test.runner.AndroidJUnitRunner'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=480)
        raw=result.stdout;(O/'instrumentation-raw.log').write_text(raw);print(raw,flush=True)
        counts,outcomes=parse(raw)
        report.update(tests=counts,outcomes=outcomes,process_exit_code=result.returncode,raw_sha256=hashlib.sha256(raw.encode()).hexdigest())
        assert result.returncode==0 and counts['failures']==0 and counts['skipped']==0,counts
        assert re.search(r'OK \(19 tests\)',raw), 'Missing JUnit completion summary'
        report['status']='passed'
    except BaseException as e:
        report.update(status='failed',error=str(e));raise
    finally:
        save('instrumented-tests.json',report)
        (O/'window-after-tests.txt').write_text(adb('shell','dumpsys','window'))
        (O/'power-after-tests.txt').write_text(adb('shell','dumpsys','power'))
