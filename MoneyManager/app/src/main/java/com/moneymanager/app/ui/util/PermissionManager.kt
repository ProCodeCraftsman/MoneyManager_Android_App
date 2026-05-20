package com.moneymanager.app.ui.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object Permissions {
    val CAMERA = Manifest.permission.CAMERA
    val RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
    val READ_SMS = Manifest.permission.READ_SMS
    val READ_CONTACTS = Manifest.permission.READ_CONTACTS

    val STORAGE: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
