package com.moneymanager.di

import com.moneymanager.data.ai.AiClientRouter
import com.moneymanager.domain.ai.GenAiClient
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class AiModuleTest {

    private val module = AiModule

    @Test
    fun `provideGenAiClient returns the router`() {
        val mockRouter: AiClientRouter = mock()
        val result: GenAiClient = module.provideGenAiClient(mockRouter)
        assertSame(mockRouter, result)
    }
}
