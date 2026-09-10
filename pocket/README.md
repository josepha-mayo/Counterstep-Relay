# Counterstep Pocket 0.3 preview

A separate native Android repair-to-practice app. It does not replace the Amazon Counterstep Relay or Prom entry. The integer-linear checker supports a narrow expansion task, not general algebra, OCR or a conversational model.

## This revision

Term-specific feedback distinguishes an incorrect coefficient from an incorrect constant. Changing a draft immediately invalidates the displayed result while retaining every previous attempt and hint. An older solved attempt cannot discard an unfinished edited draft. Keyboard focus is released on check/hint and feedback is revealed without hiding later controls.

The optional RevenueCat Test Store path uses process-local connection, refreshed access before a pack task, independent restoration, named package selection, local expiry/timeouts and stale-callback rejection. Local waiting-state cancellation does not cancel provider transactions. Free practice and hints require no store. No key is included, no SDK purchase result is invented, and release builds disable this test-only setup. Never publish a Test Store-configured APK to an app store.

## Build and inspect

JDK 17, Android SDK 35 and Gradle 8.13. From `pocket/`:

```
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
gradle --no-daemon :app:connectedDebugAndroidTest
```

The existing isolated device workflow records real Gradle results, Android 15 emulator tests, screenshots and process-stop/reopen evidence. Treat its artifact verification.json as the execution record, not this README. Tests include 28 retained checker tests, 50 billing/key/work-state scenarios and 12 coaching checks. Actual RevenueCat Test Store purchase/cancel/failure/restore/expiry remains a separate uncompleted gate until connected to an owned Test Store project. Share tests intercept the Android chooser and do not send anything.

The debug preview is for testing, not Play Store release. A previous debug APK signed by another runner may not accept an in-place update. Do not delete user data without first preserving any practice notes.

## Competition status

RevenueCat registration and a separate Pocket draft exist; final submission and Next Gen academic-email verification remain separate. No unaided-authorship, validated learning benefit, real revenue or contest acceptance is claimed. Original work by Joseph Ayanda, with substantial AI assistance, MIT licensed.

SDK documentation: https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store
