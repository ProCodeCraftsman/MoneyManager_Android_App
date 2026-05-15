package com.moneymanager.app.ui.util

import android.content.ContentResolver
import android.content.Intent
import android.provider.ContactsContract
import android.net.Uri

data class ContactData(
    val displayName: String,
    val lookupKey: String,
    val phoneNumber: String = ""
)

object ContactPickerHelper {
    const val REQUEST_PICK_CONTACT = 1001

    fun createPickIntent(): Intent =
        Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

    /**
     * Queries a contact URI returned by [createPickIntent].
     * Returns display name, lookup key, and phone number (if available).
     * Safe against missing columns and permission denial — returns null gracefully.
     */
    fun queryContactData(contentResolver: ContentResolver, contactUri: Uri): ContactData? {
        return try {
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.LOOKUP_KEY
            )
            contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val keyIdx = cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                    if (nameIdx < 0 || keyIdx < 0 || idIdx < 0) return@use null
                    val contactId = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: "Unknown"
                    val lookupKey = cursor.getString(keyIdx) ?: ""
                    val phone = queryPhoneNumber(contentResolver, contactUri) ?: ""
                    return@use ContactData(name, lookupKey, phone)
                }
                null
            }
        } catch (_: SecurityException) {
            null // URI permission revoked
        } catch (_: IllegalArgumentException) {
            null // invalid URI
        } catch (_: Exception) {
            null // any other query failure
        }
    }

    /**
     * Queries the phone number for a contact using its data sub-URI.
     * Uses the temporary URI permission from ACTION_PICK — no READ_CONTACTS needed.
     * Gracefully returns null if permission is insufficient.
     */
    private fun queryPhoneNumber(
        contentResolver: ContentResolver,
        contactUri: Uri
    ): String? {
        return try {
            val dataUri = Uri.withAppendedPath(contactUri, "data")
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
            )
            contentResolver.query(dataUri, projection, null, null, null)?.use { cursor ->
                var phone = ""
                while (cursor.moveToNext()) {
                    val number = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    ) ?: continue
                    if (phone.isEmpty()) phone = number
                }
                phone
            } ?: ""
        } catch (_: Exception) {
            null
        }
    }

    fun resolveContactUri(lookupKey: String): Uri =
        Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)

    fun doesContactExist(contentResolver: ContentResolver, lookupKey: String): Boolean {
        return try {
            val uri = resolveContactUri(lookupKey)
            contentResolver.query(uri, arrayOf(ContactsContract.Contacts._ID), null, null, null)
                ?.use { it.count > 0 } ?: false
        } catch (_: Exception) {
            false
        }
    }
}
