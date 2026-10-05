package com.moneymanager.domain.ai

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsCategoryChooserTest {

    private class FakeClient(private val reply: Result<String>) : GenAiClient {
        val prompts = mutableListOf<String>()
        override suspend fun generateDraft(prompt: String): Result<String> {
            prompts += prompt
            return reply
        }
    }

    private val food = CategoryEntry(1, "Food", "expense")
    private val travel = CategoryEntry(2, "Travel", "expense")
    private val context = PromptContext(
        top20Categories = listOf(food, travel), allCategories = listOf(food, travel, CategoryEntry(9, "Salary", "income")),
        accounts = emptyList(), peers = emptyList(), tags = emptyList(),
    )
    private val draft = TransactionDraft(typeId = "expense", amount = 250.0, merchantHint = "KERALA VISI")

    @Test
    fun `returns the category the model picked`() = runTest {
        val client = FakeClient(Result.success("2. Travel"))
        assertEquals(travel, SmsCategoryChooser(client).choose(draft, context))
        assertTrue(client.prompts.single().contains("KERALA VISI"))
        assertTrue(!client.prompts.single().contains("Salary"))
    }

    @Test
    fun `returns null when the model is unavailable or unclear`() = runTest {
        assertNull(SmsCategoryChooser(FakeClient(Result.failure(AiUnavailableException("no model")))).choose(draft, context))
        assertNull(SmsCategoryChooser(FakeClient(Result.success("probably travel"))).choose(draft, context))
        assertNull(SmsCategoryChooser(FakeClient(Result.success("0"))).choose(draft, context))
    }

    @Test
    fun `does not call the model without a merchant or any category of the type`() = runTest {
        val client = FakeClient(Result.success("1"))
        val chooser = SmsCategoryChooser(client)
        assertNull(chooser.choose(draft.copy(merchantHint = null), context))
        assertNull(chooser.choose(draft.copy(typeId = "savings"), context))
        assertTrue(client.prompts.isEmpty())
    }
}
