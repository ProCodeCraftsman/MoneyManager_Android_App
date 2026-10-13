package com.moneymanager.domain.capture

/**
 * First-run SMS connect (AI-079). Nothing is read until the user grants the inbox permission, so the connect card
 * is the only way a fresh install starts capturing. A build without an inbox never shows it.
 */
object CaptureConnectPolicy {
    fun showConnectCard(inboxAvailable: Boolean, permitted: Boolean): Boolean = inboxAvailable && !permitted

    /** The scan on app open only runs once the permission exists; without it the scan would silently read nothing. */
    fun scanOnOpen(inboxAvailable: Boolean, permitted: Boolean): Boolean = inboxAvailable && permitted
}
