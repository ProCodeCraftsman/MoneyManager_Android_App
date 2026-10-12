# F07 Automatic capture and draft inbox

State: In progress (E1 done, E2 built, E4 built, E5 to E7 built with gaps, see phases) | Packages: `app/src/personal` (new flavor), `domain/ai`, `domain/sms`, `data/` (ledger table), `app/ui/capture` (new hub + inbox), `app/ui/aidraft` | Depends on: F01, F04 (AI-010), F06. Learning and audit on top of this: F08.

## Goal and non-goals
Goal: bank, card and UPI SMS become reviewable drafts (amount, account, category, date, short note) without copy-paste. Duplicates of one spend collapse into one draft. The user clears the day's drafts in seconds, in bulk or one by one. Non-goals: auto-saving (policy stays review, see F04), shipping `READ_SMS`/`RECEIVE_SMS` in the Play release.

What decides success, in order: capture completeness, zero duplicates, learned memory, a review screen that takes under 10 s a day. The model only fills gaps (E10); it is not the core of this feature.

## Current state
- No automatic capture. `SmsPickerScreen` reads the inbox (latest 50), but `READ_SMS` is declared only in `app/src/debug`.
- Drafts are navigation arguments, never persisted. `captured_messages` (E2) has a writer now: the inbox scanner (E4), rules only, no UI yet.
- The AI entry point is the **Add from…** hub (AI-056) on `TransactionsScreen`; the old dropdown is gone. The inbox, account sources and edit stats are reached from there.
- Learning on save exists: `AddTransactionViewModel.recordMerchantCategory` and `recordSmsAccountLink`. It is the only writer of merchant memory: `AiAgent` no longer records the model's category before the user saves (AI-006, fixed 2026-10-11).
- DB is SQLCipher-encrypted with a Keystore-bound key (see Risks).

## Design
```
Trigger (app open | reminder worker | manual "Scan now")
  -> Inbox scan (cursor window) -> Ledger gate -> Filter -> Parse+Verify -> Resolve account -> Resolve category (memory, history)
  -> Semantic dedup -> Draft inbox (persisted)
  -> [later, batched] model best-guess for still-missing fields
  -> Banner / notification -> Review (bulk or single) -> Save -> Learn
```

### Capture: pull scan, not a receiver (first)
- **Flavor.** The `personal` product flavor declares `READ_SMS`; the debug and release manifests are unchanged. Verify that the merged release manifest has no SMS permission. Share-sheet text works in every flavor (E3).
- **Scan window.** Lower bound = max(stored cursor minus a 2 h overlap, user-chosen start date, now minus 31 days). Delayed or out-of-order SMS inside the overlap are not missed; the ledger hash makes the overlap harmless. Built oldest first (see Decisions log), so the cap never skips messages.
  - Hard cap: 31 days or 500 messages per run. Cursor = newest message date fully processed; a capped run leaves it at the 500th message so the next run continues.
  - The cursor (date of the newest scanned message) lives in DataStore.
- **Anchor stop (fallback only).** With no cursor (first run, after a restore, or cursor lost), stop after 3 consecutive transaction SMS that are already in the ledger or match a saved transaction. Not the primary rule: a past spend logged on Cash may never match, so the anchor can be missing.
- **Speed.** A scan runs only the rule engine, `SmsVerifier`, memory and history; no model. About 100 messages takes well under a second, so it can run on every app open.
- **Real-time receiver.** `RECEIVE_SMS` is deferred to E11. Pull scans plus reminders cover the daily habit without background-execution fragility on Samsung.

### Ledger gate
- Table `captured_messages` stores: message hash, reference-number hash, amount, timestamp, status (pending / saved / rejected / merged), transaction id, and the pending draft as JSON. No raw text.
- A rescan never recreates a draft, and a rejected message stays rejected.

### Semantic dedup
- **Match priority:**
  1. Reference number.
  2. Same amount + same account or last4 within 48 h + merchant.
- **Compared against** pending drafts and saved transactions, including manual entries and generated recurring/EMI rows.
- **Merge:** keep the richest message as the draft and attach the others as sources. An own-account debit + credit pair becomes one transfer.
- **Small round amounts** need a reference or merchant match.
- **Uncertain matches are shown, not hidden:** "Looks like ₹450 Swiggy entered manually at 13:10 — Merge / Keep both".

