# SMS Analyzer Runbook

Debug-only tool that finds the SMS templates in your own inbox and reports how well the rule engine understands them. Use it to decide which rules to add.

## What it is

- A second launcher icon, **SMS Analyzer**, in debug builds only (`app/src/debug/.../SmsAnalyzerActivity.kt`).
- Reads the inbox, masks every message into a template (amounts, dates, accounts, ids, links, merchants and names replaced by placeholders), groups identical templates per sender, runs the generic extractor, and shows coverage.
- `READ_SMS` is declared in `app/src/debug/AndroidManifest.xml` only. Release builds do not contain it.

## Steps

1. Install the debug build from Android Studio (variant `debug`) or `./gradlew.bat installDebug`.
2. Open **SMS Analyzer**.
3. Type your own name words in **Your name words** (every word, including surnames and initials, e.g. `Swaraj K S Surname`). They are masked wherever they appear.
4. **Scan inbox**, grant the SMS permission. On Xiaomi/Oppo/Vivo or Android 13+, enable *Allow restricted settings* in the app info if the permission is greyed out.
5. Review the **parser gaps** list. **Export JSON** to a file.
6. Skim the JSON for leaked personal data before sharing it anywhere.

## Reading the report

| Field | Meaning |
|---|---|
| `category` | `TRANSACTION`, `OTP`, `BALANCE`, `REMINDER`, `FAILED`, `PROMO`, `OTHER` |
| `status` | `FULL` (amount + direction + account/merchant/reference found), `PARTIAL`, `NOT_TRANSACTION` |
| `count` | Messages that share this template |
| `fieldHits` | How many of them yielded each field |

Singleton templates (count 1) are mostly promos, regional-language alerts and free text; ignore them unless they are a bank sender.

## Adding a rule for a gap

1. Pick a `TRANSACTION`/`PARTIAL` template, or a transaction wrongly classified as `PROMO`/`OTHER`.
2. Add a rule to `domain/sms/BuiltInSmsRules.kt` with the template's sender, a regex using the named groups (`amount`, `cur`, `acc`, `merchant`, `balance`, `ref`, `date`), category, direction, instrument, institution, nature.
3. Add a realistic message to `SmsRuleEngineTest` (and the template to the corpus fixture `app/src/test/resources/sms/txn_templates.json`, scrubbed).
4. `./gradlew.bat :app:testDebugUnitTest --tests "com.moneymanager.domain.sms.*" --offline -q`.
5. Update `SMS-PATTERN-CATALOG.md` (rule id, sender, count).
6. Re-run the analyzer after installing to confirm the gap closed.

## Do not

- Do not commit the raw export; commit only scrubbed fixtures.
- Do not generate regex source with shell/Python string replacement (backslash escapes get corrupted). Edit the file directly.
