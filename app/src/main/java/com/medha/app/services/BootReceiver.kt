package com.medha.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.medha.app.data.Prefs

/**
 * Restarts the foreground service after reboot — but only if the user finished
 * setup and left Medha enabled. (This is the normal, documented use of
 * RECEIVE_BOOT_COMPLETED; it does not install anything to the system partition
 * or attempt to survive a factory reset.)
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = Prefs(context)
        if (prefs.setupComplete && prefs.masterEnabled) {
            MedhaSystemService.start(context)
        }
    }
}
