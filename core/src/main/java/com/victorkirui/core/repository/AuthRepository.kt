package com.victorkirui.core.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.victorkirui.core.data.DataClearer
import com.victorkirui.core.monitoring.Analytics
import com.victorkirui.core.monitoring.CrashReporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class AuthUserResult(
    val user: FirebaseUser,
    val isNewUser: Boolean = false
)

interface AuthRepository {
    val currentUser: StateFlow<FirebaseUser?>
    suspend fun signUp(email: String, password: String, name: String): Result<AuthUserResult>
    suspend fun signIn(email: String, password: String): Result<AuthUserResult>
    suspend fun signInWithGoogle(idToken: String): Result<AuthUserResult>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut(clearData: Boolean = true)
    suspend fun deleteAccount(): Result<Unit>
    suspend fun getIdToken(forceRefresh: Boolean = false): String?
    suspend fun updateDisplayName(name: String): Result<Unit>
}

class FirebaseAuthRepository(
    private val context: android.content.Context,
    private val analytics: Analytics,
    private val crashReporter: CrashReporter,
    private val dataClearers: List<DataClearer> = emptyList(),
    private val onPurgeRemoteAccount: (suspend () -> Unit)? = null
) : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val _currentUser = MutableStateFlow(auth.currentUser)
    override val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        auth.addAuthStateListener {
            val user = it.currentUser ?: auth.currentUser
            val current = _currentUser.value
            if (user != null) {
                if (current?.uid == user.uid && !current.displayName.isNullOrBlank() && user.displayName.isNullOrBlank()) {
                    // Retain non-empty displayName set during signUp until Firebase cache refreshes
                } else {
                    _currentUser.value = user
                }
                analytics.setUserId(user.uid)
                crashReporter.setUserId(user.uid)
            } else {
                _currentUser.value = null
            }
        }
        auth.currentUser?.let { user ->
            if (_currentUser.value == null) {
                _currentUser.value = user
            }
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<AuthUserResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign up failed")
            
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = name
            }
            user.updateProfile(profileUpdates).await()
            try {
                user.reload().await()
            } catch (_: Exception) {}
            
            val refreshedUser = auth.currentUser ?: user
            _currentUser.value = refreshedUser
            Result.success(AuthUserResult(refreshedUser, isNewUser = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<AuthUserResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")
            val isNew = result.additionalUserInfo?.isNewUser ?: false
            _currentUser.value = user
            Result.success(AuthUserResult(user, isNewUser = isNew))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUserResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google sign in failed")
            val isNew = result.additionalUserInfo?.isNewUser ?: false
            _currentUser.value = user
            Result.success(AuthUserResult(user, isNewUser = isNew))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(clearData: Boolean) {
        val userId = auth.currentUser?.uid ?: ""
        
        if (clearData) {
            dataClearers.forEach { 
                try {
                    it.clearUserData(userId)
                } catch (e: Exception) {
                    android.util.Log.e("AuthRepository", "Error clearing user data during signOut: ${e.message}", e)
                }
            }
        }

        try {
            auth.signOut()
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error during FirebaseAuth signOut: ${e.message}", e)
        }
        
        try {
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
            // Revoke access as well to force account selection next time
            googleSignInClient.signOut()
            googleSignInClient.revokeAccess()
        } catch (e: Exception) {
            // Ignore if Google Sign In was not used
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user is currently logged in.")
            val userId = user.uid
            
            // Purge remote database data on backend worker first
            try {
                onPurgeRemoteAccount?.invoke()
            } catch (e: Exception) {
                android.util.Log.w("AuthRepository", "Failed to purge remote account data: ${e.message}")
            }

            // Delete user in Firebase Auth
            user.delete().await()
            
            // Clear local user data across repositories
            if (userId.isNotEmpty()) {
                dataClearers.forEach { 
                    try { it.clearUserData(userId) } catch (_: Exception) {}
                }
            }
            
            // Sign out Google Sign-In & Firebase Auth
            signOut(clearData = true)
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            android.util.Log.e("AuthRepository", "Delete account failed: Re-authentication required", e)
            Result.failure(Exception("For security reasons, Firebase requires you to sign out and sign in again before deleting your account."))
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Delete account failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getIdToken(forceRefresh: Boolean): String? {
        return try {
            auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateDisplayName(name: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user logged in")
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = name
            }
            user.updateProfile(profileUpdates).await()
            try {
                user.reload().await()
            } catch (_: Exception) {}
            _currentUser.value = auth.currentUser ?: user
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
