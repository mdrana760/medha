package com.medha.app.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medha.app.MainActivity
import com.medha.app.R
import com.medha.app.data.Prefs
import com.medha.app.databinding.ActivityLoginBinding
import com.medha.app.firebase.FirebaseManager
import com.medha.app.firebase.UserRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var authManager: AuthManager
    private val repository = UserRepository()

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        lifecycleScope.launch {
            val outcome = authManager.handleSignInResult(result.data)
            outcome.onSuccess { user ->
                repository.saveUserProfile(user)
                routeNext()
            }.onFailure {
                setLoading(false)
                Toast.makeText(this@LoginActivity, R.string.sign_in_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager(this)

        if (!FirebaseManager.isAvailable) {
            binding.tvFirebaseWarning.visibility = View.VISIBLE
        }

        // Auto-login when a session already exists.
        if (authManager.isSignedIn()) {
            routeNext()
            return
        }

        binding.btnGoogleSignIn.setOnClickListener {
            if (!FirebaseManager.isAvailable) {
                Toast.makeText(this, R.string.error_firebase_missing, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            setLoading(true)
            signInLauncher.launch(authManager.signInIntent())
        }
    }

    private fun routeNext() {
        val next = if (Prefs(this).setupComplete) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, SetupActivity::class.java)
        }
        startActivity(next)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnGoogleSignIn.isEnabled = !loading
    }
}
