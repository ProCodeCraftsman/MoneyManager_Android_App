# F08 Learning from history, background audit, AI-assisted setup

State: Planned | Packages: `domain/learn` (new, pure), `domain/audit` (new, pure), `domain/ai` (narrow choosers), `data/repository`, `app/ui/capture` | Depends on: F07 E4 (scanner), F04 (AI-010), F05 (AI-014)

## Goal and non-goals
Goal: use the on-device model where it saves the user real effort or catches real mistakes, at moments that are not latency-critical. These are one-time setup, one-time bootstrap and a background second look; not "AI on every entry". Three tracks:
- **L, learn from history:** fill the merchant memory and account links from the user's existing SMS and saved transactions, so capture is accurate from day one instead of after weeks of corrections.
- **A, background audit:** after entries are saved (by the user, by capture, by recurring), look for likely mistakes and ask the user to confirm a fix.
- **O, AI-assisted setup:** for a new user or a fresh start, propose accounts, a category set, merchant labels, recurring items and budgets from their history.

Non-goals:
- The model writing amounts, dates or ids.
- Model-orchestrated tool calling.
- Auto-fixing saved transactions; every change is a user tap.
- Re-running setup on every entry.

## Current state
- Merchant memory (`MerchantCategoryMemoryRepository`, exact normalised key) and SMS account links (DataStore) learn only from saves.
- `MerchantHistoryIndex` matches SMS merchants against transaction descriptions, which are empty on 77% of rows (F07 data profile), so it rarely helps.
- The model category pick (`SmsCategoryChooser`) is the only model use on the SMS path.
- There is no post-save checking of any kind, and no onboarding flow for accounts or categories.

## Design

### Why the model is used here and not per entry
- **Per entry (F07):** the user waits on every draft, so latency is paid daily. Most answers repeat what memory already knows.
- **Here:** the work is batched, runs once or overnight, and progress is visible. A 1–2 s model call per item is fine for 30 items once.
- **Shadow mode first.** The model's judgement is logged without being shown, until a measured precision bar is met (Acceptance).
- **No tool calling.** The model never calls tools, and E2B tool-call output is not trusted (README principle 2). Instead the app runs a fixed set of deterministic checks and lookups, the "tools", and gives the model at most one multiple-choice question about their result.

### L: learn from history (bootstrap)
- **L1 SMS ↔ transaction join (deterministic, no model).**
  - **Scan.** Scan SMS over a user-chosen window (default 6 months) with the F07 scanner in a read-only mode: rules + Verify, no ledger writes.
  - **Match.** For each actionable debit or credit, find saved transactions with the same amount, a compatible type, and a date within ±1 day, on **any** account (past spends sit on Cash).
  - **Keep or skip.** Keep only unique matches; two or more candidates means skip.
  - **Label.** Each kept match is a labelled pair: SMS merchant key → the user's category, and SMS source key → account when the matched row is on a non-cash account.
  - **Aggregate.** Group per key and accept a label when count ≥ 2 and share ≥ 0.6 (same bar as `MerchantHistoryIndex`).
  - This bridges the gap that blocks category today: bank merchant strings never appear in what the user types, but the amount and date tie them together.
  - **Side output.** A coverage figure: the share of past non-cash transactions that had a matching SMS. That is the expected capture recall, measured before building the inbox UI.
- **L2 confirm screen.** "Learned from your history": merchant → category rows with counts, grouped by category.
  - Pre-ticked when count ≥ 3 and the label is unanimous; the user confirms in bulk.
  - Confirmed rows are written to merchant memory and links.
  - Nothing is written without this screen.
- **L3 teach screen (model assists).** The top 30 SMS merchants (by frequency or spend) that are still unlabelled after L1/L2, each with a category chip.
  - The model pre-suggests a category from the user's shortlist (same chooser and parser as `SmsCategoryChooser`), batched in a background worker with progress. Suggestions are marked *suggested* and never pre-ticked.
  - One tap confirms, and a confirmed row is written to memory.
  - Typical cost: 30 calls × about 1–2 s on GPU, run once.
