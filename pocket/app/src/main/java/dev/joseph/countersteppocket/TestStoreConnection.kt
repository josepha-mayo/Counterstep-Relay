package dev.joseph.countersteppocket

import android.content.Context
import android.os.SystemClock
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

/** Test-only SDK configuration. Remembering a public key needs separate, explicit permission. */
object TestStoreConnection {
    private const val PREFS = "test_store_setup"
    private const val SAVED_KEY = "public_test_key"
    private var key: String? = null
    val diagnostics = StoreDiagnostics { SystemClock.elapsedRealtime() }
    val configured: Boolean get() = key != null

    @Synchronized fun connect(context: Context, publicTestKey: String, remember: Boolean = false) {
        require(BuildConfig.DEBUG) { "The prototype only allows Test Store in debug builds." }
        require(TestStoreKey.accepts(publicTestKey)) { "A public test_ SDK key is required." }
        require(key == null || key == publicTestKey) { "Restart before changing the process Test Store." }
        if (key == null) {
            Purchases.logLevel = LogLevel.ERROR
            Purchases.configure(PurchasesConfiguration.Builder(context.applicationContext, publicTestKey).build())
            key = publicTestKey
            diagnostics.record(StoreDiagnostics.Operation.CONNECTION, StoreDiagnostics.Outcome.CONNECTED)
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit()
        if (remember) edit.putString(SAVED_KEY, publicTestKey) else edit.remove(SAVED_KEY)
        check(edit.commit()) { "Could not save the requested setup preference." }
    }

    @Synchronized fun reconnectRemembered(context: Context) {
        if (!BuildConfig.DEBUG || key != null) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(SAVED_KEY, null) ?: return
        if (!TestStoreKey.accepts(saved)) {
            prefs.edit().remove(SAVED_KEY).commit()
            return
        }
        // An SDK configuration exception leaves free practice available; no purchase is started.
        runCatching { connect(context, saved, true) }
    }

    fun hasRememberedKey(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(SAVED_KEY)

    fun forgetRememberedKey(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(SAVED_KEY).commit()
}
