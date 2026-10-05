package com.moneymanager.domain.sms

import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression corpus: every transaction-like template found by the SMS Analyzer on a real inbox
 * (scrubbed, see docs/SMS-ANALYZER.md), re-filled with synthetic values and fed to the engine.
 * Each template must either match a rule or be classified as something that is not a transaction;
 * unmatched generic parses count as gaps.
 */
class SmsCorpusTest {

    private class Template(val sender: String, val count: Int, val text: String)

    private fun load(): List<Template> {
        val json = javaClass.getResourceAsStream("/sms/txn_templates.json")!!.bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Template(o.getString("sender"), o.getInt("count"), o.getString("template"))
        }
    }

    /** Replaces analyzer placeholders with plausible values. */
    private fun fill(template: String): String {
        var s = template
        s = Regex("""\*<N>""").replace(s, "*1234")
        s = Regex("""(?i)\b(card|supercard|ending with|ending|a/c|ac) <N>""").replace(s) { "${it.groupValues[1]} 1234" }
        return s
            .replace("<AMT>", "Rs.1,250.00")
            .replace("<ACC>", "XX1234")
            .replace("<DATE>", "02-10-26")
            .replace("<TIME>", "14:05:33")
            .replace("<NUM>", "123456789012")
            .replace("<ID>", "AB12CD34EF")
            .replace("<N>", "5")
            .replace("<MERCHANT>", "SWIGGY")
            .replace("<NAME>", "RAHUL")
            .replace("<URL>", "https://x.in/a")
            .replace("<VPA>", "abc@okhdfc")
            .replace("<EMAIL>", "a@b.com")
    }

    @Test
    fun `built-in rules cover the real transaction templates`() {
        val templates = load()
        assertTrue("corpus should not be empty", templates.size > 150)

        var total = 0
        var handled = 0
        val gaps = mutableListOf<String>()
        for (t in templates) {
            val parsed = SmsRuleEngine.parse(SmsMessage("AD-${t.sender}-S", fill(t.text)))
            total += t.count
            val ok = parsed.isDefinitelyNotATransaction ||
                (parsed.matched && parsed.category == SmsCategory.TRANSACTION && parsed.isActionableTransaction)
            if (ok) handled += t.count else gaps += "${t.count}x ${t.sender} [${parsed.category}] ${t.text.take(140)}"
        }
        gaps.forEach { println("CORPUS-GAP $it") }
        val pct = handled * 100 / total
        println("CORPUS-COVERAGE $pct% ($handled/$total)")
        assertTrue(
            "Only $pct% of corpus messages handled by rules or classified as non-transactions ($handled/$total). Gaps:\n" +
                gaps.joinToString("\n"),
            pct >= 90
        )
    }
}
