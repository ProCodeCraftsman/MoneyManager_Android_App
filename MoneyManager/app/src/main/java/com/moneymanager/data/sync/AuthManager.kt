package com.moneymanager.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class SignedIn(val user: FirebaseUser) : AuthState()
    data object SignedOut : AuthState()
    data class Error(val message: String) : AuthState()
}

@Singleton
class AuthManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val isSignedIn: Boolean
        get() = firebaseAuth.currentUser != null

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authState.value = auth.currentUser?.let {
                AuthState.SignedIn(it)
            } ?: AuthState.SignedOut
        }
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val user = firebaseAuth.currentUser
        _authState.value = user?.let { AuthState.SignedIn(it) } ?: AuthState.SignedOut
    }

    fun signInWithGoogle(idToken: String, onComplete: (Result<FirebaseUser>) -> Unit) {
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                result.user?.let {
                    _authState.value = AuthState.SignedIn(it)
                    onComplete(Result.success(it))
                } ?: onComplete(Result.failure(Exception("User is null")))
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
                onComplete(Result.failure(e))
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
        _authState.value = AuthState.SignedOut
    }

    fun deleteAccount(onComplete: (Result<Unit>) -> Unit) {
        firebaseAuth.currentUser?.delete()
            ?.addOnSuccessListener { onComplete(Result.success(Unit)) }
            ?.addOnFailureListener { onComplete(Result.failure(it)) }
    }
}
