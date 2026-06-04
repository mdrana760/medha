package com.medha.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medha.app.R
import com.medha.app.ai.ApiKeyManager
import com.medha.app.auth.AuthManager
import com.medha.app.auth.LoginActivity
import com.medha.app.data.Prefs
import com.medha.app.databinding.ActivityProfileBinding
import com.medha.app.firebase.FirebaseManager
import com.medha.app.firebase.UserRepository
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var authManager: AuthManager
    private lateinit var apiKeyManager: ApiKeyManager
    private val repository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)
        apiKeyManager = ApiKeyManager(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val user = FirebaseManager.auth?.currentUser
        binding.tvName.text = user?.displayName ?: "—"
        binding.tvEmail.text = user?.email ?: "—"
        binding.tvMaskedKey.text = apiKeyManager.maskedKey()

        binding.btnUpdateKey.setOnClickListener { showUpdateKeyDialog() }

        binding.btnSignOut.setOnClickListener {
            lifecycleScope.launch {
                authManager.signOut()
                goToLogin()
            }
        }

        binding.btnDelete.setOnClickListener { confirmDelete() }
    }

    private fun showUpdateKeyDialog() {
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.setup_apikey_hint)
            setText(apiKeyManager.getKey() ?: "")
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.profile_update_key)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val key = input.text.toString().trim()
                if (!apiKeyManager.isValidFormat(key)) {
                    Toast.makeText(this, R.string.apikey_invalid, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                binding.progress.visibility = View.VISIBLE
                lifecycleScope.launch {
                    val ok = apiKeyManager.testKey(key)
                    binding.progress.visibility = View.GONE
                    if (ok) {
                        apiKeyManager.saveKey(key)
                        apiKeyManager.getEncryptedBlob()?.let { repository.saveApiKey(it) }
                        binding.tvMaskedKey.text = apiKeyManager.maskedKey()
                        Toast.makeText(this@ProfileActivity, R.string.apikey_valid, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, R.string.apikey_invalid, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.profile_delete)
            .setMessage(R.string.profile_delete_confirm)
            .setPositiveButton(R.string.profile_delete) { _, _ ->
                lifecycleScope.launch {
                    apiKeyManager.clear()
                    authManager.deleteAccount()
                    goToLogin()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun goToLogin() {
        Prefs(this).setupComplete = false
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
