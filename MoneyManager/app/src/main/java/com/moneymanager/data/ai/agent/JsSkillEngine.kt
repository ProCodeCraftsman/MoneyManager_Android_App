package com.moneymanager.data.ai.agent

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "JsSkillEngine"
private const val EXECUTE_TIMEOUT_SECONDS = 15L
private const val INIT_TIMEOUT_SECONDS = 5L

@Singleton
class JsSkillEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val initLatch = CountDownLatch(1)
    private var webView: WebView? = null

    init {
        mainHandler.post { createWebView() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView() {
        webView = WebView(context.applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
        }
        initLatch.countDown()
    }

    fun execute(skillName: String, input: String): String {
        if (!initLatch.await(INIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            return """{"error":"JsSkillEngine failed to initialize"}"""
        }

        val jsCode = loadSkillJs(skillName)
            ?: return """{"error":"Skill '$skillName' not found"}"""

        val latch = CountDownLatch(1)
        val resultRef = AtomicReference<String>()

        val bridge = object {
            @JavascriptInterface
            fun onResult(json: String) {
                Log.d(TAG, "JS skill returned: ${json.take(200)}")
                resultRef.set(json)
                latch.countDown()
            }

            @JavascriptInterface
            fun onError(message: String) {
                Log.w(TAG, "JS skill error: $message")
                resultRef.set("""{"error":"$message"}""")
                latch.countDown()
            }
        }

        mainHandler.post {
            val wv = webView
            if (wv == null) {
                resultRef.set("""{"error":"WebView not available"}""")
                latch.countDown()
                return@post
            }
            wv.addJavascriptInterface(bridge, "AndroidBridge")
            val escaped = input.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")
            val script = """
                (function() {
                    try {
                        $jsCode
                        processInput('$escaped');
                    } catch(e) {
                        AndroidBridge.onError(e.message);
                    }
                })()
            """.trimIndent()
            wv.evaluateJavascript(script, null)
        }

        if (!latch.await(EXECUTE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            return """{"error":"Skill '$skillName' timed out after ${EXECUTE_TIMEOUT_SECONDS}s"}"""
        }

        return resultRef.get() ?: """{"error":"No result from skill"}"""
    }

    private fun loadSkillJs(skillName: String): String? {
        return try {
            context.assets.open("skills/$skillName/skill.js")
                .bufferedReader()
                .readText()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load skill '$skillName'", e)
            null
        }
    }

    fun destroy() {
        mainHandler.post {
            webView?.destroy()
            webView = null
        }
    }
}
