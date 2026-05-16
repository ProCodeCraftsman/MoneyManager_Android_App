package com.moneymanager.data.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.moneymanager.domain.ai.AiUnavailableException
import com.moneymanager.domain.ai.GenAiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "EdgeAiClient"

/**
 * GenAiClient backed by LiteRT-LM (litertlm-android:0.11.0).
 *
 * Lazy-initializes the Engine on first generateDraft() call and reuses it for
 * subsequent calls. Delegate cascade mirrors the gallery app: NPU (QNN) → GPU → CPU.
 * Temperature=0.0 / topK=1 for deterministic JSON output required by DraftParser.
 *
 * Call close() to release ~1.5–2 GB RAM when the model is idle.
 */
@Singleton
class EdgeAiClient @Inject constructor(
    private val modelManager: LiteRtModelManager,
    @ApplicationContext private val context: Context,
) : GenAiClient {

    // Lazy singleton Engine — created once, reused across calls
    @Volatile private var engine: Engine? = null
    private val engineLock = Any()

    override suspend fun generateDraft(prompt: String): Result<String> {
        if (!modelManager.isModelDownloaded()) {
            return Result.failure(AiUnavailableException("Local model not downloaded"))
        }
        return try {
            val eng = getOrCreateEngine()
            val raw = runInference(eng, prompt)
            Log.d(TAG, "Local inference OK: ${raw.take(120)}…")
            Result.success(raw)
        } catch (e: AiUnavailableException) {
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            // Release broken engine so next call retries initialization
            close()
            Result.failure(AiUnavailableException("Local model error: ${e.message}"))
        }
    }

    /** Release the Engine from RAM. Safe to call from any thread. */
    fun close() {
        synchronized(engineLock) {
            try { engine?.close() } catch (e: Exception) { Log.e(TAG, "Engine close error", e) }
            engine = null
            Log.d(TAG, "Engine released")
        }
    }

    // ── Private ──────────────────────────────────────────────────────────────

    private fun getOrCreateEngine(): Engine {
        engine?.let { return it }
        return synchronized(engineLock) {
            engine ?: buildEngine().also { engine = it }
        }
    }

    /**
     * Build Engine with NPU → GPU → CPU delegate cascade.
     * Mirrors gallery's LlmChatModelHelper backend selection.
     */
    private fun buildEngine(): Engine {
        val model = modelManager.selectModelForDevice()
            ?: throw AiUnavailableException("Device RAM too low for local model")
        val modelPath = modelManager.getModelFile(model).absolutePath
        Log.d(TAG, "Initializing Engine for ${model.filename} at $modelPath")

        // Try NPU first (Snapdragon QNN), then GPU, then CPU — first that succeeds wins.
        val backends = listOf(
            Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir),
            Backend.GPU(),
            Backend.CPU(),
        )
        for (backend in backends) {
            try {
                val cfg = EngineConfig(
                    modelPath = modelPath,
                    backend = backend,
                    visionBackend = null,
                    audioBackend = null,
                    maxNumTokens = 1024,
                )
                val eng = Engine(cfg)
                eng.initialize()
                Log.d(TAG, "Engine initialized with backend: $backend")
                return eng
            } catch (e: Exception) {
                Log.w(TAG, "Backend $backend failed: ${e.message}")
            }
        }
        throw AiUnavailableException("All delegates failed (NPU, GPU, CPU)")
    }

    private suspend fun runInference(eng: Engine, prompt: String): String =
        suspendCoroutine { cont ->
            try {
                val conversation = eng.createConversation(
                    ConversationConfig(
                        // temperature=0.0 / topK=1 → greedy / deterministic JSON output
                        samplerConfig = SamplerConfig(
                            topK = 1,
                            topP = 1.0,
                            temperature = 0.0,
                        ),
                        systemInstruction = null,
                        tools = emptyList(),
                    )
                )

                val sb = StringBuilder()
                conversation.sendMessageAsync(
                    Contents.of(listOf(Content.Text(prompt))),
                    object : MessageCallback {
                        override fun onMessage(message: Message) {
                            sb.append(message.toString())
                        }

                        override fun onDone() {
                            try { conversation.close() } catch (_: Exception) {}
                            cont.resume(sb.toString())
                        }

                        override fun onError(throwable: Throwable) {
                            try { conversation.close() } catch (_: Exception) {}
                            if (throwable is CancellationException) {
                                cont.resume(sb.toString())
                            } else {
                                cont.resumeWithException(throwable)
                            }
                        }
                    },
                    emptyMap(),
                )
            } catch (e: Exception) {
                cont.resumeWithException(e)
            }
        }
}
