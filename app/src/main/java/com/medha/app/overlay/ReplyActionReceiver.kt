package com.medha.app.overlay

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.medha.app.R

/**
 * Handles the Send / Ignore actions of the fallback approval notification that
 * is posted when the floating overlay permission isn't granted. The user edits
 * the reply inline (RemoteInput) and taps Send — still an explicit approval.
 */
class ReplyActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        when (intent.action) {
            ACTION_SEND -> {
                val edited = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(KEY_REPLY_TEXT)?.toString()
                val text = edited ?: ReplyCoordinator.get(id)?.suggestedText ?: ""
                val ok = ReplyCoordinator.send(id, text)
                Toast.makeText(
                    context,
                    if (ok) R.string.reply_sent else R.string.error_generic,
                    Toast.LENGTH_SHORT
                ).show()
            }

            ACTION_IGNORE -> {
                ReplyCoordinator.ignore(id)
                Toast.makeText(context, R.string.reply_ignored, Toast.LENGTH_SHORT).show()
            }
        }
        NotificationManagerCompat.from(context).cancel(id.hashCode())
    }

    companion object {
        const val ACTION_SEND = "com.medha.app.action.SEND_REPLY"
        const val ACTION_IGNORE = "com.medha.app.action.IGNORE_REPLY"
        const val EXTRA_ID = "extra_reply_id"
        const val KEY_REPLY_TEXT = "key_reply_text"
    }
}
