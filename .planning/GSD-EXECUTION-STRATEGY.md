# GSD End-to-End Execution Strategy
## MoneyManager Android App Bridge Implementation

This document defines the complete execution strategy using GSD (Get Shit Done) agents and GitNexus tools.

---

## Available GSD Agents

| Agent | Purpose | When to Use |
|-------|---------|-------------|
| `gsd-phase-researcher` | Research how to implement a phase before planning | Before each phase |
| `gsd-planner` | Create executable phase plans with task breakdown | After research |
| `gsd-executor` | Execute GSD plans with atomic commits | During implementation |
| `gsd-nyquist-auditor` | Generate tests, verify coverage for requirements | Testing phase |
| `gsd-ui-checker` | Validate UI-SPEC.md design contracts | UI validation |
| `gsd-ui-researcher` | Produce UI-SPEC.md design contract | UI design |
| `gsd-ui-auditor` | Retroactive 6-pillar visual audit of implemented UI | Final UI review |
| `gsd-verifier` | Verify phase goal achievement, goal-backward analysis | Final verification |
| `gsd-integration-checker` | Verify cross-phase integration and E2E flows | Phase handoff |
| `gsd-debugger` | Investigate bugs using scientific method | When bugs found |

---

## GitNexus Tools

| Tool | Purpose |
|------|---------|
| `gitnexus_query` | Find execution flows related to a concept |
| `gitnexus_context` | 360-degree view of a symbol |
| `gitnexus_impact` | Symbol blast radius - what breaks if changed |
| `gitnexus_detect_changes` | Git-diff impact - what do current changes affect |
| `gitnexus_rename` | Multi-file coordinated rename |
| `gitnexus_cypher` | Raw graph queries |

---

## Execution Workflow

### Phase 0: Preparation (Execute Once)
```bash
# 1. Index the repository
npx gitnexus analyze

# 2. Verify index loaded
READ gitnexus://repo/MoneyManager/context
```

### Phase 1-8: Each Phase Follows This Pattern

```
┌─────────────────────────────────────────────────────────────┐
│  PHASE EXECUTION LOOP                                       │
├─────────────────────────────────────────────────────────────┤
│  1. RESEARCH (gsd-phase-researcher)                        │
│     - Analyze BRIDGE.md requirements for phase             │
│     - Research Android implementation patterns              │
│     - Produce RESEARCH.md                                  │
│                                                             │
│  2. PLAN (gsd-planner)                                      │
│     - Create task breakdown                                │
│     - Define dependencies                                  │
│     - Set success criteria                                 │
│                                                             │
│  3. IMPLEMENT (gsd-executor)                               │
│     - Atomic commits for each task                         │
│     - Code review inline                                   │
│     - Unit tests for new features                          │
│                                                             │
│  4. VERIFY (gsd-verifier)                                   │
│     - Goal-backward analysis                               │
│     - Check deliverables against requirements              │
│     - Produce VERIFICATION.md                              │
│                                                             │
│  5. UAT & BUG FIX (gsd-debugger)                           │
│     - Manual testing guidance                               │
│     - Bug investigation and fixes                          │
│                                                             │
│  6. FINAL TEST & SIGN-OFF                                  │
│     - Integration check                                    │
│     - End-to-end flow verification                         │
│     - Move to next phase                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Phase-by-Agent Mapping

### Phase 7: Core Transaction Features
| Step | Agent | Output |
|------|-------|--------|
| Research | `gsd-phase-researcher` | RESEARCH.md for transactions, tags, filters |
| Plan | `gsd-planner` | 7-transactions/1-PLAN.md |
| Implement | `gsd-executor` | Code changes + commits |
| Verify | `gsd-verifier` | VERIFICATION.md |
| UAT | `gsd-debugger` | Bug fixes |
| Sign-off | `gsd-integration-checker` | Integration report |

### Phase 8: Dashboard Enhancements
| Step | Agent | Output |
|------|-------|--------|
| Research | `gsd-phase-researcher` | RESEARCH.md for dashboard, charts |
| Plan | `gsd-planner` | 8-dashboard/1-PLAN.md |
| Implement | `gsd-executor` | Code changes |
| Verify | `gsd-verifier` | VERIFICATION.md |
| UAT | `gsd-debugger` | Bug fixes |

### Phase 9: Recurring & Reports
| Step | Agent | Output |
|------|-------|--------|
| Research | `gsd-phase-researcher` | RESEARCH.md |
| Plan | `gsd-planner` | 9-recurring-reports/1-PLAN.md |
| Implement | `gsd-executor` | Code changes |
| UI Check | `gsd-ui-checker` | Design verification |
| Verify | `gsd-verifier` | VERIFICATION.md |

### Phase 10: Budgets, Goals, Templates
| Step | Agent | Output |
|------|-------|--------|
| Research | `gsd-phase-researcher` | RESEARCH.md |
| Plan | `gsd-planner` | 10-planning/1-PLAN.md |
| Implement | `gsd-executor` | Code changes |
| UI Check | `gsd-ui-checker` | Design verification |
| Verify | `gsd-verifier` | VERIFICATION.md |

### Phase 11: Categories Management
| Step | Agent | Output |
|------|-------|--------|
| Research | `gsd-phase-researcher` | RESEARCH.md |
| Plan | `gsd-planner` | 11-categories/1-PLAN.md |
| Implement | `gsd-executor` | Code changes |
| Verify | `gsd-verifier` | VERIFICATION.md |

### Phase 12: Data Management
| Step | Agent | Output |
|------|-------|--------|
| Research | `gsd-phase-researcher` | RESEARCH.md |
| Plan | `gsd-planner` | 12-data/1-PLAN.md |
| Implement | `gsd-executor` | Code changes |
| Verify | `gsd-verifier` | VERIFICATION.md |

### Phase 13: Receipts & Documents
| Step | Agent | Output |
|------|-------|--------|
| Research | `gsd-phase-researcher` | RESEARCH.md |
| Plan | `gsd-planner` | 13-receipts/1-PLAN.md |
| Implement | `gsd-executor` | Code changes |
| Verify | `gsd-verifier` | VERIFICATION.md |

---

## GitNexus Usage at Each Stage

### Before Writing Code
```bash
# Understand the existing transaction flow
gitnexus_query({query: "transaction add"})
# → TransactionAddFlow: openAddTx → validateInput → saveTransaction

