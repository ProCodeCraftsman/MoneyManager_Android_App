# Phase 17: Income/Expense Coloring - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in 17-CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-25
**Phase:** 17-income-expense-coloring
**Areas discussed:** Hardcoded Color Replacements

---

## Hardcoded Color Handling

| Option | Description | Selected |
|--------|-------------|----------|
| Replace all hardcoded | Replace all Color(0xFF00C853) and similar with theme colors | ✓ |
| Fix obvious cases only | Only fix obvious income/expense displays | |
| Agent discretion | Delegate to agent to decide which ones to replace | |

**User's choice:** Replace all hardcoded
**Notes:** Complete consistency across all themes

---

## Pre-decided Items

Theme infrastructure (Phases 14-16) already provides:
- Income color: `colorScheme.secondary` ✓
- Expense color: `colorScheme.error` ✓
- Instant switching via theme selection ✓

---

## Deferred Ideas

No scope creep was detected during discussion.