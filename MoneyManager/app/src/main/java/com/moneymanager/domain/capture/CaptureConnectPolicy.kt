package com.moneymanager.domain.capture

/**
 * First-run SMS connect (AI-079, AI-060). Nothing is read until READ_SMS is granted, and the live receiver needs
 * RECEIVE_SMS too. The card stays until both are granted, so an install that granted READ_SMS before the receiver
 * existed is asked once more. A build without an inbox never shows it.
 */
object CaptureConnectPolicy {
    fun showConnectCard(inboxAvailable: Boolean, readPermitted: Boolean, receivePermitted: Boolean): Boolean =
        inboxAvailable && !(readPermitted && receivePermitted)

    /** The scan on app open only runs once READ_SMS exists; without it the scan would silently read nothing. */
    fun scanOnOpen(inboxAvailable: Boolean, readPermitted: Boolean): Boolean = inboxAvailable && readPermitted
}
