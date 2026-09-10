package dev.joseph.countersteppocket

import android.content.Context
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

/** An explicit, process-local Test Store connection. No secret or key is persisted/exported. */
object TestStoreConnection {
    private var key: String? = null
    val configured: Boolean get() = key != null

    @Synchronized fun connect(context: Context, publicTestKey: String) {
        require(BuildConfig.DEBUG) { "The prototype only allows Test Store in debug builds." }
        require(TestStoreKey.accepts(publicTestKey)) {
            "A public test_ SDK key is required; secret or platform keys are not accepted."
        }
        if (key != null) {
            require(key == publicTestKey) { "Another Test Store is already connected for this process." }
            return
        }
        Purchases.logLevel = LogLevel.ERROR
        Purchases.configure(PurchasesConfiguration.Builder(context.applicationContext, publicTestKey).build())
        key = publicTestKey
    }
}
