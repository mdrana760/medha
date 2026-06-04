package com.medha.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.medha.app.data.Prefs
import com.medha.app.data.SupportedApps
import com.medha.app.databinding.ActivityAppManagerBinding
import com.medha.app.firebase.UserRepository
import kotlinx.coroutines.launch

class AppManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppManagerBinding
    private lateinit var prefs: Prefs
    private val repository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val pm = packageManager
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = AppAdapter(
            apps = SupportedApps.ALL,
            isEnabled = { prefs.isAppConnected(it) },
            isInstalled = { AppAdapter.isInstalled(pm, it) },
            onToggle = { pkg, on ->
                prefs.setAppConnected(pkg, on)
                syncToFirebase()
            }
        )
    }

    private fun syncToFirebase() {
        val appMap = SupportedApps.ALL.associate {
            it.packageName to prefs.isAppConnected(it.packageName)
        }
        lifecycleScope.launch { repository.saveConnectedApps(appMap) }
    }
}
