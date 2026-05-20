package com.moneymanager.app.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.moneymanager.data.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object FileHelper {

    private const val MAX_DIMENSION = 1024
    private const val JPEG_QUALITY = 80

    fun saveReceipt(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
            val dest = File(dir, "receipt_${UUID.randomUUID()}.jpg")

            val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: return null

            val scaled = scaledDown(bitmap)
            FileOutputStream(dest).use { out -> scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out) }
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()

            dest.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun scaledDown(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return src
        val ratio = MAX_DIMENSION.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(src, (w * ratio).toInt(), (h * ratio).toInt(), true)
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

    fun deleteReceiptsForTransaction(transaction: TransactionEntity) {
        deleteReceipt(transaction.receiptPath)
    }
}
