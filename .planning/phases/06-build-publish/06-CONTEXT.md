# Phase 6: Build & Publish - Context

**Gathered:** 2026-04-13
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase delivers:
- JSON/CSV import/export functionality for backing up and migrating user data
- Play Store listing with app icons, screenshots, and privacy policy
- Final build configuration and upload to Google Play

</domain>

<decisions>
## Implementation Decisions

### Import/Export Format
- **D-01:** Export in BOTH JSON and CSV formats
  - JSON: Full backup with all metadata intact
  - CSV: Spreadsheet-compatible for manual editing/review

### Export Scope
- **D-02:** Export ALL data points:
  - Transactions
  - Accounts
  - Categories
  - Budgets
  - Goals
  - Tags

### Play Store Assets
- **D-03:** Use recommended defaults
  - Standard 2-5 screenshots showing main features
  - Feature graphic for Play Store listing

### Privacy Policy
- **D-04:** Standard financial app privacy policy
  - Data collection disclosure
  - No sale of personal data
  - Cloud backup explanation

### App Icon Style
- **D-05:** Create custom icon design
  - Use Android Adaptive Icons
  - Follow Material Design 3 guidelines

### the agent's Discretion
- Default export file naming convention
- CSV encoding (UTF-8)
- Screenshot details (device frames, light/dark)
- Privacy policy template specifics

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Google Play Requirements
- `.planning/PROJECT.md` — App name, target SDK, configuration
- Google Play Console guidelines for asset specifications

### Technical References
- `MoneyManager/app/build.gradle.kts` — Current build configuration
- Room schema for data model reference

[If no external specs: "No external specs — requirements fully captured in decisions above"]

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Room database entities: Account, Transaction, Category, Tag, Budget, Goal
- Settings screen: Currency, theme, auto-lock already implemented

### Established Patterns
- MVVM with Hilt dependency injection
- Material Design 3 theming

### Integration Points
- Export: Trigger from Settings screen
- Play Store: Build configuration in build.gradle.kts

</code_context>

<specifics>
## Specific Ideas

No specific requirements — open to standard approaches

</specifics>

<deferred>
## Deferred Ideas

None — all Phase 6 scope items covered

</deferred>

---

*Phase: 06-build-publish*
*Context gathered: 2026-04-13*