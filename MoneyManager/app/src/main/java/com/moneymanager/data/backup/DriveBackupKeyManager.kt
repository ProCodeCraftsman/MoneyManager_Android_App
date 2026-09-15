package com.moneymanager.data.backup

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the real encryption secret for Drive backups: a random 256-bit key generated on
 * first use and stored alongside the backup in Drive's hidden appDataFolder space.
 *
 * Replaces the old scheme of deriving the passphrase from the signed-in Google account's
 * email - that offered no real protection (an email address is public) and fell back to a
 * passphrase hardcoded into the APK when signed out.
 */
@Singleton
class DriveBackupKeyManager @Inject constructor(
    private val driveBackupManager: DriveBackupManager,
) {
    suspend fun getOrCreatePassphrase(accessToken: String): Result<String> {
        val existing = driveBackupManager.findBackup(accessToken, DriveBackupManager.KEY_FILE_NAME)
            .getOrElse { return Result.failure(it) }

        if (existing != null) {
            return driveBackupManager.downloadBackup(existing.id, accessToken).map { it.decodeToString() }
        }

        val passphrase = ByteArray(32).apply { SecureRandom().nextBytes(this) }
            .joinToString("") { "%02x".format(it) }

        return driveBackupManager.uploadBackup(
            passphrase.encodeToByteArray(),
            accessToken,
            DriveBackupManager.KEY_FILE_NAME,
        ).map { passphrase }
    }
}
