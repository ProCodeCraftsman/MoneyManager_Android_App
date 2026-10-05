package com.moneymanager.data.repository

import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.sms.SmsAccountLinks
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers which account the user files each SMS source under. Kept in preferences rather than the database:
 * it is a learned convenience that rebuilds itself from the next saves, so it needs no migration. It is not
 * part of backups.
 */
@Singleton
class SmsAccountLinkRepository @Inject constructor(private val preferences: PreferencesManager) {

    suspend fun load(): SmsAccountLinks = SmsAccountLinks(preferences.getSmsAccountLinks())

    suspend fun learn(linkKey: String, accountId: Long) {
        if (accountId > 0) preferences.setSmsAccountLink(linkKey, accountId)
    }
}
