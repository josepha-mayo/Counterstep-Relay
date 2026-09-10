package dev.joseph.countersteppocket

/** Local UI coordination, not provider authentication or a persistent purchase ledger. */
class BillingGate(
    private val elapsedMillis: () -> Long,
    private val wallMillis: () -> Long,
    private val freshnessMillis: Long = 60_000,
    private val requestTimeoutMillis: Long = 30_000
) {
    init { require(freshnessMillis in 1..300_000 && requestTimeoutMillis in 1..120_000) }
    enum class Kind { REFRESH, OFFERINGS, RESTORE, PURCHASE }
    class Ticket internal constructor(val kind: Kind, internal val startedAt: Long)
    private var foreground = false
    private var destroyed = false
    private var current: Ticket? = null
    private var granted = false
    private var checkedElapsed: Long? = null
    private var checkedWall: Long? = null
    private var expiresAt: Long? = null

    val busy: Boolean get() = current != null
    val isForeground: Boolean get() = foreground && !destroyed
    val active: Boolean get() {
        val elapsed = checkedElapsed ?: return false
        val wall = checkedWall ?: return false
        val nowElapsed = elapsedMillis()
        val nowWall = wallMillis()
        return isForeground && granted && nowElapsed >= elapsed &&
            nowElapsed - elapsed < freshnessMillis && nowWall >= wall &&
            (expiresAt?.let { nowWall < it } ?: true)
    }

    fun onForeground() { if (!destroyed) foreground = true }
    fun onBackground() { foreground = false; current = null; clearAccess() }
    fun destroy() { onBackground(); destroyed = true }

    fun begin(kind: Kind): Ticket? {
        if (!isForeground || current != null) return null
        if (kind != Kind.OFFERINGS) clearAccess()
        val ticket = Ticket(kind, elapsedMillis())
        current = ticket
        return ticket
    }

    /** Identity comparison prevents a ticket from a different gate accepting a result. */
    private fun accepts(ticket: Ticket): Boolean {
        if (!isForeground || current !== ticket) return false
        val elapsed = elapsedMillis()
        if (elapsed < ticket.startedAt || elapsed - ticket.startedAt >= requestTimeoutMillis) {
            current = null
            clearAccess()
            return false
        }
        return true
    }

    fun finishOfferings(ticket: Ticket): Boolean {
        if (ticket.kind != Kind.OFFERINGS || !accepts(ticket)) return false
        current = null
        return true
    }

    /** Call only with the active mixed_signs entitlement and expiry returned by the SDK. */
    fun finishAccess(ticket: Ticket, sdkActive: Boolean, expirationWallMillis: Long?): Boolean {
        if (ticket.kind == Kind.OFFERINGS || !accepts(ticket)) return false
        current = null
        val wall = wallMillis()
        checkedElapsed = elapsedMillis()
        checkedWall = wall
        expiresAt = expirationWallMillis
        granted = sdkActive && (expirationWallMillis?.let { it > wall } ?: true)
        return true
    }

    fun fail(ticket: Ticket): Boolean {
        if (!accepts(ticket)) return false
        current = null
        if (ticket.kind != Kind.OFFERINGS) clearAccess()
        return true
    }

    /** Invalidate local waiting state only; this does not cancel a provider transaction. */
    fun expireOutstanding(ticket: Ticket): Boolean {
        if (current !== ticket) return false
        val elapsed = elapsedMillis()
        if (elapsed >= ticket.startedAt && elapsed - ticket.startedAt < requestTimeoutMillis) return false
        current = null
        clearAccess()
        return true
    }

    private fun clearAccess() {
        granted = false; checkedElapsed = null; checkedWall = null; expiresAt = null
    }
}

/** Snapshot of work at the time of a request. Async store callbacks must not replace newer work. */
data class WorkStamp(val sequence: Int, val coefficient: Int, val offset: Int,
                     val draft: String, val attempts: List<Attempt>, val hints: Int)
fun Practice.workStamp(sequence: Int) = WorkStamp(sequence, task.coefficient, task.offset,
                                                 draft, attempts.toList(), hints)

/** Public test-key shape only. Matching this pattern does not authenticate a provider account. */
object TestStoreKey {
    fun accepts(value: String): Boolean = Regex("test_[A-Za-z0-9_]{10,200}").matches(value)
}
