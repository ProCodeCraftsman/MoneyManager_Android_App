package com.moneymanager.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class DriveFile(val id: String, val name: String, val createdTime: String)

/**
 * Communicates with Google Drive REST API v3 using the appDataFolder space.
 * All calls require a valid OAuth access token.
 */
@Singleton
class DriveBackupManager @Inject constructor() {

    /** Uploads [data] to appDataFolder, replacing any existing backup with the same name. */
    suspend fun uploadBackup(data: ByteArray, accessToken: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val boundary = "backup_${System.currentTimeMillis()}"
                val metadata = JSONObject().apply {
                    put("name", BACKUP_FILE_NAME)
                    put("parents", JSONArray().apply { put("appDataFolder") })
                }.toString()

                val conn = openConnection("$UPLOAD_URL?uploadType=multipart", "POST", accessToken)
                conn.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                conn.doOutput = true

                conn.outputStream.use { out ->
                    DataOutputStream(out).apply {
                        writeBytes("--$boundary\r\n")
                        writeBytes("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                        writeBytes(metadata)
                        writeBytes("\r\n--$boundary\r\n")
                        writeBytes("Content-Type: application/octet-stream\r\n\r\n")
                        write(data)
                        writeBytes("\r\n--$boundary--\r\n")
                    }
                }

                check(conn.responseCode in 200..299) {
                    val body = conn.errorStream?.bufferedReader()?.readText() ?: ""
                    "Upload failed (${conn.responseCode}): $body"
                }
                JSONObject(conn.inputStream.bufferedReader().readText()).getString("id")
            }
        }

    /** Finds the most recent backup file in appDataFolder, or null if none exists. */
    suspend fun findBackup(accessToken: String): Result<DriveFile?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val query = URLEncoder.encode(
                    "name='$BACKUP_FILE_NAME' and trashed=false", "UTF-8"
                )
                val url = "$FILES_URL?spaces=appDataFolder&q=$query" +
                    "&fields=files(id,name,createdTime)&orderBy=createdTime+desc&pageSize=1"

                val conn = openConnection(url, "GET", accessToken)
                check(conn.responseCode in 200..299) { "Search failed (${conn.responseCode})" }

                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val files = json.getJSONArray("files")
                if (files.length() == 0) return@runCatching null

                files.getJSONObject(0).let {
                    DriveFile(it.getString("id"), it.getString("name"), it.getString("createdTime"))
                }
            }
        }

    /** Downloads a backup file by [fileId] and returns the raw bytes. */
    suspend fun downloadBackup(fileId: String, accessToken: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = openConnection("$FILES_URL/$fileId?alt=media", "GET", accessToken)
                check(conn.responseCode in 200..299) { "Download failed (${conn.responseCode})" }
                conn.inputStream.readBytes()
            }
        }

    /** Deletes all backup files except [keepFileId] to prevent accumulation. */
    suspend fun deleteOldBackups(keepFileId: String, accessToken: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                val query = URLEncoder.encode(
                    "name='$BACKUP_FILE_NAME' and trashed=false", "UTF-8"
                )
                val url = "$FILES_URL?spaces=appDataFolder&q=$query&fields=files(id)&pageSize=20"
                val conn = openConnection(url, "GET", accessToken)
                if (conn.responseCode !in 200..299) return@runCatching

                val files = JSONObject(conn.inputStream.bufferedReader().readText())
                    .getJSONArray("files")
                for (i in 0 until files.length()) {
                    val id = files.getJSONObject(i).getString("id")
                    if (id != keepFileId) deleteFile(id, accessToken)
                }
            }
        }

    private suspend fun deleteFile(fileId: String, accessToken: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                openConnection("$FILES_URL/$fileId", "DELETE", accessToken).responseCode
            }
        }

    private fun openConnection(url: String, method: String, accessToken: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 30_000
            readTimeout = 60_000
        }

    companion object {
        const val BACKUP_FILE_NAME = "moneymanager_backup.enc"
        private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }
}
