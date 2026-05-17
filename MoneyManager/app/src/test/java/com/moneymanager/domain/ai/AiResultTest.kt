package com.moneymanager.domain.ai

import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResultTest {

    @Test
    fun `Loading is a singleton`() {
        assertTrue(AiResult.Loading === AiResult.Loading)
    }

    @Test
    fun `Success holds data`() {
        val result: AiResult<String> = AiResult.Success("hello")
        assertEquals("hello", (result as AiResult.Success).data)
    }

    @Test
    fun `Error holds exception`() {
        val ex = RuntimeException("test")
        val result: AiResult<String> = AiResult.Error(ex)
        assertEquals("test", (result as AiResult.Error).exception.message)
    }

    @Test
    fun `Success and Error are different types`() {
        val success: AiResult<Int> = AiResult.Success(42)
        val error: AiResult<Int> = AiResult.Error(RuntimeException("fail"))
        assertTrue(success is AiResult.Success)
        assertTrue(error is AiResult.Error)
        assertTrue(success !is AiResult.Error)
        assertTrue(error !is AiResult.Success)
        assertTrue(success !is AiResult.Loading)
        assertTrue(error !is AiResult.Loading)
    }

    @Test
    fun `when expression covers all three branches`() {
        val results: List<AiResult<String>> = listOf(
            AiResult.Loading,
            AiResult.Success("ok"),
            AiResult.Error(RuntimeException("err"))
        )
        val labels = results.map { r ->
            when (r) {
                is AiResult.Loading -> "loading"
                is AiResult.Success -> "success"
                is AiResult.Error -> "error"
            }
        }
        assertEquals(listOf("loading", "success", "error"), labels)
    }

    @Test
    fun `generateDraftWithProgress emits Loading then result`() {
        val client = object : GenAiClient {
            override suspend fun generateDraft(prompt: String): Result<String> =
                Result.success("result:$prompt")
        }
        kotlinx.coroutines.test.runTest {
            val emissions = client.generateDraftWithProgress("hi")
                .toList()
            assertEquals(2, emissions.size)
            assertTrue(emissions[0] is AiResult.Loading)
            assertEquals("result:hi", (emissions[1] as AiResult.Success<String>).data)
        }
    }

    @Test
    fun `generateDraftWithProgress emits Error on failure`() {
        val client = object : GenAiClient {
            override suspend fun generateDraft(prompt: String): Result<String> =
                Result.failure(RuntimeException("fail"))
        }
        kotlinx.coroutines.test.runTest {
            val emissions = client.generateDraftWithProgress("x")
                .toList()
            assertEquals(2, emissions.size)
            assertTrue(emissions[0] is AiResult.Loading)
            assertTrue(emissions[1] is AiResult.Error)
        }
    }
}
