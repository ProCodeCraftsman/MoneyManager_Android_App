package com.moneymanager.domain.sms

/**
 * Merchant key shared by memory, history and the SMS join (F08 L4). Banks print the same shop with different
 * noise around it (`POS 123456 SWIGGY BLR`, `paytmqr5ojiw8@paytm`), so the noise is removed before the key is
 * built. The key format is unchanged (lowercase alphanumeric, max 40), so existing rows keep working.
 */
object MerchantKey {
    private const val MAX = 40

    /** `POS 123456 `, `TID 0012 `, `Terminal ID ABC1 `. The id token must contain a digit, so `POS SWIGGY` is kept. */
    private val terminalPrefix = Regex("""^(?:pos|tid|terminal(?:\s+id)?)\s+[\w/-]*\d[\w/-]*\s+""", RegexOption.IGNORE_CASE)

    /** UPI QR payee ids: `paytmqr…`, `q123456…@ybl`. */
    private val qrToken = Regex("""^(?:paytmqr[a-z0-9]+|q\d{6,}[a-z0-9]*)(?:@\S+)?$""", RegexOption.IGNORE_CASE)

    /** Long digit runs are transaction or phone ids, never a payee name. */
    private val longNumber = Regex("""^\d{6,}(?:@\S+)?$""")

    /** `[#*]12`, `*0456`: store or till numbers at the end of a name. */
    private val storeRef = Regex("""^[#*]\d+$""")

    /** City and country words banks append to a merchant name. Dropped only from the end of the name. */
    private val citySuffixes = setOf(
        "bangalore", "bengaluru", "blr", "mumbai", "delhi", "new delhi", "chennai", "hyderabad", "kochi", "cochin",
        "pune", "kolkata", "gurgaon", "gurugram", "noida", "trivandrum", "thiruvananthapuram", "kozhikode",
        "calicut", "ahmedabad", "jaipur", "india", "ind", "in",
    )

    /**
     * Stable key for a bank-printed merchant string. A name that is only noise (`POS 1234`) comes out empty
     * or short, and callers treat a key under three characters as no key.
     */
    fun canonical(raw: String): String {
        var s = raw.trim().replace(Regex("""\s+"""), " ")
        s = terminalPrefix.replace(s, "")
        val tokens = s.split(' ')
            .filter { it.isNotEmpty() && !qrToken.matches(it) && !longNumber.matches(it) }
            .map { token -> token.substringBefore('@').takeIf { it.any(Char::isLetter) } ?: token }
            .toMutableList()
        while (tokens.size > 1 && (storeRef.matches(tokens.last()) || tokens.last().lowercase() in citySuffixes)) {
            tokens.removeAt(tokens.lastIndex)
        }
        return alnum(tokens.joinToString(" "))
    }

    /** The key used before [canonical] existed. Reads fall back to it so memory written earlier still matches. */
    fun legacy(raw: String): String = alnum(raw)

    private fun alnum(s: String): String = s.lowercase().replace(Regex("""[^a-z0-9]"""), "").take(MAX)
}
