package com.medha.app.overlay

/**
 * A reply that Medha has drafted and is waiting for the user to approve.
 *
 * [sendAction] encapsulates the actual delivery (firing the originating app's
 * RemoteInput action). It returns true on success. Keeping it as a lambda means
 * the coordinator/overlay never need to know how a particular app sends.
 */
data class PendingReply(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val contactId: String,
    val sender: String,
    val incomingText: String,
    var suggestedText: String,
    val sendAction: (CharSequence) -> Boolean
)
