package com.moneymanager.data.ai

import com.moneymanager.domain.ai.AiBackend

@Deprecated("Use domain AiBackend — migrate to com.moneymanager.domain.ai.AiBackend")
enum class AiBackendTier(val id: String, val requiresUserAction: Boolean, val label: String) {
    AICORE("aicore", requiresUserAction = false, label = "Gemini Nano (AICore)"),
    LOCAL_MODEL("local_model", requiresUserAction = true, label = "Gemma 3 1B (Local)"),
    NONE("none", requiresUserAction = false, label = "No AI Available");

    companion object {
        fun fromId(id: String): AiBackendTier =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}
