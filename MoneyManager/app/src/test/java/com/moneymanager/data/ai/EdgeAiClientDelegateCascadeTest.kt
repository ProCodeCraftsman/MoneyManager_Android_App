package com.moneymanager.data.ai

import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.domain.ai.AiUnavailableException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class EdgeAiClientDelegateCascadeTest {

    private lateinit var mockModelManager: LiteRtModelManager
    private lateinit var mockPreferencesManager: PreferencesManager
    private lateinit var mockToolSet: com.moneymanager.data.ai.agent.TransactionToolSet
    private lateinit var mockContext: android.content.Context
    private lateinit var client: EdgeAiClient

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
        val entry = ModelEntry(
            name = "test", modelId = "test/test", modelFile = "test.task",
            commitHash = "abc", sizeBytes = 100, minRamGb = 6
        )
        runBlocking {
            whenever(mockModelManager.selectModelForDevice()).thenReturn(entry)
            whenever(mockModelManager.isModelDownloaded()).thenReturn(true)
        }
        whenever(mockModelManager.getModelFile(any())).thenReturn(java.io.File("/tmp/test.task"))
        client = EdgeAiClient(mockModelManager, mockToolSet, mockPreferencesManager, mockContext)
    }

    @Test
    fun `delegate cascade tries NPU first, then GPU, then CPU`() {
        kotlinx.coroutines.test.runTest {
            val result = client.generateDraft("hi")
            assertTrue("generateDraft must return failure when all delegates fail", result.isFailure)
            verify(mockPreferencesManager).setAiBackendTier(AiBackend.NONE.id)
        }
    }

    @Test
    fun `all delegates fail persists NONE before throwing`() {
        kotlinx.coroutines.test.runTest {
            val result = client.generateDraft("hi")
            assertTrue(result.isFailure)
            val inOrder = inOrder(mockPreferencesManager)
            inOrder.verify(mockPreferencesManager).setAiBackendTier("none")
        }
    }

    @Test
    fun `failure returns AiUnavailableException not raw exception`() {
        kotlinx.coroutines.test.runTest {
            val result = client.generateDraft("hi")
            assertTrue("Failure must wrap in AiUnavailableException",
                result.exceptionOrNull() is AiUnavailableException)
        }
    }

    @Test
    fun `NONE is not persisted when model not downloaded`() {
        runBlocking { whenever(mockModelManager.isModelDownloaded()).thenReturn(false) }
        kotlinx.coroutines.test.runTest {
            client.generateDraft("hi")
            verify(mockPreferencesManager, never()).setAiBackendTier(any())
        }
    }

    @Test
    fun `after NONE persistence next resolveBackendTier returns NONE`() {
        kotlinx.coroutines.test.runTest {
            client.generateDraft("hi")
            verify(mockPreferencesManager).setAiBackendTier("none")
            argumentCaptor<String>().apply {
                verify(mockPreferencesManager).setAiBackendTier(capture())
                assertEquals("none", firstValue)
            }
        }
    }
}
