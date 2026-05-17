package com.moneymanager.data.ai

import android.app.ActivityManager
import android.content.Context
import com.moneymanager.data.preferences.PreferencesManager
import com.google.mlkit.genai.common.FeatureStatus
import com.moneymanager.domain.ai.AiBackend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class DeviceCapabilityManagerTest {

    private lateinit var mockPreferencesManager: PreferencesManager
    private lateinit var mockModelManager: LiteRtModelManager
    private lateinit var mockContext: Context
    private lateinit var manager: DeviceCapabilityManager

    @Before
    fun setUp() {
        mockPreferencesManager = mock()
        mockModelManager = mock()
        mockContext = mock()

        val memoryInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 8L * 1024 * 1024 * 1024
        }
        val mockActivityManager: ActivityManager = mock()
        whenever(mockContext.getSystemService(Context.ACTIVITY_SERVICE))
            .thenReturn(mockActivityManager)
        whenever(mockActivityManager.getMemoryInfo(any())).then {
            val info = it.getArgument<ActivityManager.MemoryInfo>(0)
            info.totalMem = memoryInfo.totalMem
        }

        manager = DeviceCapabilityManager(
            mockPreferencesManager, mockModelManager, mockContext
        )
    }

    @Test
    fun `RAM below 6GB returns NONE immediately`() {
        val lowMemInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 4L * 1024 * 1024 * 1024
        }
        val mockActivityManager: ActivityManager = mock()
        whenever(mockContext.getSystemService(Context.ACTIVITY_SERVICE))
            .thenReturn(mockActivityManager)
        whenever(mockActivityManager.getMemoryInfo(any())).then {
            val info = it.getArgument<ActivityManager.MemoryInfo>(0)
            info.totalMem = lowMemInfo.totalMem
        }

        runTest {
            val result = manager.resolveBackendTier()
            assertEquals(AiBackend.NONE, result)
        }
    }

    @Test
    fun `RAM exactly 6GB allows local model check`() {
        val memInfo = ActivityManager.MemoryInfo().apply {
            totalMem = 6L * 1024 * 1024 * 1024
        }
        val mockActivityManager: ActivityManager = mock()
        whenever(mockContext.getSystemService(Context.ACTIVITY_SERVICE))
            .thenReturn(mockActivityManager)
        whenever(mockActivityManager.getMemoryInfo(any())).then {
            val info = it.getArgument<ActivityManager.MemoryInfo>(0)
            info.totalMem = memInfo.totalMem
        }

        runBlocking {
            doReturn(mock<ModelEntry>()).whenever(mockModelManager).selectModelForDevice()
            doReturn(true).whenever(mockModelManager).isModelDownloaded()
        }

        runTest {
            val result = manager.resolveBackendTier()
            assertEquals(AiBackend.LOCAL_MODEL, result)
        }
    }

    @Test
    fun `no model selected for device returns NONE`() {
        runBlocking { doReturn(null).whenever(mockModelManager).selectModelForDevice() }

        runTest {
            val result = manager.resolveBackendTier()
            assertEquals(AiBackend.NONE, result)
        }
    }

    @Test
    fun `resolveCurrentTier reads from DataStore stored value`() {
        whenever(mockPreferencesManager.aiBackendTier).thenReturn(flowOf("aicore"))

        runTest {
            val result = manager.resolveCurrentTier()
            assertEquals(AiBackend.AICORE, result)
        }
    }

    @Test
    fun `resolveCurrentTier calls resolveBackendTier when stored is pending`() {
        whenever(mockPreferencesManager.aiBackendTier).thenReturn(flowOf("pending"))

        runTest {
            val result = manager.resolveCurrentTier()
            assertEquals(AiBackend.NONE, result)
        }
    }

    @Test
    fun `resolveCurrentTier returns LOCAL_MODEL from DataStore`() {
        whenever(mockPreferencesManager.aiBackendTier).thenReturn(flowOf("local_model"))

        runTest {
            val result = manager.resolveCurrentTier()
            assertEquals(AiBackend.LOCAL_MODEL, result)
        }
    }

    @Test
    fun `resolveCurrentTier returns NONE from DataStore`() {
        whenever(mockPreferencesManager.aiBackendTier).thenReturn(flowOf("none"))

        runTest {
            val result = manager.resolveCurrentTier()
            assertEquals(AiBackend.NONE, result)
        }
    }

    @Test
    fun `resolveCurrentTier returns NONE for unknown stored value`() {
        whenever(mockPreferencesManager.aiBackendTier).thenReturn(flowOf("unknown"))

        runTest {
            val result = manager.resolveCurrentTier()
            assertEquals(AiBackend.NONE, result)
        }
    }

    @Test
    fun `FeatureStatus AVAILABLE code 3 returns AICORE — regression guard`() {
        manager.aicoreCodeOverrideForTest = FeatureStatus.AVAILABLE
        runTest {
            val result = manager.resolveBackendTier()
            assertEquals(AiBackend.AICORE, result)
        }
        manager.aicoreCodeOverrideForTest = null
    }

    @Test
    fun `FeatureStatus UNAVAILABLE code 0 falls through to local model tier`() {
        manager.aicoreCodeOverrideForTest = FeatureStatus.UNAVAILABLE
        runBlocking {
            doReturn(mock<ModelEntry>()).whenever(mockModelManager).selectModelForDevice()
            doReturn(true).whenever(mockModelManager).isModelDownloaded()
        }
        runTest {
            val result = manager.resolveBackendTier()
            assertEquals(AiBackend.LOCAL_MODEL, result)
        }
        manager.aicoreCodeOverrideForTest = null
    }
}
