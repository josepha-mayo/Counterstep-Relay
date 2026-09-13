# Counterstep Pocket 0.4 preview

Write the expanded expression, inspect feedback on the incorrect term, and retain the first attempt, hints and later repair. Free practice, hints, saving and sharing stay free and work without connecting a store. This is a separate native Android prototype; it does not replace the Amazon Relay or Prom browser entry.

## This revision

The optional **Test Store checks and setup** panel explains the required `mixed_signs` entitlement, package and current offering. It provides separate current-access and offering checks, a sanitized diagnostic export, and removal of a remembered public Test Store key. It remains a developer test interface, not a paid production offer.

Store setup is explicit. Remembering its public `test_` SDK key on this device is unchecked by default; opting in permits reconnecting after a process restart. No private key is accepted or bundled. Forgetting the saved key does not erase RevenueCat history, cancel existing requests, or destroy the in-memory SDK connection. Core practice never depends on a store being configured.

The bounded diagnostic contains only enum events and relative time, not keys, customer/transaction IDs, raw provider error strings or typed practice. Sharing opens Android's chooser, not an automatic send. It is an editable local observation, not a signed receipt or evidence of payment. A Test Store current-access/restore response is not proof of Google Play or cross-account restore.

The original checker, coaching, draft and billing-gate code and all 98 JVM / 19 Android baseline test methods remain intact. Ten JVM diagnostic tests and six native setup/export tests are added. The new test/check entry shares a compact secondary-action row with practice-note sharing, preserving large touch targets.

## Build and run the native checks

JDK 17, Android SDK 35 and Gradle 8.13. From `pocket/`:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
gradle --no-daemon :app:connectedDebugAndroidTest
```

The isolated workflow is `.github/workflows/pocket-store-check.yml` on `release/pocket-store-20260913`. It compiles the actual app, runs the 108 JVM and 25 Android test methods, checks process-stop/reopen draft persistence, and captures real emulator pixels at 1179 x 2556. Read that exact run's `verification.json` for results; a planned count is not a passing result. No key, real SDK purchase response or customer is fabricated by these tests.

The initial 0.4 run, 34761132710 at d67b07780b163317eb9af5cf47ff7bbe91f618b6, compiled and passed all 108 JVM tests. It passed the 19 retained native tests and two new tests, but four new tests could not reach the lower setup button. Its failed evidence remains preserved. The following layout correction compacts the secondary controls and reruns the same assertions. It must not be called a verified correction until the later run actually passes.

## Complete the owned Test Store check

See [the setup and evidence instructions](docs/STORE-CHECKS.md). An actual owned public key and configured product/offering are still required. The app uses RevenueCat Android SDK 9.9.0. The source/APK includes no key. SDK-backed test purchase, cancellation/failure and entitlement refresh/restart remain separate from the no-account automated tests.

The debug preview is for testing, not Google Play release. A previous debug APK signed by another runner may reject an in-place update. Preserve practice notes before any uninstall; do not erase user data automatically.

## Competition state and provenance

The RevenueCat project draft exists, but final submission, student verification, an actual Test Store demonstration and required final assets remain separate tasks. No production transaction, app-store release, physical-phone test, learning gain or award is claimed.

Original work by Joseph Ayanda, with substantial AI development assistance, under the MIT license. Native views and a narrow integer-linear checker, not OCR or a conversational model. Other submitted projects and production branches are unchanged.

Official SDK references:
- https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store
- https://www.revenuecat.com/docs/customers/identifying-customers
- https://www.revenuecat.com/docs/getting-started/restoring-purchases
