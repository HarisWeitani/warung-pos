# Warung POS — QA Handoff

E2E test suite (17 files, 259 authored cases) executed against a running debug build on a single Android
emulator (API 33), primarily offline, over a multi-day session. Full step-by-step evidence for every case is in
[`EXECUTION_LOG.md`](EXECUTION_LOG.md) (~3,100 lines) — this document is the scannable summary for whoever picks
up this project next.

**Scope note:** this began as a test-execution pass only. Three follow-up sessions then worked the defect list:
- **2026-07-10** — DEFECT-001 and DEFECT-002 (Auth / Lock App) fixed.
- **2026-07-11 (fix pass)** — every other then-open defect (DEFECT-003/008 through DEFECT-015) fixed.
- **2026-07-11 (two-device session)** — a second emulator became available, unblocking E3 (multi-device) cases
  for the first time. Found **DEFECT-016**, a new defect distinct from the earlier fixes — see below. Committed
  as [`1bb45c2`](../../.git) — the 14 defects above are no longer just "uncommitted fixes," they're in `main`.

**15 numbered defects found in total (DEFECT-001 through DEFECT-016). 14 are fixed and committed; DEFECT-016 is
open and logged for the product owner, not fixed in-session (see below).** DEFECT-003 and DEFECT-008 are the
same root-cause issue found twice. DEFECT-012 was never assigned — a candidate finding during file 11 was folded
into DEFECT-011 instead of becoming its own entry, so the numbering has a deliberate gap, not a missing write-up.

## Release-gate verdict

**Meets the suite's own release-gate condition for single-device use** (every Critical/High regression-pack row
Pass; zero Blocker/Critical defects open in Order, Payment, Void, Day-close, or Sync on one device). The sole
Critical blocker (DEFECT-003/008, multiple concurrent OPEN shifts on one device) is fixed and committed; every
Major/High defect touching money-visible screens is also fixed and committed. Full unit + instrumented test
suites pass (128 + 35 tests, zero failures).

**Does not yet meet the gate for multi-device use.** DEFECT-016 (below) is a Major data-integrity defect that
only manifests with two-or-more devices sharing a Firebase project: a real open bill's revenue can become
permanently unreachable from Close Day/Z-report. Any store actually running Warung POS on more than one device
should not go live until this is resolved or a mitigation (e.g. a periodic "orphaned open bill" audit) is in
place.

## New defect — found this session, not fixed

| ID | Area | Finding | Why it wasn't fixed here |
|----|------|---------|---------------------------|
| **DEFECT-016** | Sync / Day-Shift (multi-device) | The shared Firebase project has accumulated 24 separate OPEN shift rows over this engagement's history (across many past sessions/reinstalls). `getOpenShift()` always resolves to the single most-recently-opened one, so both devices agree on "the current shift" — but a real, live, unpaid Rp 55.000 bill is attached to a *different* OPEN shift that's no longer reachable via Close Day or the Z-report. Orders (by design) still shows it, since it's shift-agnostic, but there is no in-app path to ever close it out or have it counted in revenue. Confirmed identically on both `emulator-5554` and `emulator-5556` by pulling and querying each device's Room DB directly. | This is the concrete, harmful expression of a gap the test suite already documents and explicitly scopes as "outside the intended operating envelope" (TC-SYNC-050, gap R-1: no server-side single-open-day enforcement). Fixing it correctly requires a product/architecture decision — a reconciliation policy for inbound OPEN-shift sync, a cleanup job, or rescoping Close Day — not a same-pattern bug fix like the 14 above. Per user direction, logged for the product owner rather than fixed speculatively. |

Full repro evidence (DB query output, screenshots) is in `EXECUTION_LOG.md`'s "Addendum — 2026-07-11 second
session" at the end of the file.

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

- **Multi-device (E3) cases, partial** — a second emulator became available 2026-07-11. TC-ONB-007 (no duplicate payment methods) was run and passes. TC-SYNC-050 (split-brain shift) was substantively addressed — the shared Firebase project's already-accumulated shift history stood in for a clean synthetic race, and surfaced DEFECT-016 (see above). TC-ORD-053/054, TC-PAY-020, TC-MENU-038, TC-PM-007, TC-NAV-009, and the regression pack's R5 rows are still NOT RUN — deprioritized once DEFECT-016 surfaced, and would need a fresh (uncontaminated) Firebase project to get a clean read anyway, since these two devices now carry the same 24-shift backlog.
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
