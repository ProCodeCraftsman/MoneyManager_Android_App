package com.moneymanager.app.ui.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileHelper {
    fun saveReceipt(context: Context, uri: Uri): String? {
        return try {
            val fileName = "receipt_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, "receipts").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(file, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteReceipt(path: String?) {
        if (path == null) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
