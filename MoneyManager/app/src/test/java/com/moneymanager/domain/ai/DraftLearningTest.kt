package com.moneymanager.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftLearningTest {

    @Test
    fun `expense learns the account it was saved to`() {
        assertEquals(4L, DraftLearning.accountToLearn("expense", 4L, null))
    }

    @Test
    fun `transfer learns the destination, not the source`() {
        assertEquals(9L, DraftLearning.accountToLearn("transfer", 4L, 9L))
    }

    @Test
    fun `transfer without a destination learns nothing`() {
        assertNull(DraftLearning.accountToLearn("transfer", 4L, null))
    }
}
