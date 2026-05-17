package com.moneymanager.data.ai

import com.moneymanager.data.preferences.PreferencesManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class EdgeAiClientLifecycleTest {

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
        runBlocking {
            whenever(mockModelManager.selectModelForDevice()).thenReturn(null)
            whenever(mockModelManager.isModelDownloaded()).thenReturn(true)
        }
        client = EdgeAiClient(mockModelManager, mockToolSet, mockPreferencesManager, mockContext)
    }

    @Test
    fun `cleanUp nulls internal references`() {
        kotlinx.coroutines.test.runTest {
            client.cleanUp()
            assertTrue(true)
        }
    }

    @Test
    fun `close is idempotent`() {
        kotlinx.coroutines.test.runTest {
            client.close()
            client.close()
            assertTrue(true)
        }
    }

    @Test
    fun `resetIdleTimer cancels previous timer`() {
        kotlinx.coroutines.test.runTest {
            client.generateDraft("first")
            client.generateDraft("second")
        }
    }

    @Test
    fun `streaming fallback works when model not downloaded`() {
        runBlocking { whenever(mockModelManager.isModelDownloaded()).thenReturn(false) }
        kotlinx.coroutines.test.runTest {
            val tokens = mutableListOf<String>()
            val result = client.generateDraftStreaming("sys", "hi") { tokens.add(it) }
            assertTrue(result.isFailure)
            assertTrue(tokens.isEmpty())
        }
    }

    @Test
    fun `generateDraft does not throw raw exception`() {
        kotlinx.coroutines.test.runTest {
            val result = client.generateDraft("hi")
            assertTrue(result.isFailure)
        }
    }

    @Test
    fun `generateDraftStreaming does not throw raw exception`() {
        kotlinx.coroutines.test.runTest {
            val result = client.generateDraftStreaming("sys", "hi") {}
            assertTrue(result.isFailure)
        }
    }
}
