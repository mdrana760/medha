package com.medha.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.medha.app.R

/**
 * Draws the approval popup on top of other apps. The popup always shows the
 * incoming message and the drafted reply, and nothing is sent until the user
 * taps "Send" (or, when the user has explicitly disabled approval, after a
 * visible countdown they can cancel).
 */
object OverlayManager {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var timer: CountDownTimer? = null

    fun showReplyPreview(context: Context, reply: PendingReply, autoSendSeconds: Int?) {
        val appCtx = context.applicationContext
        if (!Settings.canDrawOverlays(appCtx)) return

        mainHandler.post {
            dismiss()
            val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = LayoutInflater.from(appCtx)
                .inflate(R.layout.overlay_reply_preview, null)

            view.findViewById<TextView>(R.id.tvApp).text = reply.appLabel
            view.findViewById<TextView>(R.id.tvSender).text = reply.sender
            view.findViewById<TextView>(R.id.tvIncoming).text = reply.incomingText
            val editText = view.findViewById<EditText>(R.id.etReply)
            editText.setText(reply.suggestedText)

            val countdownView = view.findViewById<TextView>(R.id.tvCountdown)
            val btnSend = view.findViewById<Button>(R.id.btnSend)
            val btnEdit = view.findViewById<Button>(R.id.btnEdit)
            val btnIgnore = view.findViewById<Button>(R.id.btnIgnore)

            fun finishWithSend() {
                timer?.cancel()
                val ok = ReplyCoordinator.send(reply.id, editText.text.toString())
                Toast.makeText(
                    appCtx,
                    if (ok) R.string.reply_sent else R.string.error_generic,
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }

            btnSend.setOnClickListener { finishWithSend() }
            btnEdit.setOnClickListener {
                timer?.cancel()
                countdownView.visibility = View.GONE
                editText.requestFocus()
                editText.setSelection(editText.text.length)
            }
            btnIgnore.setOnClickListener {
                timer?.cancel()
                ReplyCoordinator.ignore(reply.id)
                Toast.makeText(appCtx, R.string.reply_ignored, Toast.LENGTH_SHORT).show()
                dismiss()
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM
                dimAmount = 0.4f
            }

            runCatching {
                wm.addView(view, params)
                windowManager = wm
                overlayView = view
            }

            // Only auto-send when the user has explicitly opted out of approval.
            if (autoSendSeconds != null && autoSendSeconds > 0) {
                countdownView.visibility = View.VISIBLE
                timer = object : CountDownTimer(autoSendSeconds * 1000L, 1000L) {
                    override fun onTick(ms: Long) {
                        countdownView.text = appCtx.getString(
                            R.string.reply_countdown, (ms / 1000).toInt()
                        )
                    }

                    override fun onFinish() = finishWithSend()
                }.start()
            } else {
                countdownView.visibility = View.GONE
            }
        }
    }

    fun dismiss() {
        mainHandler.post {
            timer?.cancel()
            timer = null
            val view = overlayView
            val wm = windowManager
            if (view != null && wm != null) {
                runCatching { wm.removeView(view) }
            }
            overlayView = null
            windowManager = null
        }
    }
}
