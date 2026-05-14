# Phase 24: Scroll-to-Top (Global) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-27
**Phase:** 24-scroll-to-top-global
**Areas discussed:** Button visibility & position, Component implementation, Default screens

---

## Button Visibility & Position

| Option | Description | Selected |
|--------|------------|----------|
| Threshold: 1 screen | Shows after scrolling 1 screen height (50dp) | ✓ |
| Threshold: Always | Shows immediately when not at top | |
| Manual toggle | User toggles visibility manually | |

| Option | Description | Selected |
|--------|------------|----------|
| Bottom-right | Standard Material FAB position | |
| Bottom-left | Above nav bar | |
| Center-bottom | Center of bottom area | ✓ |

**User's choice:** Appears after 1 screen threshold, positioned center-bottom

---

## Component Implementation

| Option | Description | Selected |
|--------|------------|----------|
| Modifier | Wrap LazyColumn with modifier | ✓ |
| Composable | Component with states | |
| Explicit | Manual show/hide | |

**User's choice:** Modifier-based approach

---

## Default Screens

| Option | Description | Selected |
|--------|------------|----------|
| All scrollable | All 13+ screens automatically | ✓ |
| Transactions only | Only transaction screens | |
| Manual opt-in | Add per-screen | |

**User's choice:** All scrollable screens get scroll-to-top by default

---

*Phase: 24-scroll-to-top-global*
*Context gathered: 2026-04-27*