# Check impact before adding new fields
gitnexus_impact({name: "TransactionEntity"})
# → 12 files affected, 3 DAOs need updates

# Get context on CategoryEntity
gitnexus_context({name: "CategoryEntity"})
# → Used by: CategoryDao, CategoryRepository, CategoryViewModel
```

### After Code Changes
```bash
# Check what your changes affect
gitnexus_detect_changes()
# → Modified: TransactionEntity.kt, TransactionDao.kt
# → Impacts: TransactionsViewModel, TransactionsScreen

# Verify no regressions
gitnexus_context({name: "saveTransaction"})
# → Called by: AddTransactionDialog, TransactionRepository
```

### For Refactoring
```bash
# Before renaming a function
gitnexus_impact({name: "oldFunctionName"})
# → Rename affects 5 files with HIGH confidence

# Execute safe rename
gitnexus_rename({oldName: "oldFunctionName", newName: "newFunctionName"})
```

---

## Commands to Start Execution

### Start from Phase 7 (Current Priority)

```bash
# Phase 7: Core Transaction Features
Task(description="Phase 7 Research", 
     prompt="Research how to implement transaction search, filters, tags, sub-categories, and transfers in Android. 
             Read BRIDGE.md requirements.
             Research existing Android patterns in the codebase.
             Produce RESEARCH.md in .planning/phases/7-transactions/",
     subagent_type="gsd-phase-researcher")
```

### Sequential Execution Command

The system will:
1. Run researcher for current phase
2. Wait for RESEARCH.md
3. Run planner to create tasks
4. Run executor to implement
5. Run verifier to check
6. Run integration checker for sign-off
7. Move to next phase

---

## Quality Gates

Each phase must pass these gates before moving on:

| Gate | Criteria | Tool |
|------|----------|------|
| Code Compiles | `./gradlew assembleDebug` succeeds | bash |
| Tests Pass | Unit tests in test/ pass | bash |
| No Critical Bugs | Debugger finds 0 critical issues | gsd-debugger |
| Design Match | UI matches BRIDGE.md spec | gsd-ui-checker |
| Integration | Cross-phase flows work | gsd-integration-checker |
| Goal Achievement | VERIFICATION.md shows all goals met | gsd-verifier |

---

## Quick Reference

| Command | What It Does |
|---------|--------------|
| `npx gitnexus analyze` | Re-index codebase |
| `npx gitnexus status` | Check index freshness |
| `gitnexus_query({query: "X"})` | Find X-related code |
| `gitnexus_context({name: "X"})` | Deep dive on X |
| `gitnexus_impact({name: "X"})` | What breaks if X changes |

---

## Current Status

- [x] BRIDGE.md created with gap analysis
- [x] ROADMAP.md updated with phases 7-13
- [x] Execution strategy documented
- [ ] Phase 7 research not started
- [ ] Phase 7 implementation not started

**Next Action:** Run Phase 7 research using `gsd-phase-researcher`
