package com.moneymanager.data.ai

import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.ai.AiUnavailableException
import com.moneymanager.domain.ai.GenAiClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class EdgeAiClientTest {

    private lateinit var mockModelManager: LiteRtModelManager
    private lateinit var mockPreferencesManager: PreferencesManager
    private lateinit var mockToolSet: com.moneymanager.data.ai.agent.TransactionToolSet
    private lateinit var mockContext: android.content.Context
    private lateinit var client: GenAiClient

    @Before
    fun setUp() {
        mockModelManager = mock()
        mockPreferencesManager = mock()
        mockToolSet = mock()
        mockContext = mock {
            on { applicationInfo } doReturn android.content.pm.ApplicationInfo()
            on { cacheDir } doReturn java.io.File("/tmp/cache")
            on { filesDir } doReturn java.io.File("/tmp/files")
        }
        whenever(mockModelManager.isModelDownloaded()).thenReturn(true)
        client = EdgeAiClient(mockModelManager, mockToolSet, mockPreferencesManager, mockContext)
    }

    @Test
    fun `lazy init — engine is null after construction`() {
        verify(mockModelManager, never()).selectModelForDevice()
    }

    @Test
    fun `generateDraft returns failure when model not downloaded`() {
        whenever(mockModelManager.isModelDownloaded()).thenReturn(false)
        kotlinx.coroutines.test.runTest {
            val result = client.generateDraft("hi")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiUnavailableException)
        }
    }

    @Test
    fun `close is idempotent — calling twice does not crash`() {
        kotlinx.coroutines.test.runTest {
            val edgeClient = client as EdgeAiClient
            edgeClient.close()
            edgeClient.close()
        }
    }

    @Test
    fun `calculateMaxTokens returns at least 512`() {
        val edgeClient = client as EdgeAiClient
        val result = edgeClient.calculateMaxTokens(0, 0)
        assertTrue("calculateMaxTokens(0,0) should be >= 512, was $result", result >= 512)
    }

    @Test
    fun `engine exception returns Result failure not crash`() {
        whenever(mockModelManager.selectModelForDevice()).thenReturn(null)
        kotlinx.coroutines.test.runTest {
            val result = client.generateDraft("hi")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is AiUnavailableException)
        }
    }
}
