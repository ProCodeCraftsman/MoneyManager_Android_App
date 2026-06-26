# MoneyManager — Features & Functionality Overview

> Personal finance Android app. Offline-first, AI-assisted, with optional cloud sync and local on-device AI.

---

## 1. Transaction Management

**8 transaction types:** Income, Expense, Savings, Transfer, Borrow, Lend, Repay, Receive.

- Full CRUD on transactions.
- Attach receipt images.
- Add free-text notes and custom tags.
- Split a transaction across multiple categories (parent–child hierarchy).
- Link a transaction to a savings goal (auto-contributes amount to goal).
- Search and filter by type, account, category, tag, goal, peer, date range, or keyword.

---

## 2. Accounts

**6 account types:** Bank Account, Cash, Credit Card, Savings Account, Peer, Investment.

- Full CRUD with custom emoji/icon and color.
- Balance is auto-calculated from linked transactions; initial balance is set on creation.
- View all accounts with current balances and a total-assets figure.
- Bar chart comparing balances and inflow/outflow across accounts.

---

## 3. Budgeting

- Create monthly per-category budgets; auto-create for following months.
- Navigate months with forward/back arrows or a month selector.
- Real-time progress tracking: amount spent vs. budgeted, utilization percentage, green/amber/red status.

---

## 4. Savings Goals

- Create goals with name, emoji, target amount, and optional deadline.
- Contributions via: manual entries, or by linking transactions to the goal.
- Progress bar and percentage; deadline countdown (days remaining).
- Auto-marks complete when target is reached.

---

## 5. Recurring Transactions

- Frequencies: daily, weekly, biweekly, monthly, yearly.
- Activate/deactivate without deleting.
- Optional reminder N days before next occurrence.
- Background worker runs on app start, generates due transactions, advances the next-due date, and updates account balances automatically.

---

## 6. Categories & Sub-categories

- Full CRUD; each category has a type (expense/income/savings), emoji/icon, and color.
- Sub-category support (parent–child hierarchy).
- Archive/unarchive without deleting.
- Default categories seeded on first launch.

---

## 7. Tags

- Create, edit, delete with a custom color.
- Assign multiple tags to any transaction.
- Filter transaction list by tag.

---

## 8. Peer Contacts & Borrow/Lend Tracking

- Peer contacts store name, phone, email, photo, and description.
- Auto-tracked cumulative totals: total given, total received, outstanding balance.
- Four dedicated transaction types (Borrow, Lend, Repay, Receive) update peer balances automatically.
- Import from device contacts.
- Dedicated peer list and borrow/lend management screens.

---

## 9. Summary Dashboard

**5-tab dashboard** with global time-filter (Day / Week / Month / Quarter / Year / All Time / custom range) and period navigation arrows.

| Tab | Content |
|---|---|
| Expense | Pie chart by category + account breakdown |
| Income | Pie chart by source + account breakdown |
| Lending | Net position, per-person lend/borrow summary |
| Transfers | Count, total, account-wise breakdown |
| Savings | Goal progress bars + savings account growth |

Hero card shows net balance (income − expense) with trend percentage vs. previous period. Total-assets card always visible.

---

## 10. Insights Screen

- **Status pane:** hero metric + 4 key-figure cards for current month. *(Live)*
- **Risks pane:** planned (not yet available).
- **Trends pane:** planned (not yet available).

---

## 11. AI-Powered Transaction Drafting

AI extracts structured transaction data from four input sources:

| Source | How it works |
|---|---|
| **Receipt scan** | Camera photo → vision model extracts amount, merchant, category |
| **SMS** | Read SMS inbox → pattern-match bank/payment messages → draft transaction |
| **Voice memo** | Record audio → transcribe → extract transaction details |
| **Free text** | Type a description → AI infers type, amount, category |

**AI backends (automatic routing):**
- **Cloud AI** — Google Generative AI via AICore (requires internet).
- **On-device AI** — Google LiteRT-LM running locally; uses NPU → GPU → CPU delegation; requires ~1.5–2 GB RAM; works fully offline.
- Router selects the best available backend based on device capability.

**Additional AI capabilities:**
- Merchant-to-category memory (learns your preferences).
- Draft confidence scoring; low-confidence drafts are flagged for review before saving.
- Full conversation history for reviewing past AI sessions.
- Streaming response so the UI updates in real-time as the model generates.

**Model management:**
- Download on-device models from HuggingFace (with OAuth authentication).
- Track download progress; manage an allowlist of approved models.
- Device capability detection for hardware-accelerated inference.

---

## 12. Templates & Quick Add

- Save any transaction as a template (name, type, amount, account, category, note).
- One-tap reuse of templates in the add-transaction form.
- **Home screen widget:** quick-add Income, Expense, or Transfer without opening the app.

---

## 13. Data Export & Import

**Formats:** CSV (per entity type or full dump) and JSON (full backup/restore).

- Export any combination of: accounts, transactions, categories, budgets, goals, tags, peers, recurring entries, templates.
- Full JSON backup restores all data in one step.
- Import validates data, resolves references (account/category names → IDs), skips duplicates, and reports import counts per entity type.
- File picker uses Android Storage Access Framework (user chooses save/load location).

---

## 14. Google Drive Backup

- Sign in with Google to enable Drive access.
- Backup is **AES-encrypted** with a user-supplied passphrase.
- Manual backup trigger or automatic (daily / weekly schedule).
- Restore from Drive: supply passphrase → decrypt → import.
- Shows last backup timestamp and detects existing backups on sign-in.

---

## 15. Firebase Cloud Sync *(Partial)*

- Google Sign-In via Firebase Auth; works fully offline without sign-in.
- Firestore backend; offline changes queued and synced when online.
- **Current status:** DELETE operations sync; CREATE/UPDATE sync and conflict resolution are not yet implemented.

---

## 16. Security & App Lock

- **PIN protection:** 4-digit PIN stored with PBKDF2-SHA256 + random salt.
- **Biometric unlock:** fingerprint or face recognition (requires PIN as fallback).
- **Auto-lock:** configurable timeout (1 / 5 / 15 / 30 minutes); app locks when returning after timeout.
- PIN change supported at any time.

---

## 17. Settings & Customization

| Area | Options |
|---|---|
| Theme | Coco Brown, Calm Green (default), Midnight Blue |
| Dark mode | On / Off / Follow system |
| Currency | INR, USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY, BRL |
| Data management | View storage usage, clear all data (with confirmation) |
| Entity management | Accounts, Categories, Tags, Peers, Budgets, Goals, Recurring, Templates — each has a dedicated settings sub-screen |

---

## 18. Navigation

- Bottom navigation: **Summary · Transactions · Settings**.
- Deep links: open app directly to transactions list, add-transaction form (with pre-filled type), transfer screen, or home.
- HuggingFace OAuth redirect handled via deep link.

---

## Known Gaps / Planned Work

| Item | Status |
|---|---|
| Firebase CREATE/UPDATE sync & conflict resolution | Incomplete |
| Insights — Risks & Trends panes | Placeholder |
| Investment account type | Data model exists; not fully surfaced in UI |
| Receipt image viewer | Stored but not yet displayable |
| Peer detail screen | Route exists; screen not built |
| Undo for deletes | Not implemented |
| Database encryption at rest | Not implemented |
| Automated tests / CI | Not implemented |
