package com.moneymanager.domain.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureConnectPolicyTest {

    @Test
    fun `fresh install in a build with an inbox shows the connect card and does not scan`() {
        assertTrue(CaptureConnectPolicy.showConnectCard(inboxAvailable = true, permitted = false))
        assertFalse(CaptureConnectPolicy.scanOnOpen(inboxAvailable = true, permitted = false))
    }

    @Test
    fun `granted permission hides the card and scans on open`() {
        assertFalse(CaptureConnectPolicy.showConnectCard(inboxAvailable = true, permitted = true))
        assertTrue(CaptureConnectPolicy.scanOnOpen(inboxAvailable = true, permitted = true))
    }

    @Test
    fun `a build without an inbox never shows the card or scans, even if permission somehow exists`() {
        assertFalse(CaptureConnectPolicy.showConnectCard(inboxAvailable = false, permitted = false))
        assertFalse(CaptureConnectPolicy.showConnectCard(inboxAvailable = false, permitted = true))
        assertFalse(CaptureConnectPolicy.scanOnOpen(inboxAvailable = false, permitted = true))
    }
}
