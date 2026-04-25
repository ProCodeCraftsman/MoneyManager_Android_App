# Phase 14: Theme Infrastructure - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in 14-CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-25
**Phase:** 14-theme-infrastructure
**Areas discussed:** Theme Data Model, DataStore Structure, Startup Behavior, System Theme Handling

---

## Theme Data Model

| Option | Description | Selected |
|--------|-------------|----------|
| Sealed class/Enum | Type-safe, IDE support, future-proof | ✓ |
| String-based | Simple storage, risk of typos | |
| Data class | Most flexible for future custom themes | |

**User's choice:** Sealed class/Enum
**Notes:** Type-safe approach preferred for compile-time checking and IDE support

---

## DataStore Structure

| Option | Description | Selected |
|--------|-------------|----------|
| Extend DataStore | Add theme preference to existing PreferencesManager | ✓ |
| Separate DataStore | New DataStore for theme settings | |
| Migration logic | Clean migration but more complex | |

**User's choice:** Extend DataStore
**Notes:** Keep it simple, extend existing PreferencesManager

---

## Startup Behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Block startup | Block until theme loaded, no flicker | ✓ |
| Apply immediately | Show default, switch after load | |
| Allow flicker | Accept brief visual flash | |

**User's choice:** Block startup
**Notes:** No visual flicker is important for user experience

---

## System Theme Handling

| Option | Description | Selected |
|--------|-------------|----------|
| Smart default | Device theme for new users, stored preference after override | ✓ |
| Always override | Always use stored preference | |
| System only | App follows system, no in-app toggle | |

**User's choice:** Smart default
**Notes:** Best of both worlds — respects device setting for new users, stored preference after manual change

---

## Deferred Ideas

No scope creep was detected during discussion. All suggestions stayed within Phase 14 scope.