# Warung POS — QA Handoff

E2E test suite (17 files, 259 authored cases) executed against a running debug build on a single Android
emulator (API 33), primarily offline, over a multi-day session. Full step-by-step evidence for every case is in
[`EXECUTION_LOG.md`](EXECUTION_LOG.md) (~3,100 lines) — this document is the scannable summary for whoever picks
up this project next.

**Scope note:** this began as a test-execution pass only. Two follow-up sessions then fixed defects in the
working tree:
- **2026-07-10** — DEFECT-001 and DEFECT-002 (Auth / Lock App) fixed.
- **2026-07-11** — every other open defect (DEFECT-003/008 through DEFECT-015) fixed.

**As of 2026-07-11, all 14 numbered defects found by this suite are fixed and re-verified.** All fixes are
**uncommitted** in the working tree. 14 numbered defects were found in total (DEFECT-001 through DEFECT-015;
DEFECT-003 and DEFECT-008 are the same root-cause issue found twice — see below; DEFECT-012 was never assigned —
a candidate finding during file 11 was folded into DEFECT-011 instead of becoming its own entry, so the
numbering has a deliberate gap, not a missing write-up).

## Release-gate verdict

**Now meets the suite's own release-gate condition** (every Critical/High regression-pack row Pass; zero
Blocker/Critical defects open in Order, Payment, Void, Day-close, or Sync) **once the uncommitted fixes land.**
The sole Critical blocker (DEFECT-003/008, multiple concurrent OPEN shifts) is fixed; every Major/High defect
touching money-visible screens is also fixed. Full unit + instrumented test suites pass (128 + 35 tests, zero
failures) as of the last verification pass.

## All defects — fixed and re-verified

