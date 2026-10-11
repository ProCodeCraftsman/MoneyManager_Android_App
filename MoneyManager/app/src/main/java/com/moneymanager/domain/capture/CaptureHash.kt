package com.moneymanager.domain.capture

import java.security.MessageDigest

/** Hashes that stand in for SMS content in the ledger. Raw text and reference numbers are never stored. */
object CaptureHash {
    fun message(sender: String?, body: String, receivedAt: Long): String =
        sha256("${sender.orEmpty()}\u0000$body\u0000$receivedAt")

    fun reference(reference: String): String = sha256("ref\u0000${reference.trim().uppercase()}")

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