### Account
- Resolved in order: learned links, then name match, then the **Account Sources** screen. That screen does a one-time mapping of each discovered `institution|instrument|last4` to an account, with a "create account" option, and applies it to all pending drafts from that source.
- Balance cross-check: the SMS "available balance" against the app's account balance confirms a mapping or flags a missed transaction.

### Missing fields: best guess, then verify (E10)
- Deterministic first.
- When a field is still empty, the model may propose a value only by choosing from an app-built numbered list:
  - category;
  - account, on the setup screen only;
  - merchant spelling from the user's own history.
- Every proposal is checked against the source text or the user's data; a proposal that cannot be verified is dropped.
- A kept proposal sets `needsReview` and a flag (`ai_category`, `ai_account`, ...).
- Amounts, dates and ids are never model output.
- Runs batched after a scan (GPU/NPU backend through `EdgeAiClient`, per-call timeout). The inbox row shows "suggesting…" until it finishes; it never blocks the inbox.

### Note
- A deterministic template, e.g. "Swiggy · UPI · HDFC ••1234".
- If a template field is missing, the model does not free-write it; the note simply leaves that part out.

### Capture hub (replaces the AI dropdown)
- The `AutoAwesome` button on `TransactionsScreen` opens a full **Add from…** screen. It is not branded "AI".
- Primary tile: **SMS inbox**, with the pending count and the last scan time; tapping it runs a scan, then opens the inbox.
- Secondary tiles: Receipt, Voice, Paste text. Later tiles from F08: Learn from history, Check these.

### Draft inbox (the screen that decides the feature)
- **Layout.** Grouped by day. Each row shows amount, account, merchant, date, and a category chip labelled with its source: *learned* (memory/history), *suggested* (model), *missing*. Duplicate and verify flags appear as badges.
- **Bulk approve.** "Approve N ready" saves only rows where every field is deterministic or learned. Rows with a model suggestion or a missing field are never pre-ticked: bulk approve must not rubber-stamp guesses.
- **Inline fix.** Tapping the category chip opens the top 5 categories for that type plus the model suggestion; one tap, no full form. Tapping the row opens the full draft editor.
- **Reject.** Swipe to reject; the ledger marks the message as rejected. Rejecting the same sender + rule id twice offers "Always ignore these". That ignore list lives in DataStore and is exported in the JSON settings block.
- **Undo** snackbar after a bulk approve; the undo deletes the created rows and puts the ledger rows back to pending.
- **Learning.** Every save teaches merchant memory and account links (the existing hooks), and edit tracking records which fields changed (AI-014).

### Prompting the user
- **App open.** A silent scan, then a non-blocking banner on the home screen: "4 new from SMS · Review · ✕". The ✕ hides the banner for today; "Don't show this" in its menu turns it off. The SMS tile in the hub still shows the count.
- **Reminders.** Around noon and around 21:00, an inexact alarm (`setWindow`, 30-min window) starts a worker. The worker scans first and notifies only if the pending count is above zero ("3 new spends to review"). A count of zero means no notification.
  - Settings: on/off and the two times.
  - The alarms are rescheduled on `BOOT_COMPLETED` and when the app updates.
  - Needs the `POST_NOTIFICATIONS` runtime permission, asked when reminders are first enabled.
  - A one-time tip asks the user to set battery to "Unrestricted" and turn off "Put unused apps to sleep" (Samsung kills background work otherwise). The scan on app open is the safety net.
- **No exact alarms.** `SCHEDULE_EXACT_ALARM` is off by default on Android 14+, and `USE_EXACT_ALARM` is restricted by Play policy.
- Nothing touches balances before approval.

