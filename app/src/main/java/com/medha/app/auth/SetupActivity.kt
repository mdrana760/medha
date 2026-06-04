package com.medha.app.auth

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.medha.app.MainActivity
import com.medha.app.R
import com.medha.app.ai.ApiKeyManager
import com.medha.app.data.Prefs
import com.medha.app.data.SupportedApps
import com.medha.app.databinding.ActivitySetupBinding
import com.medha.app.firebase.UserRepository
import com.medha.app.services.MedhaSystemService
import com.medha.app.ui.AppAdapter
import com.medha.app.utils.PermissionHelper
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var prefs: Prefs
    private lateinit var apiKeyManager: ApiKeyManager
    private val repository = UserRepository()

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        apiKeyManager = ApiKeyManager(this)

        binding.etApiKey.setText(apiKeyManager.getKey() ?: "")

        binding.btnGetKey.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")))
        }
        binding.btnValidate.setOnClickListener { validateApiKey() }

        binding.btnPermNotif.setOnClickListener {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
        binding.btnPermOverlay.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        binding.btnPermContacts.setOnClickListener {
            requestPermission.launch(Manifest.permission.READ_CONTACTS)
        }
        binding.btnPermPostNotif.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setupAppList()

        binding.btnBack.setOnClickListener { goBack() }
        binding.btnNext.setOnClickListener { goNext() }

        updateNav()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun setupAppList() {
        val pm = packageManager
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = AppAdapter(
            apps = SupportedApps.ALL,
            isEnabled = { prefs.isAppConnected(it) },
            isInstalled = { AppAdapter.isInstalled(pm, it) },
            onToggle = { pkg, on -> prefs.setAppConnected(pkg, on) }
        )
    }

    private fun validateApiKey() {
        val key = binding.etApiKey.text.toString().trim()
        if (key.isEmpty()) {
            binding.tvApiStatus.text = getString(R.string.apikey_empty)
            return
        }
        if (!apiKeyManager.isValidFormat(key)) {
            binding.tvApiStatus.text = getString(R.string.apikey_invalid)
            return
        }
        binding.pbApi.visibility = View.VISIBLE
        binding.tvApiStatus.text = getString(R.string.apikey_testing)
        lifecycleScope.launch {
            val ok = apiKeyManager.testKey(key)
            binding.pbApi.visibility = View.GONE
            if (ok) {
                apiKeyManager.saveKey(key)
                apiKeyManager.getEncryptedBlob()?.let { repository.saveApiKey(it) }
                binding.tvApiStatus.text = getString(R.string.apikey_valid)
            } else {
                binding.tvApiStatus.text = getString(R.string.apikey_invalid)
            }
        }
    }

    private fun refreshPermissions() {
        bindStatus(binding.tvNotifStatus, PermissionHelper.isNotificationAccessGranted(this))
        bindStatus(binding.tvOverlayStatus, PermissionHelper.canDrawOverlays(this))
        bindStatus(
            binding.tvContactsStatus,
            com.medha.app.utils.ContactHelper.hasPermission(this)
        )
        val postOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        bindStatus(binding.tvPostNotifStatus, postOk)
    }

    private fun bindStatus(view: android.widget.TextView, granted: Boolean) {
        view.text = getString(if (granted) R.string.perm_granted else R.string.perm_grant)
    }

    private fun goBack() {
        if (binding.flipper.displayedChild > 0) {
            binding.flipper.showPrevious()
            updateNav()
        } else {
            finish()
        }
    }

    private fun goNext() {
        val last = binding.flipper.childCount - 1
        if (binding.flipper.displayedChild < last) {
            binding.flipper.showNext()
            updateNav()
        } else {
            completeSetup()
        }
    }

    private fun updateNav() {
        val last = binding.flipper.childCount - 1
        binding.btnNext.text = getString(
            if (binding.flipper.displayedChild == last) R.string.setup_finish else R.string.setup_next
        )
        binding.btnBack.visibility =
            if (binding.flipper.displayedChild == 0) View.INVISIBLE else View.VISIBLE
    }

    private fun completeSetup() {
        prefs.setupComplete = true
        prefs.masterEnabled = true
        // Mirror the chosen apps to Firestore.
        val appMap = SupportedApps.ALL.associate { it.packageName to prefs.isAppConnected(it.packageName) }
        lifecycleScope.launch { repository.saveConnectedApps(appMap) }
        MedhaSystemService.start(this)
        Toast.makeText(this, R.string.setup_done_title, Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
