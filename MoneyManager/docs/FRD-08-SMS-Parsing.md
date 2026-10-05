# SMS Transaction Parsing - Functional & Technical Specification

**Feature:** Turn bank, card, UPI and investment SMS into transaction drafts
**Entry point:** AI Draft from SMS (`SmsPickerScreen` -> `AiDraftViewModel.generateDraft(..., "SMS", sender)`)
**Code:** `domain/sms/*`, `domain/ai/AiAgent.kt`, dev tooling in `app/smsanalyzer/*` (+ debug-only screen)
**Status:** Phases A, B and C implemented and unit-tested; real-inbox scan on the author's phone: 1,230/1,230 transaction messages matched by a rule (the last Samsung template has no amount). Draft dry-run (export 6): 0 of 1,228 drafts ready (account matched 67%, category 0%), which led to the history fallback, the Verify step, learned account links and the model category pick (section 7.1). Not yet verified on a device
**Last Updated:** October 2026

---

## 1. Problem

The SMS draft flow used the generic `DeterministicExtractor` and then an on-device LLM for every message. That is slow, needs a downloaded model, and is wrong in predictable ways: it read "credit card" as a credit, treated promo and reminder SMS as transactions, and picked the wrong amount when a balance or limit was present.

Real inbox data (one phone, 6,988 messages, scanned with the analyzer) shows the shape of the problem:

| Fact | Value |
|---|---|
| Messages from non-personal senders | 6,756 |
| Real transaction messages | ~1,170 (17%) |
| Senders that send transactions | 45 |
| Distinct transaction templates | ~190 |
| Volume concentration | 5 senders = ~75% of transaction messages |
| Baseline generic parser: fully parsed | 95% of transaction messages, but only after tuning against this data |

So the vocabulary of transaction SMS is small and stable per sender. A deterministic rule per template family is accurate, instant, offline and free. The LLM is only needed for templates nobody has written a rule for yet.

## 2. Goals and non-goals

**Goals**
1. Parse the common bank / card / UPI / wallet / mutual-fund SMS deterministically into a `TransactionDraft` with amount, direction, date, counterparty, account hint and reference.
2. Reject non-transactions clearly (OTP, promo, reminder, failed payment, informational) instead of drafting garbage.
3. Fall back to the existing AI pipeline for unrecognised templates, passing everything the rules did extract as ground-truth hints. If the AI is unavailable, still offer a reviewable draft from the generic parse.
4. Keep the rule set maintainable: a runbook to find new templates in an inbox and add a rule + test.

**Non-goals (this release)**
- No background SMS listener or auto-import, and no `READ_SMS` in release builds. The user pastes/selects a message as today.
- No automatic categorisation beyond the existing merchant-category memory.
- No cloud processing of SMS.
- No foreign-currency conversion (flag only).

## 3. Architecture

```
Raw SMS (text, optional sender, optional received time)
        |
        v
  SmsRuleEngine.parse()                         domain/sms (pure Kotlin)
    1. OTP guard
    2. sender-scoped rules, then sender-agnostic rules   (BuiltInSmsRules)
    3. generic fallback (SmsFieldExtractor)
        |
        v
  ParsedSms { category, direction, amount, currency, balance, accountHint,
              counterparty, reference, date, nature, ruleId, matched, flags }
        |
        v  AiAgent.processText (sourceType == "SMS")
  +-----------------------------------------------------------------+
  | category OTP/PROMO/REMINDER/FAILED/INFO -> Failure(NotATransaction) |
  | matched && TRANSACTION -> SmsDraftMapper -> merchant memory -> Success (no AI) |
  | otherwise -> DeterministicExtractor + hints -> merchant cache -> AI |
  |              AI failure + generic parse had amount&direction -> fallback draft |
  +-----------------------------------------------------------------+
```

The analyzer is the feedback loop that keeps the rules current:

```
Phone inbox -> SMS Analyzer (debug build) -> masked templates JSON
            -> find gaps -> new rule + test -> release
```

### 3.1 Components

| Component | Responsibility |
|---|---|
| `SmsRule` / `BuiltInSmsRules` | One rule per template family: senders, regex with named groups (`amount`, `cur`, `acc`, `merchant`, `balance`, `ref`, `date`), category, direction, instrument, institution, nature |
| `SmsRuleEngine` | Ordered matching, normalising sender ids (`AD-HDFCBK-S` -> `HDFCBK`), generic fallback, field cleanup |
| `SmsDateParser` | Parses the date formats banks print (`02-10-26`, `30Mar25`, `02/OCT/2026`, ...), falls back to the SMS received time |
| `AccountMatcher` | Maps institution + instrument + last digits to one of the user's accounts, or null when ambiguous |
| `SmsDraftMapper` | `ParsedSms` + user accounts -> `TransactionDraft` (type, transfer, savings, flags, review state) |
| `NotATransactionException` | Carries the reason shown to the user |
| `SmsFieldExtractor` | Generic regex extractor (formerly analyzer-only), used as the engine's last stage and by the analyzer coverage report |