## Phases
| Phase | Scope | Exit check | Status |
|---|---|---|---|
| E1 | Aggregate data profile of the real DB (counts only) to size sources, accounts, dup rates | Numbers recorded in this file | Done (see Data profile) |
| E2 | `captured_messages` table (ledger and pending draft in one), migration 18->19, JSON/Drive + CSV backup of it plus learned SMS links and merchant memory (AI-013) | Round-trip tests; migration applied on the phone with the JSON backup in hand | Built (unit tests; migration not yet run on the device) |
| E3 | Share-sheet target feeding the draft flow | Unit tests + phone | Not started |
| E4 | `personal` flavor + inbox scanner (cursor window, anchor fallback, caps), rules only, writes pending ledger rows | Scanner unit tests incl. out-of-order and no-cursor cases; merged release manifest has no SMS permission; phone | Built (9 scanner/planner unit tests; play release manifest has no SMS permission; not run on the phone: no trigger yet, arrives with AI-058) |
| E5 | Semantic dedup (F06 D1 + ref matching + transfer pairing, against manual/recurring/EMI) | Unit tests incl. repeat-payment cases | Built (AI-054, 2026-10-12): ref, saved, pending and uncertain matches; 11 dedup + 4 scanner unit tests. Not built: transfer pairing, check at save time. Not on the phone |
| E6 | Account Sources screen (map / create account) + balance cross-check | Phone, real sources | Built (AI-055, 2026-10-12): map or create, applied to pending drafts; grouping unit-tested. Balance cross-check not built. Not on the phone |
| E7 | Capture hub screen (replaces the dropdown) + draft inbox with the bulk-approve rules, inline category fix, reject / always-ignore, undo | Phone: a week of real use, zero duplicate drafts, daily review under 10 s | Built (AI-056, 2026-10-12): hub, inbox, bulk approve of ready rows, undo, reject, merge / keep both, category fix. Not built: always-ignore, paste-text tile. Not on the phone |
| E8 | Scan on app open + home banner with dismiss / don't show | Phone | Not started |
| E9 | Noon / evening reminder worker: scan first, notify only if count > 0, reboot reschedule, notification permission, battery tip | Notifications seen on the S24 over 3 days | Not started |
| E10 | Model best-guess for missing fields, batched after scan (multiple choice, verify, timeout, parser tests) | Unit tests; E2B check on phone; kill rule from AI-004 respected | Not started |
| E11 | Real-time `RECEIVE_SMS` receiver into the same scanner | Phone | Idea |

## Data profile (from the 2026-10-05 JSON backup; counts only, no names or values read)
- 18 accounts: 11 savings/bank, 6 credit, 1 cash; 2 archived. No account name carries a 3-4 digit run, so last4 cannot be matched by name; learned links / Account Sources are the only reliable mapping.
- 1,716 transactions over ~6 months: 1,409 expense, 254 savings, 42 transfer, 5 income, 6 lend/borrow. About 5.3 per day, of which about 3.3 per day are on non-cash accounts (the SMS-capturable share): a daily inbox of roughly 3 drafts.
- The Cash account holds 68% of past expenses (1,083 rows). Many real spends are logged by hand against Cash, so an SMS for the same spend lands on a bank/card account: dedup must also compare across accounts against manual entries.
- Only 4 of 1,414 expense/income rows have no category; 144 categories exist, so the model's multiple-choice list must be a shortlist (frequency + merchant memory), not all 144.
- Description is empty on 77% of rows, note on 11%: the note is the field that matters, description is not. Consequence: history lookup by description (`MerchantHistoryIndex`) rarely matches a bank merchant string; F08 L1 bootstraps memory by joining SMS to saved transactions instead.
- 85 same-account/same-amount pairs within 48 h, 77 with identical description, 74 of them on Cash and 46 at or under 100: genuine repeats (daily tea). Amount + window alone would flag these falsely; need ref or merchant match (non-cash pairs: 11).
- 17 EMI-linked and 242 recurring-flagged rows, 12 active recurring definitions, 8 active EMIs: SMS for these will duplicate generated rows, so dedup checks them too.
- No expense/income mirror pairs across accounts; own-account moves are already modelled as transfers.
- Not available: learned links and merchant memory are not in the JSON backup (AI-013 gap confirmed); rows carry no source type, so there is no baseline of SMS-derived vs manual entries. `defaultAccounts` is empty.

## Acceptance
- Zero duplicate drafts for one spend across bank + card + UPI messages on a week of real data.
- Zero missed bank/card SMS transactions over that week (compare the inbox against the bank app).
- At least 70% of drafts approved with zero edits, per source (needs AI-014).
- Daily review under 10 s for about 3 drafts.
- No raw SMS stored; release manifest free of SMS permissions.

## Risks and unknowns
- **The DB file backup is not portable.** The SQLCipher passphrase sits in Keystore-backed `EncryptedSharedPreferences` (`DeviceDbKeyStore`). A copy of `moneymanager.db` taken over adb cannot be opened off-device and is useless after an uninstall or "clear data". The real safety copy is the in-app JSON/Drive backup.
- "Allow restricted settings" is needed for sideloaded builds that hold SMS permissions.
- Dedup false positives on repeated equal payments.
- Bulk approve invites rubber-stamping; the "never pre-tick a guess" rule is the mitigation and must not be relaxed for convenience.
- Samsung app sleeping and battery limits stop the reminder worker; scan on app open is the safety net.
- The scan cursor is in DataStore and not backed up: after a restore the anchor fallback and the start date bound the scan, and the ledger hash prevents re-creating drafts.
- Schema change touches real, critical data: rehearse the migration on a restored copy first.

