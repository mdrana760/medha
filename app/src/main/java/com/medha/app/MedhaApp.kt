package com.medha.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.medha.app.firebase.FirebaseManager

class MedhaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseManager.init(this)
        createChannels()
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val service = NotificationChannel(
            CHANNEL_SERVICE,
            getString(R.string.svc_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = getString(R.string.svc_channel_desc) }

        val reply = NotificationChannel(
            CHANNEL_REPLY,
            getString(R.string.reply_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )

        manager.createNotificationChannel(service)
        manager.createNotificationChannel(reply)
    }

    companion object {
        const val CHANNEL_SERVICE = "medha_service"
        const val CHANNEL_REPLY = "medha_reply"
    }
}
