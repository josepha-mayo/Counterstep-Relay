package dev.joseph.countersteppocket

import java.util.ArrayDeque

/** Small local diagnostic, never a receipt or a source of access rights. No free-text SDK data. */
class StoreDiagnostics(private val now: () -> Long) {
    enum class Operation { SESSION, CONNECTION, REFRESH, OFFERINGS, PURCHASE, RESTORE }
    enum class Outcome { OPENED, CONNECTED, STARTED, ACTIVE_RESPONSE, INACTIVE_RESPONSE,
        OFFERING_AVAILABLE, OFFERING_MISSING, ERROR, CANCELLED, TIMEOUT, STALE_IGNORED }
    data class Entry(val sequence: Long, val elapsedMillis: Long, val operation: Operation, val outcome: Outcome)
    private val entries = ArrayDeque<Entry>()
    private var sequence = 0L
    private val started = now()
    @Synchronized fun record(operation: Operation, outcome: Outcome) {
        if (entries.size == 64) entries.removeFirst()
        entries.addLast(Entry(++sequence, (now() - started).coerceAtLeast(0), operation, outcome))
    }
    @Synchronized fun snapshot(): List<Entry> = entries.toList()
    @Synchronized fun report(): String = buildString {
        append("Counterstep Pocket | local Test Store diagnostic\n")
        append("RevenueCat Android SDK: 9.9.0\n")
        append("Scope: this app process only; up to 64 recent events.\n")
        append("No SDK key, customer ID, transaction ID, raw provider message, or practice text is included.\n")
        append("These are app observations, not authenticated receipts or proof of payment.\n")
        append("Test Store uses sandbox data. Restore here is not validation of Google Play purchase recovery.\n\n")
        if (entries.isEmpty()) append("No store events recorded.\n")
        entries.forEach { append("${it.sequence}. +${it.elapsedMillis}ms | ${it.operation} | ${it.outcome}\n") }
    }
}
