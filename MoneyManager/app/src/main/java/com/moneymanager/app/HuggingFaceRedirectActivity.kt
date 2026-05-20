package com.moneymanager.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class HuggingFaceRedirectActivity : Activity() {
    companion object {
        private const val TAG = "HFRedirect"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        if (data != null) {
            Log.d(TAG, "Redirect received: $data")
            val code = data.getQueryParameter("code")
            if (code != null) {
                Log.d(TAG, "Authorization code received")
            }
            val error = data.getQueryParameter("error")
            if (error != null) {
                Log.e(TAG, "Authorization error: $error")
            }
        }
        finish()
    }
}
