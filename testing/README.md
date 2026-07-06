# Warung POS — E2E Test Suite

Production-release end-to-end test suite for the Warung POS Android app. Written against the **shipped code**
(branch `main`, versionCode 1, DB schema v3), not just the PRD — where the two disagree, cases assert actual
behaviour and flag the divergence.

## Read in this order
1. **[00-assumptions-and-gaps.md](00-assumptions-and-gaps.md)** — start here. Environment facts, resolved
   ambiguities, and the full list of PRD-vs-implementation gaps (auth model, order flow, sold-out, split
   payment, i18n, categories, etc.). Every "Expected Result" downstream depends on these.
2. **[TEST_PLAN.md](TEST_PLAN.md)** — scope, entry/exit criteria, environments (E1–E5), data baselines (BL-0..3).
3. **[TEST_STRATEGY.md](TEST_STRATEGY.md)** — approach, case format, determinism rules, risk weighting.
4. **[TEST_COVERAGE.md](TEST_COVERAGE.md)** — Requirement Traceability Matrix (every FR/NFR + every gap → cases)
   and a coverage-by-test-type self-review.

## Feature suites
| File | Area |
|------|------|
| [01-authentication-pin.md](01-authentication-pin.md) | Local username + PIN (register/unlock/lock) |
| [02-onboarding-firstrun.md](02-onboarding-firstrun.md) | First-run seeding, auto-open day |
| [03-order-and-bills.md](03-order-and-bills.md) | Order tab, bill lifecycle, add items, variants |
| [04-payment.md](04-payment.md) | Payment, change, stock deduction |
| [05-void.md](05-void.md) | Void item / void bill / audit |
| [06-menu-management.md](06-menu-management.md) | Items, variants, recipes, sold-out, hide |
| [07-payment-methods-settings.md](07-payment-methods-settings.md) | Payment method toggles |
| [08-day-management.md](08-day-management.md) | Day close, rollover, Z-report, history |
| [09-expenses.md](09-expenses.md) | Expense logging |
| [10-stock.md](10-stock.md) | Stock items & batches |
| [11-stock-opname.md](11-stock-opname.md) | Physical count / variance |
| [12-reports-dashboard.md](12-reports-dashboard.md) | Dashboard, date-range reports, CSV export |
| [13-sync-multidevice.md](13-sync-multidevice.md) | Offline, reconnect, 2-device, conflict |
| [14-settings-language-i18n.md](14-settings-language-i18n.md) | Settings & language |
| [15-version-gate.md](15-version-gate.md) | Minimum-version gate |
| [16-navigation-state-lifecycle.md](16-navigation-state-lifecycle.md) | Nav, lifecycle, recovery |
| [17-regression.md](17-regression.md) | Release-gate regression pack (run every RC build) |

## How to execute
- Each case is self-contained: Preconditions/Baseline, Test Data, numbered Steps, an observable Expected Result,
  Postconditions, Edge Case Notes, and an Automation Candidate flag.
- Reset to a clean state via **Settings → Apps → Warung POS → Clear storage** (wipes Room + encrypted prefs +
  first-run flag) or reinstall.
- Storage-level assertions use the RTDB console (online) or `adb shell run-as com.wfx.warungpos sqlite3 …` on a
  debug build — see `TEST_STRATEGY.md` §6.

## Counts
259 numbered cases across 16 feature files, plus a regression pack (7 tables) in file 17. Priorities/severities
per case; the regression pack curates the money- and data-critical subset for the release gate.

> These are documentation only — no app code is modified and no tests are executed by this deliverable.