- **L4 merchant key normalisation (deterministic).** Run on both lookup and record:
  - strip POS / terminal prefixes and city suffixes;
  - collapse UPI handles to a stable payee: drop numeric QR ids such as `paytmqr…`, `q123…@ybl`, and use the payee name from the SMS when present;
  - handle bank truncation by prefix.
  - Needs a migration-free re-key of existing memory: compute on read, keep the old key as an alias.
- **L5 corrections replace memory.** When a save changes a learned category, the memory entry moves to the new category after 2 consistent corrections (1 if the old one came from a model suggestion). Today a single different save can thrash it. Edit tracking (AI-014) supplies the signal.
- Privacy: the bootstrap reads SMS into memory only. Nothing raw is persisted; the outputs are memory entries and links, already in the backup formats (AI-013).

### A: background audit ("second look")
- **When.** A worker runs nightly when the device is idle and charging, and on demand from the hub ("Check my entries"). It looks at transactions created or edited in the last 30 days, every source including manual. That matters: 1,409 of the 1,716 rows are manual, and SMS capture never looks at them.
- **Checks.** Each is a pure function with unit tests, reading only the DB and learned data:

  | Check | Signal | Example finding |
  |---|---|---|
  | A1 category conflict | Memory or history strongly says X (count ≥ 3, share ≥ 0.8), saved as Y | "Swiggy is usually Food, this one is Fuel" |
  | A2 amount slip | Amount = 10× or 1/10 of the merchant's median (n ≥ 5), or ≥ 5× the category's monthly p95 | "₹4,500 at Chai Point, usually ₹45" |
  | A3 type / category mismatch | Income category on an expense, a transfer to the same account, a savings row with no destination | |
  | A4 account mismatch | The row came from SMS whose source link points to another account; a new entry on an archived account | |
  | A5 duplicate after the fact | Two saved rows, same amount + merchant within the F06 window, not a known repeat pattern | |
  | A6 balance drift | Latest SMS "available balance" for a linked source differs from the app balance | "HDFC is ₹1,200 lower than your bank says: a missed spend?" |
  | A7 date slip | Future date, or more than 2 days from the source SMS date | |
  | A8 leftovers | `needsReview` or no category after 3 days | |

- **Model role (A9, optional).** Only for transactions with no learned signal (A1 cannot fire):
  - **Question:** "Merchant `<key>`, filed under `<category>`. Pick: 1 fits, 2 does not fit, 3 unsure"; when the answer is 2, a second question chooses an alternative from the shortlist.
  - **Shown to the user** only when the answer is "does not fit" with an alternative that is also in the user's top 20 for that type.
  - **Shadow mode first:** findings are logged as counts only and go live per the Acceptance bar.
  - Strict parser, timeout, parser tests.
- **UX.** A "Check these" section in the inbox and a hub tile; never a notification by itself (it rides along with the F07 reminder if there are drafts too).
  - At most 3 findings a day, highest confidence first.
  - Each has a one-tap fix ("Change to Food", "Merge", "Change account") or "It's correct".
  - "It's correct" stores a dismissal fingerprint (check + transaction id, or check + merchant key + category for A1/A9). The same pattern is not raised again; for A1 it also teaches memory that both categories are valid for that merchant.
- **Storage.** No new table. Findings are recomputed each run. Dismissal fingerprints are kept as a small set in DataStore and exported in the JSON settings block (CSV bundle: not needed, they are hints).

### O: AI-assisted setup (new user or fresh start)
- **Input.** The SMS inbox (personal flavor), with consent and a chosen window. In a Play build, an imported bank-statement CSV instead (the release has no SMS permission).
- **O1 accounts (deterministic).** Distinct `institution|instrument|last4` sources with counts become proposed accounts ("HDFC card ••1234", card → credit, a/c → bank). Each gets an opening balance from the latest "available balance" SMS. The user ticks which to create; links are written at once. This is the same screen as F07 E6 Account Sources in "create" mode.
- **O2 merchant clusters (deterministic).** L4 keys plus char n-gram similarity for variants. EmbeddingGemma is not used here (see AI-045).
- **O3 category set (model assists).**
  - The app holds a curated catalogue of about 60 categories with type and icon.
  - The model labels each top merchant cluster in two multiple-choice steps: group (about 10 groups) and then category within the group (about 6 options).
  - Proposed set = catalogue entries with at least one assigned merchant, plus essentials. The user edits the set before it is created.
  - Without a model, clusters stay unlabelled and the user assigns them.
