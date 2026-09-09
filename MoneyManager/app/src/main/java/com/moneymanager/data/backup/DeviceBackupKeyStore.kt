package com.moneymanager.data.backup

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a random, per-device passphrase used to encrypt local (non-Drive) backups.
 *
 * Local backups have no Google account to derive a stable cross-device passphrase from
 * (that's what [EncryptionHelper.getEffectivePassphrase] does for Drive backups), so instead
 * we generate a random 256-bit key on first use and store it Keystore-backed via
 * [EncryptedSharedPreferences]. This key never leaves the device — local backups are a
 * same-device recovery mechanism, not a cross-device restore path.
 */
@Singleton
class DeviceBackupKeyStore @Inject constructor(
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

    /** Returns the device's local-backup passphrase, generating and persisting one on first call. */
    fun getOrCreatePassphrase(): String {
        prefs.getString(KEY_PASSPHRASE, null)?.let { return it }

        val randomBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val passphrase = randomBytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_PASSPHRASE, passphrase).apply()
        return passphrase
    }

    companion object {
        private const val PREFS_FILE_NAME = "device_backup_key_store"
        private const val KEY_PASSPHRASE = "local_backup_passphrase"
    }
}
