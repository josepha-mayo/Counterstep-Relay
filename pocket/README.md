# Counterstep Pocket: native Android prototype

A separate native-mobile implementation of the Counterstep repair-to-practice idea. It does not replace the existing Amazon or Prom entries. The current slice is deliberately focused: expand an integer multiple of (x + an integer), check it, retain the first response and hint history, persist a draft, reopen and share a practice note.

Core practice and hints are free and work offline. The optional mixed-sign pack has an actual pinned RevenueCat Android SDK adapter for Test Store offerings, purchase and restore callbacks. Public Test Store keys are entered only after the operator accepts the SDK connection. No key is shipped, no provider success is fabricated, and local study files cannot grant an entitlement. The SDK's active mixed_signs entitlement is the only in-process unlock source. This prototype is not configured for real charges or store publication.

## Build

JDK 17, Android SDK 35, Gradle 8.13:

```
gradle :app:testDebugUnitTest :app:assembleDebug
```

The isolated GitHub workflow produces the debug APK and test XML. Open this directory as an Android Studio project. The included exercise checker is new Kotlin code scoped to expanded integer-linear terms, not a full port of the earlier algebra/model stack. No conversational model or photo OCR is claimed.

## RevenueCat setup and evidence still needed

Create an owned RevenueCat Test Store app, a product, the mixed_signs entitlement, and a current offering. On device, open Mixed-sign practice pack and connect its public test_ SDK key. Test cancellation, failure, success, expiry and restore against the actual SDK and confirm the original exercise draft survives. Test Store transactions are sandbox events, not revenue. Never use a secret API key in a mobile client. Do not release this test-key prototype to an app store.

No account/configuration or live Test Store transaction was available during this initial code stage. A compiled dependency and local callback-state tests are not end-to-end payment evidence. Android runtime interaction, accessibility and lifecycle/expiry behavior still require device or emulator checks before a hackathon demo.

## Registration and submission remain pending

Target prospect: RevenueCat Shipaton Next Gen. Registration requires explicit consent to the official rules/Devpost terms. Student-category entry needs active student status and a qualifying academic email on Devpost. Neither registration nor student verification is implied by this source. The required meaningful RevenueCat demonstration, public open-source repository, under-two-minute device video, icon and screenshot remain final-entry tasks.

Sources: https://revenuecat-shipaton-2026.devpost.com/rules ; https://www.revenuecat.com/docs/getting-started/installation/android ; https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store ; https://github.com/RevenueCat/purchases-android/blob/9.9.0/purchases/src/main/kotlin/com/revenuecat/purchases/ListenerConversionsCommon.kt

Original work by Joseph Ayanda, developed with substantial AI assistance. MIT. Dependency licenses are retained by their distributions. Synthetic exercises, no student personal data, no measured learning or revenue claim.