## Unverified on device
Everything.

## Decisions log
- 2026-10-05: `personal` build flavor with RECEIVE_SMS and READ_SMS approved by the user (sole user, sideloaded). Release stays clean.
- 2026-10-05: note is a deterministic template; model best-guess only fills missing fields by multiple choice and must pass verification.
- 2026-10-05: the Cash account was a stand-in for UPI-linked bank accounts; the user will create the real bank accounts and archive the old ones. So no cross-account matching against Cash history is required: an SMS draft maps to its real account. Consequences: Account Sources must offer "create account" for unmapped sources, new accounts need a correct opening balance or the balance cross-check will flag them, and the catch-up scan must start from a chosen date, never backfill the whole inbox.
- 2026-10-05: one table `captured_messages` instead of separate ledger and draft tables: the draft is a nullable JSON column cleared once the user decides. Fewer joins, one migration. Schema v19 is additive (new table and 3 indexes only); the SQL was checked against Room's generated `19.json`.
- 2026-10-05: backups. JSON (also used by Drive and local backup) carries the full ledger including pending drafts, merchant memory, and SMS links under `settings.smsAccountLinks`, all by preserved ids, and each block is optional and non-fatal on restore. The CSV bundle carries links by account name, merchant memory by category name+type, and only decided ledger rows (no pending drafts, no transaction ids); a pending message is just captured again. Merchant memory whose category is gone and links whose account is gone are dropped on export/import.
- 2026-10-05: persist a hash ledger, never raw SMS text.
- 2026-10-11: capture is a pull scan (app open, noon/evening reminder, manual) bounded by a stored cursor and the user's start date; the user's "scan until a few already-recorded ones" idea is kept as the fallback stop when there is no cursor. Real-time receiver deferred (E11).
- 2026-10-11: reminders scan first and notify only with a non-zero count; inexact `setWindow` alarms, no exact-alarm permission.
- 2026-10-11: the on-open prompt is a non-blocking banner with dismiss / don't show, not a dialog.
- 2026-10-11: the AI dropdown becomes a full "Add from…" capture hub; SMS inbox is the primary tile. Not labelled "AI".
- 2026-10-11: bulk approve covers only fully deterministic/learned rows; model suggestions are never pre-ticked.
- 2026-10-11: the model's category guess must not be written to merchant memory before the user saves (AI-006); memory learns from saves only (README principle 5).
- 2026-10-11 (E4 built): the window is read oldest first, not newest first as first planned. A capped newest-first read would leave the cursor past messages it never read; oldest first with the cursor at the last message read cannot skip anything. The anchor walk (no cursor) stays newest first, because it has to stop early.
- 2026-10-11 (E4 built): the `personal` flavor shares the `com.moneymanager.app` applicationId with `play`, so switching builds keeps the data. `play` is the default flavor; `personal` gets the READ_SMS manifest and the real `SmsInbox`, while `play` binds an inbox that always returns nothing.
- 2026-10-12 (E5 built): dedup order is reference number, then saved transactions, then pending drafts, then uncertain. A saved match needs the same type, amount within 48 h, the same account (or an unknown draft account) and a merchant match of at least four letters. Amount and window without a merchant match are shown as "maybe duplicate", never hidden. A saved transaction can be claimed by one ledger row only, so repeat payments do not hide each other.
- 2026-10-12 (E5 gap): transfer pairing (own-account debit and credit as one transfer) is not built. Without it, both sides of an own-account move appear as separate drafts.
- 2026-10-12 (E6 built): sources are mapped by the existing SMS account links (`PreferencesManager`), not a new table. A created account opens at zero; the balance cross-check needs the SMS balance kept on the draft, which is not built.
- 2026-10-12 (E7 built): only expense and income rows with a category, an account, no flags and no open duplicate question are approved in bulk. Transfers, lend/borrow and anything with a flag always need a look. Undo covers the last approve action only; it deletes the created transactions, reverses their balances, and puts the rows back in the inbox.
- 2026-10-12 (E7 gap): the always-ignore list and the paste-text tile are not built. The SMS tile opens the inbox, which scans on open; there is no separate scan step before it.
- 2026-10-12 (AI-014 built): edit tracking stores field names and counts per source in DataStore, not in Room, so it needs no migration and is not in backups. Only saves from a capture source are counted.
