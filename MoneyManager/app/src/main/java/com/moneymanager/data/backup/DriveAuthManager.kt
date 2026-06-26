package com.moneymanager.data.backup

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.moneymanager.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class DriveAuthState {
    data object Idle : DriveAuthState()
    data object Loading : DriveAuthState()
    data class SignedIn(
        val email: String,
        val displayName: String?,
        val photoUrl: String?,
    ) : DriveAuthState()
    data object SignedOut : DriveAuthState()
    /** Drive scope consent is needed — launch [pendingIntent] via StartIntentSenderForResult. */
    data class NeedsAuthorization(val pendingIntent: PendingIntent) : DriveAuthState()
    data class Error(val message: String) : DriveAuthState()
}

/**
 * Handles Google Sign-In via Credential Manager and requests the Drive appDataFolder
 * OAuth scope via Identity.getAuthorizationClient.
 *
 * The access token is held in memory only (expires ~1 hr). Call [getSilentAccessToken]
 * from a WorkManager worker to silently refresh it without UI.
 */
@Singleton
class DriveAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _authState = MutableStateFlow<DriveAuthState>(DriveAuthState.Idle)
    val authState: StateFlow<DriveAuthState> = _authState.asStateFlow()

    private var _accessToken: String? = null
    val accessToken: String? get() = _accessToken

    private var pendingEmail: String? = null
    private var pendingDisplayName: String? = null
    private var pendingPhotoUrl: String? = null

    val isSignedIn: Boolean get() = _authState.value is DriveAuthState.SignedIn

    /**
     * Launches the Google Sign-In UI and, if successful, requests Drive scope.
     * If scope consent is needed, the state transitions to [DriveAuthState.NeedsAuthorization].
     * Must be called from a coroutine with an Activity context (Credential Manager requires it).
     */
    suspend fun signIn(activity: Activity) {
        _authState.value = DriveAuthState.Loading
        try {
            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activity.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val idTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

            requestDriveScope(
                email = idTokenCredential.id,
                displayName = idTokenCredential.displayName,
                photoUrl = idTokenCredential.profilePictureUri?.toString()
            )
        } catch (e: GetCredentialCancellationException) {
            _authState.value = DriveAuthState.Idle
        } catch (e: GetCredentialException) {
            _authState.value = DriveAuthState.Error(e.message ?: "Sign-in failed")
        } catch (e: Exception) {
            _authState.value = DriveAuthState.Error(e.message ?: "Unexpected sign-in error")
        }
    }

    private suspend fun requestDriveScope(email: String, displayName: String?, photoUrl: String?) {
        val authRequest = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()

        try {
            val result = Identity.getAuthorizationClient(context).authorize(authRequest).await()
            when {
                result.hasResolution() -> {
                    pendingEmail = email
                    pendingDisplayName = displayName
                    pendingPhotoUrl = photoUrl
                    _authState.value = DriveAuthState.NeedsAuthorization(result.pendingIntent!!)
                }
                result.accessToken != null -> {
                    _accessToken = result.accessToken
                    _authState.value = DriveAuthState.SignedIn(email, displayName, photoUrl)
                }
                else -> _authState.value = DriveAuthState.Error("No access token received from Drive")
            }
        } catch (e: Exception) {
            _authState.value = DriveAuthState.Error(e.message ?: "Drive authorization failed")
        }
    }

    /**
     * Called after the user completes the Drive consent screen.
     * Pass the Intent from the StartIntentSenderForResult callback.
     */
    suspend fun handleAuthorizationData(data: Intent?) {
        try {
            val result = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(data)

            _accessToken = result.accessToken
            if (_accessToken != null) {
                _authState.value = DriveAuthState.SignedIn(
                    email = pendingEmail ?: "",
                    displayName = pendingDisplayName,
                    photoUrl = pendingPhotoUrl
                )
            } else {
                _authState.value = DriveAuthState.Error("Authorization was denied")
            }
        } catch (e: Exception) {
            _authState.value = DriveAuthState.Error(e.message ?: "Failed to complete authorization")
        } finally {
            pendingEmail = null
            pendingDisplayName = null
            pendingPhotoUrl = null
        }
    }

    /**
     * Attempts a silent token refresh using Identity.getAuthorizationClient.
     * Returns the access token if successful, or null if user interaction is required.
     * Safe to call from a WorkManager worker (no UI).
     */
    suspend fun getSilentAccessToken(): String? {
        val accessToken = _accessToken
        if (accessToken != null) return accessToken

        val authRequest = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()

        return try {
            val result = Identity.getAuthorizationClient(context).authorize(authRequest).await()
            if (!result.hasResolution()) {
                _accessToken = result.accessToken
                result.accessToken
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun signOut() {
        _accessToken = null
        _authState.value = DriveAuthState.SignedOut
        pendingEmail = null
        pendingDisplayName = null
        pendingPhotoUrl = null
    }

    fun clearError() {
        if (_authState.value is DriveAuthState.Error) {
            _authState.value = DriveAuthState.Idle
        }
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
