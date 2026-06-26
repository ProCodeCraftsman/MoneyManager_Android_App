package com.moneymanager.data.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the backup passphrase securely on-device using EncryptedSharedPreferences
 * backed by an Android Keystore AES-256-GCM master key.
 *
 * This passphrase is device-specific and used only for the background WorkManager sync.
 * For cross-device restore, the user always enters the passphrase manually.
 */
@Singleton
class BackupPassphraseStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "backup_passphrase_prefs",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun savePassphrase(passphrase: String) {
        prefs.edit().putString(KEY_PASSPHRASE, passphrase).apply()
    }

    fun getPassphrase(): String? = prefs.getString(KEY_PASSPHRASE, null)

    fun clearPassphrase() {
        prefs.edit().remove(KEY_PASSPHRASE).apply()
    }

    companion object {
        private const val KEY_PASSPHRASE = "backup_passphrase"
    }
}
