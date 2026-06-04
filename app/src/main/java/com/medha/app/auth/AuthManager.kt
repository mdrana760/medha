package com.medha.app.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.medha.app.firebase.FirebaseManager
import kotlinx.coroutines.tasks.await

/**
 * Firebase Auth + Google Sign-In wrapper. The user signs in with their own
 * Google account; Medha stores data only under that account.
 */
class AuthManager(context: Context) {

    private val appContext = context.applicationContext

    fun currentUser(): FirebaseUser? = FirebaseManager.auth?.currentUser

    fun isSignedIn(): Boolean = currentUser() != null

    private fun googleClient(): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        // default_web_client_id is generated from google-services.json. Look it
        // up reflectively so the project still compiles without that file.
        val resId = appContext.resources.getIdentifier(
            "default_web_client_id", "string", appContext.packageName
        )
        if (resId != 0) {
            builder.requestIdToken(appContext.getString(resId))
        }
        return GoogleSignIn.getClient(appContext, builder.build())
    }

    fun signInIntent(): Intent = googleClient().signInIntent

    suspend fun handleSignInResult(data: Intent?): Result<FirebaseUser> {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
                ?: return Result.failure(IllegalStateException("Missing Google ID token. Is Firebase configured?"))
            val auth = FirebaseManager.auth
                ?: return Result.failure(IllegalStateException("Firebase not available"))
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Sign-in returned no user"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        runCatching { FirebaseManager.auth?.signOut() }
        runCatching { googleClient().signOut().await() }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = currentUser()
            ?: return Result.failure(IllegalStateException("No signed-in user"))
        return try {
            user.delete().await()
            runCatching { googleClient().signOut().await() }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