- **O4 recurring (deterministic).** Same merchant, similar amount, monthly cadence ±3 days, ≥ 3 occurrences → proposed recurring / EMI / subscription. Salary credits are detected the same way.
- **O5 budgets (deterministic).** Per-category monthly median → optional budgets.
- **Scope note.** The current user already has 144 categories and 1,716 rows, so O is not needed today; L and A carry the value. O becomes worth building only if the app goes to other users.

## Phases
| Phase | Scope | Exit check | Status |
|---|---|---|---|
| L1 | SMS ↔ transaction join engine (pure) + coverage figure | Unit tests on scrubbed fixtures; on the phone: labels found, coverage recorded here (counts only) | Not started |
| L2 | "Learned from your history" confirm screen, writes memory + links | Phone: memory filled; next F07 scan shows the learned share of categories | Not started |
| L4 | Merchant key normalisation with alias read | Unit tests over real-shaped keys (scrubbed) | Not started |
| A1 | Deterministic checks A1–A8 (pure) | Unit tests per check incl. the daily-tea repeat case | Not started |
| A2 | Audit worker + "Check these" section + dismiss fingerprints + backup of dismissals | Phone: 2 weeks, findings precision logged | Not started |
| L3 | Teach screen with batched model suggestions | Parser tests; phone | Not started |
| L5 | Correction-aware memory (needs AI-014) | Unit tests | Not started |
| A3 | Model plausibility judge, shadow mode then live | Shadow precision meets the bar | Not started |
| O1–O5 | Setup flow | Only if the app goes to other users | Idea |

## Acceptance
- L: share of F07 drafts whose category comes from memory/history (not the model, not missing) is at least 70% in the first week after L2. Today it is close to 0 (F01 dry run: category 0%).
- A, deterministic checks: at least 60% of shown findings accepted as a fix; no more than 3 per day; dismissals never re-raised.
- A, model judge: goes live only if at least 80% of its shadow "does not fit" findings agree with a later user edit or a deterministic signal; otherwise it stays off.
- Everything works with no model: L1, L2, L4, A1–A8, O1, O2, O4, O5 are deterministic.

## Risks and unknowns
- **L1 false joins:** two different spends with the same amount on the same day. The unique-candidate rule and the count ≥ 2 aggregate are the guard, and L2 confirmation is the last line.
- **A fatigue:** a check that is wrong a few times trains the user to ignore all of them. That is why the cap, the precision bars and the dismiss memory are there.
- **L4 re-keying:** a bad normaliser merges unrelated merchants. Use alias read so it is reversible.
- **Model latency on the S24:** no published Gemma 4 E2B figures for the Snapdragon 8 Gen 3. L3 and A3 are batched so latency is not user-facing, but measure it.
- **EmbeddingGemma:** unproven for opaque bank merchant strings. Replay test only (AI-045).

## Unverified on device
Everything.

## Decisions log
- 2026-10-11: the model works at setup, bootstrap and audit time (batched, background, not per entry), always via multiple choice; tool calls stay out (README principle 2). The user's "tool-calling analysis after entry" is built as app-run deterministic checks plus an optional multiple-choice judge.
- 2026-10-11: bootstrap the merchant base from SMS ↔ saved-transaction joins (deterministic) before any model labelling; the model only suggests for what joins could not label.
- 2026-10-11: audit findings never auto-fix and never notify on their own; capped at 3 a day; dismissals remembered and backed up.
- 2026-10-11: AI-assisted setup (O) is deferred until the app has other users; L and A come first.
