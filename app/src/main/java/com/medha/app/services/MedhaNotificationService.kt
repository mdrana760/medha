package com.medha.app.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.medha.app.MedhaApp
import com.medha.app.R
import com.medha.app.ai.ApiKeyManager
import com.medha.app.ai.ConversationMemory
import com.medha.app.ai.GeminiClient
import com.medha.app.ai.PersonalityEngine
import com.medha.app.data.Prefs
import com.medha.app.data.ScheduleManager
import com.medha.app.firebase.FirebaseManager
import com.medha.app.firebase.UserRepository
import com.medha.app.overlay.OverlayManager
import com.medha.app.overlay.PendingReply
import com.medha.app.overlay.ReplyActionReceiver
import com.medha.app.overlay.ReplyCoordinator
import com.medha.app.utils.NetworkMonitor
import com.medha.app.utils.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID

/**
 * The heart of the consent-based assistant. It uses the official Android
 * NotificationListener API (which the user must explicitly enable) to read
 * incoming messages from the apps the user selected, drafts a reply with the
 * user's own Gemini key, and surfaces it for approval. It only ever replies
 * through the originating notification's own RemoteInput action — it never hooks
 * into other apps or sends anything silently.
 */
class MedhaNotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handledKeys = Collections.synchronizedSet(HashSet<String>())

    private lateinit var prefs: Prefs
    private lateinit var apiKeyManager: ApiKeyManager
    private lateinit var conversationMemory: ConversationMemory
    private val gemini = GeminiClient()
    private val repository = UserRepository()

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        apiKeyManager = ApiKeyManager(this)
        conversationMemory = ConversationMemory(this)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!shouldHandle(sbn)) return

        val notification = sbn.notification ?: return
        val replyAction = findReplyAction(notification) ?: return // app offers no inline reply
        val (sender, incomingText) = extractContent(notification) ?: return

        val dedupeKey = sbn.key + "|" + incomingText.hashCode()
        if (!handledKeys.add(dedupeKey)) return

        val appLabel = appLabel(sbn.packageName)
        val contactId = (sbn.packageName + ":" + sender).replace("/", "_")

        scope.launch {
            val userName = FirebaseManager.auth?.currentUser?.displayName ?: "User"
            val language = prefs.language

            conversationMemory.record(contactId, sbn.packageName, sender, incomingText, isAI = false)

            val draft = draftReply(contactId, sender, incomingText, userName, language)

            val reply = PendingReply(
                id = UUID.randomUUID().toString(),
                packageName = sbn.packageName,
                appLabel = appLabel,
                contactId = contactId,
                sender = sender,
                incomingText = incomingText,
                suggestedText = draft,
                sendAction = { text -> deliver(replyAction, text, contactId, sbn.packageName, sender) }
            )
            ReplyCoordinator.register(reply)
            presentForApproval(reply)
        }
    }

    /** Generates the reply text, falling back to a safe canned message. */
    private suspend fun draftReply(
        contactId: String,
        sender: String,
        incomingText: String,
        userName: String,
        language: String
    ): String {
        val apiKey = apiKeyManager.getKey()
        if (apiKey.isNullOrBlank() || !NetworkMonitor.isOnline(this)) {
            return PersonalityEngine.fallbackReply(userName, language)
        }
        val history = conversationMemory.contextFor(contactId)
        val prompt = PersonalityEngine.buildPrompt(
            PersonalityEngine.Channel.MESSAGE, userName, sender, history, language
        )
        return gemini.generate(apiKey, prompt, incomingText)
            .getOrElse { PersonalityEngine.fallbackReply(userName, language) }
    }

    /** Shows the overlay if allowed, otherwise posts an approval notification. */
    private fun presentForApproval(reply: PendingReply) {
        val autoSend = if (prefs.requireApproval) null else prefs.responseDelay
        if (PermissionHelper.canDrawOverlays(this)) {
            OverlayManager.showReplyPreview(this, reply, autoSend)
        } else {
            postApprovalNotification(reply)
        }
    }

    /** Fires the originating app's RemoteInput action to actually send [text]. */
    private fun deliver(
        action: Notification.Action,
        text: CharSequence,
        contactId: String,
        pkg: String,
        sender: String
    ): Boolean {
        return try {
            val remoteInputs = action.remoteInputs ?: return false
            val intent = Intent()
            val results = Bundle()
            for (ri in remoteInputs) results.putCharSequence(ri.resultKey, text)
            RemoteInput.addResultsToIntent(remoteInputs, intent, results)
            action.actionIntent.send(this, 0, intent)

            scope.launch {
                conversationMemory.record(contactId, pkg, sender, text.toString(), isAI = true)
                repository.saveConversationMessage(contactId, pkg, sender, text.toString(), isAI = true)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun shouldHandle(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return false
        if (!prefs.masterEnabled) return false
        if (!prefs.isAppConnected(sbn.packageName)) return false
        if (!ScheduleManager(prefs).isActiveNow()) return false

        val n = sbn.notification ?: return false
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return false

        if (prefs.autoReplyMode == "screen_off") {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (pm.isInteractive) return false
        }
        return true
    }

    private fun findReplyAction(notification: Notification): Notification.Action? {
        val actions = notification.actions ?: return null
        return actions.firstOrNull { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        }
    }

    private fun extractContent(notification: Notification): Pair<String, String>? {
        val extras = notification.extras ?: return null
        val sender = extras.getString(Notification.EXTRA_TITLE)?.takeIf { it.isNotBlank() }
            ?: "Unknown"
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        if (text.isNullOrBlank()) return null
        return sender to text
    }

    private fun appLabel(pkg: String): String {
        return try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }
    }

    private fun postApprovalNotification(reply: PendingReply) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        val immutable = PendingIntent.FLAG_IMMUTABLE

        val remoteInput = androidx.core.app.RemoteInput.Builder(ReplyActionReceiver.KEY_REPLY_TEXT)
            .setLabel(getString(R.string.reply_edit))
            .build()

        val sendIntent = Intent(this, ReplyActionReceiver::class.java).apply {
            action = ReplyActionReceiver.ACTION_SEND
            putExtra(ReplyActionReceiver.EXTRA_ID, reply.id)
        }
        val sendPending = PendingIntent.getBroadcast(
            this, reply.id.hashCode(), sendIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutable
        )
        val sendAction = NotificationCompat.Action.Builder(
            0, getString(R.string.reply_send_now), sendPending
        ).addRemoteInput(remoteInput).build()

        val ignoreIntent = Intent(this, ReplyActionReceiver::class.java).apply {
            action = ReplyActionReceiver.ACTION_IGNORE
            putExtra(ReplyActionReceiver.EXTRA_ID, reply.id)
        }
        val ignorePending = PendingIntent.getBroadcast(
            this, reply.id.hashCode() + 1, ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutable
        )
        val ignoreAction = NotificationCompat.Action.Builder(
            0, getString(R.string.reply_ignore), ignorePending
        ).build()

        val notif = NotificationCompat.Builder(this, MedhaApp.CHANNEL_REPLY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${reply.appLabel} · ${reply.sender}")
            .setContentText(reply.suggestedText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reply.suggestedText))
            .addAction(sendAction)
            .addAction(ignoreAction)
            .setAutoCancel(true)
            .build()

        runCatching { NotificationManagerCompat.from(this).notify(reply.id.hashCode(), notif) }
    }
}
