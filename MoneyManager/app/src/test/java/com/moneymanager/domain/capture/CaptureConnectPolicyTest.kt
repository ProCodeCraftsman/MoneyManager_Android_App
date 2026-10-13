package com.moneymanager.domain.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureConnectPolicyTest {

    @Test
    fun `fresh install shows the connect card and does not scan`() {
        assertTrue(CaptureConnectPolicy.showConnectCard(inboxAvailable = true, readPermitted = false, receivePermitted = false))
        assertFalse(CaptureConnectPolicy.scanOnOpen(inboxAvailable = true, readPermitted = false))
    }

    @Test
    fun `install that granted only READ_SMS still sees the card for the receiver, and scans on open`() {
        assertTrue(CaptureConnectPolicy.showConnectCard(inboxAvailable = true, readPermitted = true, receivePermitted = false))
        assertTrue(CaptureConnectPolicy.scanOnOpen(inboxAvailable = true, readPermitted = true))
    }

    @Test
    fun `both permissions hide the card and scan on open`() {
        assertFalse(CaptureConnectPolicy.showConnectCard(inboxAvailable = true, readPermitted = true, receivePermitted = true))
        assertTrue(CaptureConnectPolicy.scanOnOpen(inboxAvailable = true, readPermitted = true))
    }

    @Test
    fun `a build without an inbox never shows the card or scans, even if permission somehow exists`() {
        assertFalse(CaptureConnectPolicy.showConnectCard(inboxAvailable = false, readPermitted = false, receivePermitted = false))
        assertFalse(CaptureConnectPolicy.showConnectCard(inboxAvailable = false, readPermitted = true, receivePermitted = true))
        assertFalse(CaptureConnectPolicy.scanOnOpen(inboxAvailable = false, readPermitted = true))
    }
}
