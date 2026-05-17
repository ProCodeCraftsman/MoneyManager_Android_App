package com.moneymanager.data.ai

import android.util.Log
import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.domain.ai.AiUnavailableException
import com.moneymanager.domain.ai.GenAiClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiClientRouter @Inject constructor(
    private val nanoAiClient: NanoAiClient,
    private val edgeAiClient: EdgeAiClient,
    private val modelManager: LiteRtModelManager,
    private val deviceCapabilityManager: DeviceCapabilityManager,
) : GenAiClient {

    companion object {
        private const val TAG = "AiClientRouter"
    }

    // ── Flat-prompt path ──────────────────────────────────────────────────────

    override suspend fun generateDraft(prompt: String): Result<String> {
        return route(
            aicoreFn  = { nanoAiClient.generateDraft(prompt) },
            localFn   = { edgeAiClient.generateDraft(prompt) },
        )
    }

    // ── Agentic path (system instruction + tools) ─────────────────────────────
    //
    // EdgeAiClient overrides this variant to wire systemInstruction into
    // ConversationConfig and attach TransactionToolSet. NanoAiClient falls back to
    // the GenAiClient default which concatenates both strings into a flat prompt.

    override suspend fun generateDraft(
        systemInstruction: String,
        userMessage: String,
    ): Result<String> {
        return route(
            aicoreFn  = { nanoAiClient.generateDraft(systemInstruction, userMessage) },
            localFn   = { edgeAiClient.generateDraft(systemInstruction, userMessage) },
        )
    }

    // ── Streaming path ────────────────────────────────────────────────────────

    override suspend fun generateDraftStreaming(
        systemInstruction: String,
        userMessage: String,
        onToken: (String) -> Unit,
    ): Result<String> {
        return route(
            aicoreFn  = { nanoAiClient.generateDraftStreaming(systemInstruction, userMessage, onToken) },
            localFn   = { edgeAiClient.generateDraftStreaming(systemInstruction, userMessage, onToken) },
        )
    }

    // ── Internal routing ───────────────────────────────────────────────────────

    private suspend fun route(
        aicoreFn: suspend () -> Result<String>,
        localFn:  suspend () -> Result<String>,
    ): Result<String> {
        val currentTier = deviceCapabilityManager.resolveCurrentTier()
        Log.d(TAG, "Routing draft to tier: ${currentTier.name}")

        return when (currentTier) {
            AiBackend.AICORE -> {
                val result = aicoreFn()
                if (result.isFailure) {
                    Log.w(TAG, "AICore failed, falling back to local model")
                    localFn()
                } else result
            }
            AiBackend.LOCAL_MODEL -> {
                if (!modelManager.isModelDownloaded()) {
                    Result.failure(AiUnavailableException("Local model not downloaded yet"))
                } else {
                    localFn()
                }
            }
            AiBackend.NONE -> {
                Result.failure(AiUnavailableException("No AI backend available on this device"))
            }
        }
    }
}
