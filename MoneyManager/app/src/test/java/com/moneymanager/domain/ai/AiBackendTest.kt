package com.moneymanager.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiBackendTest {

    @Test
    fun `enum has exactly 3 entries in correct order`() {
        val entries = AiBackend.entries
        assertEquals(3, entries.size)
        assertEquals(
            listOf(AiBackend.AICORE, AiBackend.LOCAL_MODEL, AiBackend.NONE),
            entries
        )
    }

    @Test
    fun `id values match the data layer AiBackendTier strings`() {
        assertEquals("aicore", AiBackend.AICORE.id)
        assertEquals("local_model", AiBackend.LOCAL_MODEL.id)
        assertEquals("none", AiBackend.NONE.id)
    }

    @Test
    fun `fromId returns correct entry for known ids`() {
        assertEquals(AiBackend.AICORE, AiBackend.fromId("aicore"))
        assertEquals(AiBackend.LOCAL_MODEL, AiBackend.fromId("local_model"))
        assertEquals(AiBackend.NONE, AiBackend.fromId("none"))
    }

    @Test
    fun `fromId returns NONE for unknown id`() {
        assertEquals(AiBackend.NONE, AiBackend.fromId("unknown"))
        assertEquals(AiBackend.NONE, AiBackend.fromId(""))
        assertEquals(AiBackend.NONE, AiBackend.fromId("AICORE"))
    }

    @Test
    fun `fromId is case sensitive - lowercase only`() {
        assertEquals(AiBackend.NONE, AiBackend.fromId("Aicore"))
        assertEquals(AiBackend.NONE, AiBackend.fromId("AICORE"))
    }

    @Test
    fun `all values are distinct`() {
        val ids = AiBackend.entries.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }
}
