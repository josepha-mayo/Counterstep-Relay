"""Package an already tested Pocket artifact without rebuilding or changing app code."""
import hashlib, io, json, sys, zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

COMMIT = "adfa48a8cfbe95a326fdef36a0091ab59a88005c"
RUN = "34495327720"
ZIP_SHA = "d432100ffce02ea5701fc8cd3b926c9102ccf7ca43a368b1c3d3481b7b9675fd"
APK_SHA = "2d74fcea7285ce5e6425a44e62f03080c80d1eacddce44bc23b1891685cd7918"
SRC_SHA = "dfd5eebdc9159e59805b2380cdee38bd550b8a4e55d7740e8c483516917a5937"
def sha(b): return hashlib.sha256(b).hexdigest()
def require(ok, message):
    if not ok: raise ValueError(message)

def prepare(path, dest):
    raw=path.read_bytes()
    require(sha(raw)==ZIP_SHA, "Original artifact checksum mismatch")
    z=zipfile.ZipFile(io.BytesIO(raw))
    require(z.testzip() is None, "Corrupt original artifact")
    v=json.loads(z.read("device-out/verification.json"))
    require(v["status"]=="passed" and v["source_commit"]==COMMIT and str(v["workflow_run"])==RUN, "Wrong build record")
    require(v["purchase_executed"] is False and v["test_store_configured"] is False, "Unexpected billing evidence")
    total={k:0 for k in ("tests","failures","errors","skipped")}
    for name in z.namelist():
        if "/test-results/" in name and name.endswith(".xml"):
            root=ET.fromstring(z.read(name))
            for k in total: total[k]+=int(root.attrib.get(k,0))
    require(total=={"tests":98,"failures":0,"errors":0,"skipped":0}, "Unit result mismatch")
    instrumented=json.loads(z.read("device-out/instrumented-tests.json"))
    require(instrumented["status"]=="passed" and instrumented["tests"]=={"tests":19,"failures":0,"errors":0,"skipped":0}, "Device result mismatch")
    require(v["process_restart"]["status"]=="passed", "Restart check did not pass")
    apk=z.read("device-out/Counterstep-Pocket-0.3-preview.apk")
    source=z.read("device-out/Counterstep-Pocket-source.zip")
    require(sha(apk)==APK_SHA and v["apk_sha256"]==APK_SHA and sha(source)==SRC_SHA, "Release payload mismatch")
    zs=zipfile.ZipFile(io.BytesIO(source))
    require(zs.testzip() is None, "Corrupt source archive")
    for name in zs.namelist():
        p=Path(name)
        require(not p.is_absolute() and ".." not in p.parts, "Unsafe source path")
        require(p.suffix.lower() not in (".ttf",".otf",".woff",".woff2",".ttc",".jks",".keystore",".env"), "Unapproved bundled file")
    report={
      "status":"verified_existing_artifact",
      "build_source_commit":COMMIT, "original_build_run":RUN,
      "original_artifact_id":10159811385, "original_artifact_sha256":ZIP_SHA,
      "apk_sha256":APK_SHA, "source_zip_sha256":SRC_SHA,
      "historical_jvm_tests":total, "historical_android_tests":instrumented["tests"],
      "historical_process_restart":"passed",
      "release_packaging_date":"2026-09-15",
      "app_rebuilt":False, "new_device_tests_executed":False,
      "real_revenuecat_purchase_executed":False,
      "device":"Android API 35 emulator, not physical Redmi hardware",
      "scope":"Public delivery of the existing tested debug preview, not a new app build, production billing, or a final competition submission."
    }
    setup="""# Pocket 0.3 preview: run and test
This is a developer/debug preview, not a Play Store release.
Install on a compatible Android 8.0+ device. Download only from this release and compare SHA-256 first. An earlier runner's debug signature may block an in-place update. Preserve practice notes before changing installations; do not delete data to force an update.

Free task:
1. Open the app. For the initial 2(x+3) task, try 2x+3, check it, request a hint, then enter 2x+6.
2. Confirm that the original wrong response and hint remain in history.
3. Enter an unfinished draft, close/reopen the app and confirm it is retained.
4. Share a practice note only to a destination you choose.

RevenueCat setup, required for the next integration test:
1. Use an owned RevenueCat project and its Test Store.
2. Create an entitlement with identifier mixed_signs.
3. Create a Test Store product and attach it to mixed_signs. The product identifier may be your chosen value; the app does not hardcode it.
4. In the current offering, configure exactly one custom package named mixed_signs containing that product.
5. Open Mixed-sign practice pack, explicitly connect with the public test_ SDK key, and inspect the displayed package.
6. Test cancellation and verify no access is granted. Then make a Test Store purchase, refresh access, restart/reconnect if needed, and restore. Record expected versus actual results. Do not label this executed until the SDK returns real Test Store results.

The key is process-local. A cold restart may require reconnecting with the same public Test Store key. Do not share private API keys, customer identifiers, or transaction tokens in source or screenshots. No production charges are authorized by this file. Real Store integration and student verification are still unfinished.

Source contracts:
https://github.com/josepha-mayo/Counterstep-Relay/blob/adfa48a8cfbe95a326fdef36a0091ab59a88005c/pocket/app/src/main/java/dev/joseph/countersteppocket/MainActivity.kt
https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store
"""
    dest.mkdir(parents=True,exist_ok=False)
    for name,data in {
      "Counterstep-Pocket-0.3-preview.apk":apk,
      "Counterstep-Pocket-0.3-source.zip":source,
      "VERIFICATION.json":(json.dumps(report,indent=2)+"\n").encode(),
      "TEST-STORE-SETUP.md":setup.encode(),
      "LICENSE.txt":zs.read("pocket/LICENSE")
    }.items(): (dest/name).write_bytes(data)
    sums="\n".join(sha(p.read_bytes())+"  "+p.name for p in sorted(dest.iterdir()))+"\n"
    (dest/"SHA256SUMS.txt").write_text(sums)
    notes="""## Counterstep Pocket 0.3 developer preview

Pinned app source: `adfa48a8cfbe95a326fdef36a0091ab59a88005c`.

Download the APK and matching source from the assets below. Check `SHA256SUMS.txt`; follow `TEST-STORE-SETUP.md`.

This packages the original successful [Android build 34495327720](https://github.com/josepha-mayo/Counterstep-Relay/actions/runs/34495327720). Its historical reports contain 98 passing JVM tests, 19 passing Android emulator tests and restart/draft persistence. They were rechecked for packaging, not rerun as new tests. The APK bytes are unchanged.

Free practice, hints and saved work function without a store. The optional RevenueCat path is still awaiting actual owned Test Store purchase/cancel/refresh/restore evidence. No store key, real transaction, revenue, learning benefit, physical-device validation or completed competition submission is claimed.

Debug preview only. Do not publish this Test Store development build to an app store. This prerelease is not the Amazon Relay product release and does not modify other submitted apps.
"""
    return report, notes

if __name__=="__main__":
    report,notes=prepare(Path(sys.argv[1]),Path(sys.argv[2]))
    Path(sys.argv[3]).write_text(notes)
    print(json.dumps(report,indent=2))
