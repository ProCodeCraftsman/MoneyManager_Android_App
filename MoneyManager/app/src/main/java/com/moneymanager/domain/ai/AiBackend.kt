@file:JvmName("AiBackend")

package com.moneymanager.domain.ai

enum class AiBackend(val id: String) {
    AICORE("aicore"),
    LOCAL_MODEL("local_model"),
    NONE("none");

    companion object {
        fun fromId(id: String): AiBackend =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}
