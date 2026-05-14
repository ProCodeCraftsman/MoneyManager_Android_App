# Phase 21: Transactions UI Polish - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-25
**Phase:** 21-transactions-ui-polish
**Areas discussed:** Scroll choreography, Search reveal pattern, Summary surface treatment, Bottom nav separation, Filter indicator placement

---

## Scroll Choreography

| Option | Description | Selected |
|--------|-------------|----------|
| Summary scrolls away | Summary is LazyColumn item 0, not in topBar. Header-only stays sticky. | ✓ |
| Summary collapses with header | Both animate together via isScrolled — summary hides on scroll | |
| Summary always visible (sticky) | Summary stays in topBar, never hides | |

**User's choice:** Summary scrolls away — moved into LazyColumn as item 0

| Option | Description | Selected |
|--------|-------------|----------|
| Time nav scrolls away | Part of content area, not pinned | ✓ |
| Time nav stays sticky | Pinned below header for quick navigation | |

**User's choice:** Time nav scrolls away — part of the same content block as summary

---

## Search Reveal Pattern

| Option | Description | Selected |
|--------|-------------|----------|
| In the header row | Search icon next to filter icon in top-right | ✓ |
| Above the list, inline with filters | A toolbar row below topBar | |

**User's choice:** Search icon in the header row

| Option | Description | Selected |
|--------|-------------|----------|
| Full-width bar replaces header | Header replaced by search field (Gmail pattern) | |
| Search bar expands below header | Header stays, bar animates in below it | ✓ |

**User's choice:** Expands below header — non-destructive to header context

---

## Summary Surface Treatment

| Option | Description | Selected |
|--------|-------------|----------|
| Flat, no card shape | Inline values, divider/spacing only | |
| Keep card, fix the color | Rounded card with proper theme token | |
| Full-width surface with elevation | Full-width, 1dp shadow, no rounded corners | ✓ |

**User's choice:** Full-width surface with elevation — acts like a sub-header panel

---

## Bottom Nav Separation

| Option | Description | Selected |
|--------|-------------|----------|
| Color match, no gap | Same surface color, reduced padding | ✓ |
| Faded edge | Gradient fade at bottom | |
| Visible divider | 1dp divider line | |

**User's choice:** Color match, no gap — clean seamless transition

---

## Filter Indicator (follow-up)

| Option | Description | Selected |
|--------|-------------|----------|
| Badge only on filter icon | Existing BadgedBox pattern, sufficient | ✓ |
| Chip row appears when active | Animated chip row below header | |

**User's choice:** Badge only — no chip row needed

---

## Claude's Discretion

- Animation duration for search expand/collapse
- Whether searchActive is rememberSaveable
- Exact shadowElevation value for summary panel (decided 1dp, 0.5dp acceptable)
- contentPadding bottom value (WindowInsets.navigationBars or constant)

## Deferred Ideas

None
