# Pocket Test Store readiness, 13 September 2026

The 0.4 preview adds explicit optional public-key persistence, a setup/check screen,
and a bounded local diagnostic export. Existing algebra, hint and entitlement
coordination logic is retained. No private RevenueCat API key is required by the
app. No owned Test Store or completed purchase is claimed by this source update.

## Connect the actual owned project

In the RevenueCat dashboard, select the intended project. Use its Test Store public
SDK key from Project Settings > API keys. Do not use a secret API key. Configure an
entitlement named `mixed_signs`, attach the intended Test Store product, and put that
product in exactly one package named `mixed_signs` in the current offering.

In Pocket open Test Store checks and setup, then Check current access. Connecting
requires explicit confirmation. Remember this public test key on this device is
unchecked by default. Opting in stores only that public configuration in private
app preferences and permits reconnecting after process restart. Android backup is
disabled. Forget remembered key removes that saved setup, not the SDK's in-memory
state, anonymous identity or provider purchase history. Close the app process to
end that in-memory configuration. It does not cancel a transaction already started.

No key is bundled in the source/APK and no purchase starts automatically on launch.
Typing practice, checking work, hints and sharing practice remain independent of
store setup. Normal foreground refresh requests access only when configured.

## Actual integration evidence still required

With the real owned public key, verify offering retrieval, canceled checkout,
failed checkout, a successful Test Store purchase and entitlement refresh. Preserve
an unfinished practice draft during those checks. After process restart, reconnect
using the explicitly remembered key and verify current access for the SDK's cached
anonymous user. Do not clear app data and call a new anonymous identity the same
user. Inspect the same sandbox customer in the owned RevenueCat dashboard.

The existing Restore test access entry still calls the SDK restore API. Its result
is described as current test access, not proof of Google Play restore or ownership
transfer. RevenueCat's Test Store has no underlying Google Play store account to
validate cross-account recovery. A real platform-restore claim needs a separately
authorized platform sandbox; it is not silently added to this test-only app.

Official sources checked September 13:
- https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store
- https://www.revenuecat.com/docs/getting-started/restoring-purchases
- https://www.revenuecat.com/docs/customers/identifying-customers
- https://community.revenuecat.com/sdks-51/are-syncpurchases-and-restorepurchases-sdk-calls-meant-to-work-on-the-revenuecat-test-store-7779

## Diagnostic scope

Share store diagnostic opens Android's chooser; it never sends automatically. The
report contains only bounded enum events and relative elapsed time from this app
process, not SDK keys, customer/transaction IDs, raw provider error text, or typed
practice. Callbacks ignored by the access gate are labeled STALE_IGNORED. A local
request timeout does not cancel a provider transaction. An active response means
an SDK access response was accepted, not that a fresh purchase occurred.

Reports are editable local observations, not signed receipts. They cannot grant
access, establish income, or replace dashboard verification. No valid key or real
SDK purchase response is fabricated by the device test suite. These checks must
not be represented as a completed RevenueCat integration or competition entry.

## Build and native checks

Retained 98 JVM and 19 native test methods remain present. Ten JVM diagnostic tests
and six native unconfigured-setup/export tests are added. CI must compile the app
and execute all 108 JVM / 25 Android tests before the candidate is called verified.
Screenshot capture runs the real emulator at 1179 x 2556 and saves unedited pixels.
It is not a physical Redmi test. See the actual run artifact for results, not this
planned count. The RevenueCat project ID, student verification, actual purchase
flow and final video/submission remain distinct release tasks.
