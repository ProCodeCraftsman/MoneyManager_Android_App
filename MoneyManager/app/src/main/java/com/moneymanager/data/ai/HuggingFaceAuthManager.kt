package com.moneymanager.data.ai

import android.util.Log
import com.moneymanager.data.preferences.PreferencesManager
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Singleton
class HuggingFaceAuthManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
) {
    companion object {
        private const val TAG = "HFAuthManager"
    }

    fun getAccessToken(): String = preferencesManager.getHfAccessTokenSync()

    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        if (token.isEmpty()) return false
        val expiresAt = runCatching {
            runBlocking { preferencesManager.hfTokenExpiresAt.first() }
        }.getOrDefault(0L)
        return if (expiresAt > 0L) System.currentTimeMillis() < expiresAt else true
    }

    fun checkUrlAccess(url: String, accessToken: String? = null): Int {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "MoneyManager/1.0")
            if (accessToken != null && accessToken.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
            }
            connection.requestMethod = "GET"
            connection.connect()
            val code = connection.responseCode
            connection.disconnect()
            code
        } catch (e: Exception) {
            Log.e(TAG, "URL check failed", e)
            -1
        }
    }

    fun needsAuth(url: String): Boolean {
        val code = checkUrlAccess(url)
        return code == HttpURLConnection.HTTP_UNAUTHORIZED ||
            code == HttpURLConnection.HTTP_FORBIDDEN
    }

    /**
     * Returns true if [token] grants access to [url].
     * A 401 or 403 means the token is definitively invalid for this resource.
     * Any other outcome (200, redirect, network error) is treated as "proceed" —
     * the actual download will surface a real error if the server rejects it.
     */
    fun validateToken(url: String, token: String): Boolean {
        val code = checkUrlAccess(url, token)
        return code != HttpURLConnection.HTTP_UNAUTHORIZED &&
            code != HttpURLConnection.HTTP_FORBIDDEN
    }
}