### 3.2 Categories

`TRANSACTION`, `OTP`, `BALANCE`, `REMINDER`, `FAILED`, `PROMO`, `INFO`, `OTHER`.

- `REMINDER`: scheduled AutoPay, statement generated, payment due, money-request notices.
- `FAILED`: declined, unsuccessful, failed payment.
- `INFO`: recognised but nothing to record (refund *initiated*, merchant settlement, EPF passbook, UPI mandate created, "credit balance of").
- `OTHER`/`BALANCE` do not block the flow; a pasted free-text note ("spent 450 on groceries") must still reach the AI.

### 3.3 Nature (how a transaction maps to the app's types)

| Nature | Example | Draft |
|---|---|---|
| `NORMAL` | UPI sent, card spend, salary credit | `expense` / `income`, account matched if possible |
| `REFUND` | Scapia/slice refund | `income`, flag `refund` |
| `CASHBACK` | fuel surcharge waiver, Axis cashback | `income`, flag `cashback` |
| `CARD_REPAYMENT` | "payment received towards your credit card" | `transfer` with `toAccount` = matched card, from-account left for the user. The bank-side debit SMS is a separate message, so it is never recorded as an expense |
| `INVESTMENT` | SIP purchase, NPS contribution, RD deposit | `savings`, `investmentPlatform` = scheme/AMC |

### 3.4 Review state

A rule-derived draft is `needsReview = true` unless both the account and the category resolved. Category comes from the existing merchant-category memory (`MerchantCategoryMemoryRepository.lookup`). The draft never auto-commits unless `TransactionDraft.isHighConfidence()` holds and the caller asked for auto-commit (SMS flow does not).

Foreign currency (`USD 12`) keeps the numeric amount, sets flag `foreign_currency:USD` and `needsReview`.

## 4. Pattern coverage (from the analyzer export)

See `SMS-PATTERN-CATALOG.md` for the per-sender table. Families covered by built-in rules:

1. Credit-card spend, payment received, refund, cashback: Scapia/Federal, HDFC, ICICI, Axis, slice, Utkarsh SuperCard
2. Bank debit/credit: HDFC UPI sent / credit alert / debit update / deposit, SBI (CBS, UPI, NEFT/IMPS, YONO), Federal (UPI, fund transfer)
3. Wallet: IRCTC RWallet
4. Investments: ABSL, ICICI Pru, Tata, SBI, Edelweiss, HDFC, DSP, Motilal mutual funds; NPS (Protean/CRA); RD (cooperative society)
5. One-off merchant receipts: Snitch, Samsung service, PolicyBazaar, Airtel prepaid, Jio Fiber, Income-tax challan, PUC certificate, IT refund
6. Non-transactions: OTP, promos, reminders (AutoPay, statements, due dates, money requests), failed payments, informational

## 5. Privacy and safety

- Release builds do not declare `READ_SMS`. The analyzer screen and the permission live in `app/src/debug`.
- The analyzer stores nothing raw: it streams the inbox through the masker and keeps templates and counters. Export is a user-chosen JSON file.
- The rule engine runs entirely on device and does not log message bodies. Existing AI conversation logging is unchanged.
- Fixtures committed to the repo are masked templates with synthetic values.

## 6. Testing

| Layer | What |
|---|---|
| Unit: per rule | A realistic message per family asserting category, direction, amount, merchant, account hint, nature |
| Unit: corpus | Every transaction template from the analyzer export (scrubbed, placeholders replaced with synthetic values) must either match a rule or be classified as a non-transaction category. Coverage threshold enforced |
| Unit: mapper / matcher | Type and flag mapping per nature, account disambiguation, foreign currency |
| Unit: agent flow | Non-transaction rejection, rule-matched draft without AI, AI-unavailable fallback |
| Device | Paste real messages in AI Draft from SMS; re-run the analyzer after install to confirm coverage. UI is not covered by automated tests |

## 7. Phases

| Phase | Scope | State |
|---|---|---|
| A | Debug-only analyzer: masking, clustering, baseline coverage, JSON export | Done |
| B | Rule engine + built-in rules + draft mapper + account matcher + tests | Done (64 rules, 100% of the scrubbed corpus; device check pending) |
| C | `AiAgent` integration: rejection, rule-first drafts, AI fallback with hints, generic fallback draft | Done (8 flow tests; device check pending) |
| D (later) | Learned rules: AI proposes a rule for a template the analyzer reports as a gap, user approves, rule is stored (JSON) and loaded next to the built-ins | Not started |
| E (later) | Optional import from the inbox in a non-Play distribution | Not started |

