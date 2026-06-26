package com.moneymanager.data.ai

import android.util.Log
import com.moneymanager.data.preferences.PreferencesManager
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

enum class HfTokenValidation { VALID, INVALID_TOKEN, ACCESS_DENIED, NETWORK_ERROR }

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
            connection.requestMethod = "HEAD"
            connection.instanceFollowRedirects = false
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
     * Checks whether [token] grants access to [url].
     * Uses HEAD + no-redirect so the HuggingFace auth response is inspected directly
     * rather than the S3 CDN response after a 302 redirect.
     */
    fun validateToken(url: String, token: String): HfTokenValidation {
        return when (checkUrlAccess(url, token)) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> HfTokenValidation.INVALID_TOKEN
            HttpURLConnection.HTTP_FORBIDDEN -> HfTokenValidation.ACCESS_DENIED
            -1 -> HfTokenValidation.NETWORK_ERROR
            else -> HfTokenValidation.VALID
        }
    }
}
