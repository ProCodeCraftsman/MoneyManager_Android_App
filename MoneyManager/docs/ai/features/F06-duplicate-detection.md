# F06 Duplicate detection and reconciliation

State: Planned | Packages: `domain/ai` (new `DuplicateFinder`), `data/repository` | Depends on: F04 (AI-010)

## Goal
The same real-world payment often arrives more than once: bank debit SMS plus card SMS, SMS plus a voice note, SMS plus a receipt photo. Flag likely duplicates before the user saves, instead of silently double-counting.

## Design
- Deterministic only: candidate = existing transaction or pending draft with the same amount (exact), within a window (default 48 h) and compatible type; scored higher for same merchant, same last4 or reference number (UPI ref in SMS is a strong key).
- Output is a flag on the draft (`possible_duplicate:<transactionId>`) and a banner on the draft screen with "Keep both / Discard draft". Never auto-delete.
- Card repayment (transfer) is excluded; it is already modelled as a transfer, not an expense.
- Store the reference number hash on saved SMS-derived transactions only if a schema change is accepted; otherwise match on amount, time and merchant. Any column means migration + version bump + backup update.

## Phases
| Phase | Scope | Status |
|---|---|---|
| D1 | Amount + window matcher against saved transactions, unit tests | Not started |
| D2 | Draft-screen banner and actions | Not started |
| D3 | Reference-number key (needs schema decision) | Idea |

## Risks and unknowns
False positives for repeated equal payments (daily tea 20); mitigate with a short window and require merchant or reference match for small round amounts.