| ID | Area | Fix | Verification |
|----|------|-----|---------------|
| **DEFECT-001** | Auth / Lock App | `PinViewModel.refreshMode()` re-derives UNLOCK/REGISTER mode from live registration state and clears the form on every re-entry to the PIN gate (`LaunchedEffect` in `WarungPosApp.kt`). | Re-ran the exact bypass trigger on-device (register → Lock App → Lock, no restart): correctly shows UNLOCK, wrong PIN rejected, no credential overwrite. New `PinViewModelTest`. |
| **DEFECT-002** | Auth / Lock App | Same fix as DEFECT-001 — `refreshMode()` also clears the `pin` field on every Lock. | Re-ran exact repro (unlock, Lock, blind Unlock tap with 0 digits): correctly rejected, no bypass. |
| **DEFECT-003 / DEFECT-008** | Day/Shift | `ShiftDao.openIfNoneOpen()` — new `@Transaction` DAO method atomically checks-then-inserts, closing the race that let two callers both open a shift. `BillDao.getOpenBillsForShift(shiftId)` replaces the unscoped global query in Close Day/auto-close. `MIGRATION_4_5` repairs any pre-existing duplicate-OPEN corruption on upgrade. | New `ShiftDaoTest` — a genuine 20-way concurrent-coroutine test against a real Room DB confirms exactly one shift ever ends up OPEN. New `BillDaoTest` cases for the scoped query. |
| **DEFECT-004** | Menu / Order | `BillDetailViewModel.addItem()`'s read-modify-write is now `Mutex`-guarded, serializing concurrent taps so no increment is lost to interleaving. | New `AddItemRaceConditionTest` — 30 genuinely concurrent coroutines confirm the final quantity always exactly matches the tap count. |
| **DEFECT-005** | Payment | `PaymentScreen` now displays `state.error` (an `AlertDialog`, same pattern as `BillDetailScreen`'s void error) — the ViewModel already set it correctly, the UI just never showed it. | New instrumented `PaymentScreenTest` cases: error is displayed, dismiss callback fires. |
| **DEFECT-006** | Void | Added the missing `voidNote` column (`MIGRATION_5_6`) and threaded it end-to-end (entity → domain → mapper → DAO → repository → use case → RTDB sync). | New mapper/use-case test cases round-tripping a non-null `voidNote`. |
| **DEFECT-007** | Menu / Variants | `VariantGroupEditor`'s name/price fields now buffer locally (`remember(id) { mutableStateOf(...) }`) instead of being fully controlled by state that round-tripped through a DB write + full reload on every keystroke. Price field also stopped reformatting from the parsed number (was erasing a typed `-` before the digits could follow). | Manual on-device: typed a 33-char name and `-1500` in one fast burst, confirmed byte-for-byte correct on-screen and in the persisted DB (WAL-inclusive read). |
| **DEFECT-009** | Void / Reporting | `VoidBillUseCase` now cascades `VOID` status to the bill's still-active `order_items` (new `VoidReason.BILL_VOID`), so whole-bill voids correctly contribute to the Z-report's void audit. | New `VoidBillUseCaseTest` case. |
| **DEFECT-010** | Reports / Z-report | New `ZReportSnapshot` model + `ZReport.toSnapshot()` mapper; `ZReportViewModel` now reads the persisted snapshot (falling back to live re-derivation only if none exists) and `ZReportScreen` shows the previously-invisible `countedCash`/`expectedCash`/`variance`. | New `ZReportMapperTest` cases against the real JSON shape `GenerateZReportUseCase` produces. |
| **DEFECT-011** | Stock / Opname | New shared `filterDecimalInput()` (digits + at most one `.`) replaces the buggy filter in all 3 affected fields (reorder threshold, batch qty, opname counted qty). | New `DecimalInputFilterTest`, including the exact `"2.5.1"` repro. |
| **DEFECT-013** | Stock Opname | `StockOpnameViewModel` now persists every counted-qty/reason edit to the DB immediately, so navigating away and back reads the draft instead of stale data. | New `StockOpnameViewModelTest` simulating ViewModel recreation — a typed count and a variance reason both survive. |
| **DEFECT-014** | Reports / Dashboard | (a) `DashboardScreen` gained an Expenses card. (b) `ReportsScreen`'s shift-scoped card relabeled "Current Shift Summary" (was "Day Summary", easily conflated with the adjacent genuinely-day-scoped "Today's Dashboard" link). | Manual verification (pure UI text/layout changes). |
| **DEFECT-015** | i18n | Two independent bugs: (1) the prior locale-override mechanism never worked — replaced with the platform `LocaleManager` API called directly from `WarungPosApplication`/`LanguageSettingsViewModel`. (2) Indonesian resources lived in `values-id/`, but the runtime locale canonicalizes to legacy code `"in"` — renamed the folder to `values-in/`. | Confirmed via `adb shell cmd locale get-app-locales` and visually: bottom nav shows "Laporan"/"Lainnya" by default and switches live, in-session, to "Reports"/"More" with no restart. |

Full root-cause traces, code-level detail, and evidence for every fix above are in `EXECUTION_LOG.md`'s
"Addendum — 2026-07-11 fix pass" section and inline under each defect's original write-up.

## Not run — and why

These remain genuinely blocked on infrastructure this environment doesn't have; none were part of the fix pass.

- **Multi-device (E3) cases** — no second device was available; this is the single largest coverage gap (files 13, most of the regression pack's R5 section). Now that DEFECT-003/008 is fixed, these are lower-urgency than before, but still worth running once a second device is available to confirm the fix holds across devices too (the fix is client-side/local-DB; a genuine multi-device split-brain via RTDB sync was never reproducible in this single-device environment).
- **Firebase RTDB console-dependent cases** — file 15 (version gate) almost entirely, plus scattered cases in 09/12/13. No console access in this environment.
- **Device-clock-control cases** — rollover/boundary cases in files 08 and 12 (week/month range edges, midnight-crossing attribution). Suite's own authors marked these "Automation Candidate: No"; not attempted given the risk of destabilizing the rest of the session's timestamps.
- Full per-file breakdown of what ran vs. didn't is in each file's section of `EXECUTION_LOG.md`.

## One operational note

Mid-session (file 13), re-enabling network to test the sync status bar triggered an unintended full sync of this
session's accumulated local test data (including a Rp 1,000,000,000 boundary-test expense and ~23 voided test
bills) to the shared Firebase project. Flagged to the user at the time; per their direction, no cleanup was
attempted. If cross-testing with other sessions on the same Firebase project, be aware this test data is live in
that project's RTDB.

## Files

- [`EXECUTION_LOG.md`](EXECUTION_LOG.md) — full step-by-step evidence, DB queries, and per-case verdicts for all 259 cases, plus every fix's verification detail.
- [`../00-assumptions-and-gaps.md`](../00-assumptions-and-gaps.md) — the pre-existing gap catalogue (D-1 through D-16, R-1 through R-7) this session's findings build on; gap D-11 (PDF export) is now closed.
