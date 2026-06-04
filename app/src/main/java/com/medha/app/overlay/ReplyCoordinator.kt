package com.medha.app.overlay

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory registry of drafted replies awaiting user approval, plus simple
 * counters surfaced on the dashboard. All access is process-local (the overlay,
 * the notification listener and the action receiver all live in this app).
 */
object ReplyCoordinator {

    private val pending = ConcurrentHashMap<String, PendingReply>()

    @Volatile var approvedToday: Int = 0; private set
    @Volatile var ignoredToday: Int = 0; private set

    /** Optional listener so the UI can react to queue changes. */
    var onChange: (() -> Unit)? = null

    fun register(reply: PendingReply) {
        pending[reply.id] = reply
        onChange?.invoke()
    }

    fun get(id: String): PendingReply? = pending[id]

    fun pendingCount(): Int = pending.size

    /** Sends [text] for the reply with [id]. Returns true if delivery succeeded. */
    fun send(id: String, text: CharSequence): Boolean {
        val reply = pending.remove(id) ?: return false
        val ok = runCatching { reply.sendAction(text) }.getOrDefault(false)
        if (ok) approvedToday++
        onChange?.invoke()
        return ok
    }

    fun ignore(id: String) {
        if (pending.remove(id) != null) {
            ignoredToday++
            onChange?.invoke()
        }
    }

    fun clear() {
        pending.clear()
        onChange?.invoke()
    }
}
