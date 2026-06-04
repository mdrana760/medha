package com.medha.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.medha.app.data.MedhaDatabase
import com.medha.app.data.Prefs
import com.medha.app.databinding.ActivityMainBinding
import com.medha.app.firebase.FirebaseManager
import com.medha.app.overlay.ReplyCoordinator
import com.medha.app.services.MedhaSystemService
import com.medha.app.ui.AppManagerActivity
import com.medha.app.ui.ConversationAdapter
import com.medha.app.ui.ConversationLogActivity
import com.medha.app.ui.ProfileActivity
import com.medha.app.ui.SettingsActivity
import com.medha.app.utils.PermissionHelper
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private val adapter = ConversationAdapter(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)

        binding.rvActivity.layoutManager = LinearLayoutManager(this)
        binding.rvActivity.adapter = adapter

        binding.switchMaster.setOnCheckedChangeListener { _, checked ->
            prefs.masterEnabled = checked
            binding.tvMasterStatus.text =
                getString(if (checked) R.string.master_on else R.string.master_off)
            if (checked) MedhaSystemService.start(this) else MedhaSystemService.stop(this)
        }

        binding.btnApps.setOnClickListener { startActivity(Intent(this, AppManagerActivity::class.java)) }
        binding.btnProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.btnLogs.setOnClickListener { startActivity(Intent(this, ConversationLogActivity::class.java)) }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        binding.switchMaster.isChecked = prefs.masterEnabled
        binding.tvMasterStatus.text =
            getString(if (prefs.masterEnabled) R.string.master_on else R.string.master_off)

        binding.tvFirebaseStatus.text = getString(
            if (FirebaseManager.isAvailable) R.string.status_connected else R.string.status_disconnected
        )

        val notifOk = PermissionHelper.isNotificationAccessGranted(this)
        val overlayOk = PermissionHelper.canDrawOverlays(this)
        binding.tvPermStatus.text = "Notification access: ${tick(notifOk)}   Overlay: ${tick(overlayOk)}"

        binding.tvStatPending.text = ReplyCoordinator.pendingCount().toString()
        binding.tvStatApproved.text = ReplyCoordinator.approvedToday.toString()

        loadStats()
    }

    private fun tick(ok: Boolean): String = if (ok) "✓" else "✗"

    private fun loadStats() {
        val dao = MedhaDatabase.get(this).messageDao()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        lifecycleScope.launch {
            val count = dao.countAiSince(startOfDay)
            binding.tvStatMessages.text = count.toString()
            val recent = dao.recent(15)
            adapter.submit(recent)
            binding.tvNoActivity.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
