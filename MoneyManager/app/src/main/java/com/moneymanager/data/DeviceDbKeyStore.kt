package com.moneymanager.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the random, per-device passphrase used to encrypt the Room database at rest.
 *
 * The database file never leaves the device (cross-device restore goes through the JSON
 * export/Drive-backup path instead), so a random key generated on first use and stored
 * Keystore-backed via [EncryptedSharedPreferences] is sufficient - no need to derive it
 * from anything account-related.
 */
@Singleton
class DeviceDbKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Returns the device's DB passphrase, generating and persisting one on first call. */
    fun getOrCreatePassphrase(): String {
        prefs.getString(KEY_PASSPHRASE, null)?.let { return it }

        val randomBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val passphrase = randomBytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_PASSPHRASE, passphrase).apply()
        return passphrase
    }

    companion object {
        private const val PREFS_FILE_NAME = "device_db_key_store"
        private const val KEY_PASSPHRASE = "db_passphrase"
    }
}