### 7.1 Draft resolution: verify, resolve, learn

Rules extract facts; they are not trusted blindly, and the model is used only for narrow decisions. Target device: Gemma 4 E2B (2.4 GB, on-device, Galaxy S24 Ultra), which is reliable at a one-number multiple-choice answer and unreliable at free-form JSON or tool calls. Design rule: **the model never writes ids or free text into a draft; it picks from a list the app built.**

```
rules / generic parse -> Verify -> Resolve account -> Resolve category -> user edits on save -> Learn
```

| Stage | What it does | Code |
|---|---|---|
| Verify | Checks a parse against the message text: amount appears in the text; direction wording does not contradict the parsed direction; merchant is not a stray word or number; date is not in the future. Wrong amount withdraws the parse, so the message falls through to the extractor and model; junk merchant or future date is dropped; direction conflict keeps the draft but marks it `needsReview` with low type confidence. Issues are recorded as `verify:*` flags. This is the format-drift detector: a rule that keeps matching but captures the wrong thing is caught without knowing the template | `SmsVerifier` |
| Resolve account | 1) learned link, 2) institution/last4 name matching, 3) left blank. Never delegated to the model: it cannot know which of the user's accounts is "HDFC" | `SmsAccountLinks`, `AccountMatcher` |
| Resolve category | 1) merchant memory, 2) the user's own transaction history (>=60% majority, same type), 3) on-device model picks from at most 15 of the user's categories of that type, ordered by use; answer must be a number whose name does not contradict. A model pick sets `ai_category`, `needsReview`, medium confidence and is never auto-committed | `MerchantHistoryIndex`, `SmsCategoryPrompt`, `SmsCategoryChooser` |
| Learn | On save of an SMS draft, the account the user kept is stored against the SMS source key (`institution|instrument|last4`); the category is recorded in merchant memory (existing). Both make the next message from that source deterministic | `SmsAccountLinkRepository`, `AddTransactionViewModel.recordSmsAccountLink` |

Account link rules: an exact key match wins; a message with no digits borrows a link only when every link for that institution and instrument points at the same account; digits never seen before never borrow another card's link. A link to an archived or deleted account is ignored. Links live in preferences (not Room), so there is no migration; they are **not part of backups** and rebuild from later saves.

The analyzer scan reports `verifyIssues` (counts per issue, no values) so false positives of Verify on real data are visible, and applies learned links to the draft dry-run.

Not built yet: duplicate detection (bank SMS and card SMS for the same purchase), learned templates (Phase D), per-sender drift alerts, recording which fields the user edited before saving (the "zero-edit rate" metric).

## 8. Open questions

- Should card repayment drafts try to guess the source account from the most recent bank debit of the same amount? (Currently left to the user.)
- Salary detection (`SALARY` in NEFT info) is surfaced as a flag only; mapping to a category is up to the user's category setup.
- Merchant names in UPI messages are often truncated by the bank (`KERALA VISI`); the merchant memory keys on the truncated string.

## 9. Priorities and external reference

**Priority 1 (the committed plan, implemented):** analyzer, per-template rule engine, `AiAgent` integration with AI fallback, docs, corpus regression test. Anything below must not weaken these.

**Priority 2 (ideas taken from `saurabhgupta050890/transaction-sms-parser`, MIT):** reviewed the whole library (~20 KB of TypeScript). It is a token-heuristic parser with no per-sender templates, so it does not replace the rule engine. Only ideas were adopted (no code copied), all in the generic fallback:

| Adopted | Where |
|---|---|
| Wider balance keywords (`avbl credit limit`, `updated/new/total balance`, `limit available`, `avl lmt`, `a/c bal`) | `SmsFieldExtractor` |
| Wallet / card brands as an account hint when no number is printed (Paytm, Simpl, LazyPay, Amazon Pay, slice, Uni, One, Niyo) | `SmsRuleEngine.accountHintFor` |
| Merchant from a VPA (`zomato@hdfcbank` -> `zomato`) when the text names none | `SmsRuleEngine.fromGeneric` |
| Extra debit verbs `charged`, `purchased` | `SmsFieldExtractor` |

| Rejected | Why |
|---|---|
| Debit words checked before credit words, "payment" counted as a debit | "Payment received" would become an expense |
| "Valid if 2 of amount / balance / account" | Rejects complete card messages that carry only amount and merchant (e.g. Scapia) |
| Stripping every `x`, `is `, `with ` from the text | Corrupts ordinary words |
| Token walking instead of templates | Cannot tell promo / reminder / failed from a transaction |

Its test spreadsheet contains one placeholder row, so it adds no corpus. Its sample message is covered by `SmsRuleEngineTest`.

**Priority 3 (not started):** learned rules (Phase D), optional inbox import (Phase E).
