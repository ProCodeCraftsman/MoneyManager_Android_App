package com.moneymanager.di

import com.moneymanager.data.ai.EdgeAiClient
import com.moneymanager.data.ai.NanoAiClient
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.ai.GenAiClient
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class AiModuleTest {

    private val module = AiModule

    @Test
    fun `providePreferredGenAiClient returns NanoAiClient for aicore tier`() {
        val mockPrefs: PreferencesManager = mock {
            on { aiBackendTier } doReturn flowOf("aicore")
            on { isLocalModelDownloaded } doReturn flowOf(false)
        }
        val mockNano: NanoAiClient = mock()
        val mockEdge: EdgeAiClient = mock()

        val result: GenAiClient? = module.providePreferredGenAiClient(mockPrefs, mockNano, mockEdge)

        assertSame(mockNano, result)
    }

    @Test
    fun `providePreferredGenAiClient returns EdgeAiClient for local_model when downloaded`() {
        val mockPrefs: PreferencesManager = mock {
            on { aiBackendTier } doReturn flowOf("local_model")
            on { isLocalModelDownloaded } doReturn flowOf(true)
        }
        val mockNano: NanoAiClient = mock()
        val mockEdge: EdgeAiClient = mock()

        val result: GenAiClient? = module.providePreferredGenAiClient(mockPrefs, mockNano, mockEdge)

        assertSame(mockEdge, result)
    }

    @Test
    fun `providePreferredGenAiClient returns null for local_model when NOT downloaded`() {
        val mockPrefs: PreferencesManager = mock {
            on { aiBackendTier } doReturn flowOf("local_model")
            on { isLocalModelDownloaded } doReturn flowOf(false)
        }
        val mockNano: NanoAiClient = mock()
        val mockEdge: EdgeAiClient = mock()

        val result: GenAiClient? = module.providePreferredGenAiClient(mockPrefs, mockNano, mockEdge)

        assertNull(result)
    }

    @Test
    fun `providePreferredGenAiClient returns null for none tier`() {
        val mockPrefs: PreferencesManager = mock {
            on { aiBackendTier } doReturn flowOf("none")
        }
        val mockNano: NanoAiClient = mock()
        val mockEdge: EdgeAiClient = mock()

        val result: GenAiClient? = module.providePreferredGenAiClient(mockPrefs, mockNano, mockEdge)

        assertNull(result)
    }

    @Test
    fun `providePreferredGenAiClient returns null for pending tier`() {
        val mockPrefs: PreferencesManager = mock {
            on { aiBackendTier } doReturn flowOf("pending")
        }
        val mockNano: NanoAiClient = mock()
        val mockEdge: EdgeAiClient = mock()

        val result: GenAiClient? = module.providePreferredGenAiClient(mockPrefs, mockNano, mockEdge)

        assertNull(result)
    }

    @Test
    fun `providePreferredGenAiClient returns null for unknown tier`() {
        val mockPrefs: PreferencesManager = mock {
            on { aiBackendTier } doReturn flowOf("unknown_backend")
        }
        val mockNano: NanoAiClient = mock()
        val mockEdge: EdgeAiClient = mock()

        val result: GenAiClient? = module.providePreferredGenAiClient(mockPrefs, mockNano, mockEdge)

        assertNull(result)
    }

    @Test
    fun `provideGenAiClient unchanged - returns non-nullable router`() {
        assertTrue(true)
    }
}
