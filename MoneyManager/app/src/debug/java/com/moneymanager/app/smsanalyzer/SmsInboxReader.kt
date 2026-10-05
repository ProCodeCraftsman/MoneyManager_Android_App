package com.moneymanager.app.smsanalyzer

import android.content.Context
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsInboxReader {

    /** Streams the whole inbox through [SmsAnalyzer]; raw bodies are not kept. */
    suspend fun analyze(
        context: Context,
        names: List<String> = emptyList(),
        draftContext: DraftContext? = null,
        onProgress: (Int) -> Unit = {},
    ): AnalysisReport =
        withContext(Dispatchers.IO) {
            val analyzer = SmsAnalyzer(names, draftContext)
            val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI, projection, null, null, "${Telephony.Sms.DATE} DESC"
            )?.use { c ->
                val a = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val b = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val d = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                var n = 0
                while (c.moveToNext()) {
                    analyzer.add(c.getString(a).orEmpty(), c.getString(b).orEmpty(), c.getLong(d))
                    if (++n % 500 == 0) onProgress(n)
                }
            }
            analyzer.report()
        }
}
