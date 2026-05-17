package com.moneymanager.data.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.LiteRtLmJniException
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.tool
import com.moneymanager.data.ai.agent.TransactionToolSet
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.domain.ai.AiUnavailableException
import com.moneymanager.domain.ai.GenAiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "EdgeAiClient"
private const val DEFAULT_MAX_TOKENS = 1024

/**
 * GenAiClient backed by LiteRT-LM (litertlm-android:0.11.0).
 *
 * Implements the gallery's agentic pattern:
 *   - [generateDraft(systemInstruction, userMessage)] passes the system instruction directly into
 *     ConversationConfig (not concatenated into the user turn) and attaches [TransactionToolSet]
 *     so the model can call getDefaultAccount / searchCategories / searchPeers / getMerchantCategory
 *     in-flight rather than hallucinating app data from the prompt.
 *   - Delegate cascade: NPU (QNN) → GPU → CPU, same as gallery's LlmChatModelHelper.
 *   - Temperature=0.0 / topK=1 for deterministic JSON output.
 *
 * Call [close] to release ~1.5–2 GB RAM when idle.
 */
@Singleton
class EdgeAiClient @Inject constructor(
    private val modelManager: LiteRtModelManager,
    private val toolSet: TransactionToolSet,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context,
) : GenAiClient {

    @Volatile private var engine: Engine? = null
    @Volatile private var conversation: Conversation? = null
    private var savedEngineConfig: EngineConfig? = null
    private val engineLock = Mutex()

    private val idleScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob()
    )
    private var idleTimerJob: Job? = null
    private val idleTimeoutMs = 300_000L

    // ── GenAiClient: flat-prompt path (NanoAiClient fallback compat) ──────────

    override suspend fun generateDraft(prompt: String): Result<String> {
        if (!modelManager.isModelDownloaded()) {
            return Result.failure(AiUnavailableException("Local model not downloaded"))
        }
        resetIdleTimer()
        return try {
            val raw = runInference(getOrCreateEngine(), systemInstruction = null, userMessage = prompt)
            Log.d(TAG, "Flat-prompt inference OK (${raw.length} chars): ${raw.take(300)}")
            Result.success(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Flat-prompt inference failed", e)
            close()
            Result.failure(AiUnavailableException("Local model error: ${e.message}"))
        }
    }

    // ── GenAiClient: agentic path (system instruction + tools) ───────────────

    /**
     * Routes the system instruction into ConversationConfig and attaches TransactionToolSet so
     * the LLM can resolve accounts, categories, and peers via tool calls instead of guessing.
     * The litertlm runtime handles the tool-call loop automatically (same as gallery AgentChat).
     */
    override suspend fun generateDraft(
        systemInstruction: String,
        userMessage: String,
    ): Result<String> {
        if (!modelManager.isModelDownloaded()) {
            return Result.failure(AiUnavailableException("Local model not downloaded"))
        }
        resetIdleTimer()
        return try {
            val raw = runInference(getOrCreateEngine(), systemInstruction, userMessage)
            Log.d(TAG, "Agentic inference OK (${raw.length} chars): ${raw.take(300)}")
            Result.success(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Agentic inference failed", e)
            close()
            Result.failure(AiUnavailableException("Local model error: ${e.message}"))
        }
    }

    // ── GenAiClient: streaming path ────────────────────────────────────

    override suspend fun generateDraftStreaming(
        systemInstruction: String,
        userMessage: String,
        onToken: (String) -> Unit,
    ): Result<String> {
        if (!modelManager.isModelDownloaded()) {
            return Result.failure(AiUnavailableException("Local model not downloaded"))
        }
        resetIdleTimer()
        return try {
            val raw = runInferenceStreaming(getOrCreateEngine(), systemInstruction, userMessage, onToken)
            Log.d(TAG, "Streaming inference OK (${raw.length} chars)")
            Result.success(raw)
        } catch (e: Exception) {
            Log.e(TAG, "Streaming inference failed", e)
            cleanUp()
            Result.failure(AiUnavailableException("Local model error: ${e.message}"))
        }
    }

    // ── Lifecycle management (KV cache fix) ─────────────────────────────

    /**
     * Sequential two-step teardown matching gallery's LlmChatModelHelper pattern:
     * conversation.close() → engine.close() → null out references.
     * Prevents native INVALID_ARGUMENT crash from KV cache trap.
     */
    private fun cancelIdleTimer() {
        idleTimerJob?.cancel()
        idleTimerJob = null
    }

    private fun resetIdleTimer() {
        idleTimerJob?.cancel()
        idleTimerJob = idleScope.launch {
            delay(idleTimeoutMs)
            cleanUp()
            Log.d(TAG, "Idle timer: cleanUp after 5 min inactivity")
        }
    }

    suspend fun cleanUp() {
        cancelIdleTimer()
        engineLock.withLock {
            try { conversation?.close() } catch (e: Exception) { Log.e(TAG, "Conversation close error", e) }
            conversation = null
            try { engine?.close() } catch (e: Exception) { Log.e(TAG, "Engine close error", e) }
            engine = null
            savedEngineConfig = null
            Log.d(TAG, "Clean up done.")
        }
    }

    /** Alias for backward compatibility. Calls [cleanUp]. */
    suspend fun close() {
        cancelIdleTimer()
        cleanUp()
    }

    /**
     * Resets the engine session by rebuilding from saved config.
     * Gallery pattern: close conversation → close engine → rebuild from saved config.
     */
    suspend fun resetConversation(
        systemInstruction: String? = null,
        tools: Boolean = true,
    ) {
        engineLock.withLock {
            try { conversation?.close() } catch (e: Exception) { Log.e(TAG, "Conversation close error", e) }
            conversation = null
            try { engine?.close() } catch (e: Exception) { Log.e(TAG, "Engine close error", e) }
            engine = null
            val cfg = savedEngineConfig
                ?: throw IllegalStateException("No saved engine config — call buildEngine first")
            val eng = Engine(cfg)
            eng.initialize()
            engine = eng
            Log.d(TAG, "Engine rebuilt from saved config")
        }
    }

    /**
     * Configurable max tokens with adaptive sizing based on input length.
     * Longer input → more tokens for output.
     */
    fun calculateMaxTokens(inputLength: Int, userMaxTokens: Int = DEFAULT_MAX_TOKENS): Int {
        val adaptive = inputLength * 4 + 512
        return minOf(maxOf(adaptive, 512), userMaxTokens.coerceAtLeast(DEFAULT_MAX_TOKENS))
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private suspend fun getOrCreateEngine(): Engine {
        engine?.let { return it }
        return engineLock.withLock {
            engine ?: buildEngine().also { engine = it }
        }
    }

    /**
     * NPU → GPU → CPU fallback chain. Mirrors gallery's LlmChatModelHelper backend selection.
     */
    private suspend fun buildEngine(): Engine {
        val model = modelManager.selectModelForDevice()
            ?: throw AiUnavailableException("Device RAM too low for local model")
        val modelPath = modelManager.getModelFile(model).absolutePath
        Log.d(TAG, "Initializing Engine: ${model.modelFile} at $modelPath")

        val backends = listOf(
            Backend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir ?: ""),
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
                    maxNumTokens = DEFAULT_MAX_TOKENS,
                    cacheDir = context.cacheDir.absolutePath,
                )
                val eng = Engine(cfg)
                eng.initialize()
                savedEngineConfig = cfg
                Log.d(TAG, "Engine ready — backend: $backend")
                return eng
            } catch (e: Throwable) {
                Log.w(TAG, "Backend $backend failed: ${e.message}")
            }
        }
        preferencesManager.setAiBackendTier(AiBackend.NONE.id)
        Log.e(TAG, "All backends failed — persisted NONE")
        throw AiUnavailableException("All backends failed (NPU, GPU, CPU)")
    }

    /**
     * Single inference call.
     *
     * When [systemInstruction] is non-null the conversation is configured with:
     *   - systemInstruction wired into ConversationConfig (not prepended to user turn)
     *   - TransactionToolSet attached so the model can make tool calls in-flight
     *
     * When [systemInstruction] is null falls back to a flat prompt with no tools (backward compat).
     */
    private suspend fun runInference(
        eng: Engine,
        systemInstruction: String?,
        userMessage: String,
    ): String {
        val sysContents = systemInstruction?.let { Contents.of(listOf(Content.Text(it))) }
        val tools = if (systemInstruction != null) listOf(tool(toolSet)) else emptyList()

        val conv = eng.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
                systemInstruction = sysContents,
                tools = tools,
                automaticToolCalling = true,
            )
        )
        conversation = conv

        val sb = StringBuilder()
        try {
            conv.sendMessageAsync(
                Contents.of(listOf(Content.Text(userMessage))),
                emptyMap(),
            ).collect { message ->
                sb.append(message.toString())
            }
        } catch (e: LiteRtLmJniException) {
            Log.e(TAG, "Native inference error", e)
            throw e
        } catch (e: CancellationException) {
            Log.w(TAG, "Inference cancelled, returning partial result")
        } finally {
            try { conv.close() } catch (_: Exception) {}
            conversation = null
        }
        return sb.toString()
    }

    private suspend fun runInferenceStreaming(
        eng: Engine,
        systemInstruction: String?,
        userMessage: String,
        onToken: (String) -> Unit,
    ): String {
        val sysContents = systemInstruction?.let { Contents.of(listOf(Content.Text(it))) }
        val tools = if (systemInstruction != null) listOf(tool(toolSet)) else emptyList()

        val conv = eng.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(topK = 1, topP = 1.0, temperature = 0.0),
                systemInstruction = sysContents,
                tools = tools,
                automaticToolCalling = true,
            )
        )
        conversation = conv

        val sb = StringBuilder()
        try {
            conv.sendMessageAsync(
                Contents.of(listOf(Content.Text(userMessage))),
                emptyMap(),
            ).collect { message ->
                val token = message.toString()
                sb.append(token)
                onToken(token)
            }
        } catch (e: LiteRtLmJniException) {
            Log.e(TAG, "Native streaming error", e)
            throw e
        } catch (e: CancellationException) {
            Log.w(TAG, "Streaming cancelled, returning partial result")
        } finally {
            try { conv.close() } catch (_: Exception) {}
            conversation = null
        }
        return sb.toString()
    }
}
