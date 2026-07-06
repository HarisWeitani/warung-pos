# TEST PLAN — Warung POS

**Product:** Warung POS — personal offline-first Android point-of-sale for an Indonesian food stall
**Build under test:** `com.wfx.warungpos` versionCode 1 / versionName 1.0 (DB schema v3)
**Plan owner:** QA (Principal QA Engineer)
**Related docs:** [`TEST_STRATEGY.md`](TEST_STRATEGY.md), [`TEST_COVERAGE.md`](TEST_COVERAGE.md),
[`00-assumptions-and-gaps.md`](00-assumptions-and-gaps.md)

---

## 1. Purpose

Approve (or block) a production release of Warung POS. The suite is designed to **discover realistic ways the
app can fail** across functional flows, user behaviour, network/offline, local auth, multi-device sync, data
integrity, state management, UI, and recovery — not merely to demonstrate the happy path.

## 2. Scope

### In scope
- Local PIN auth (register / unlock / lock) — see `01-authentication-pin.md`
- First-run seeding & onboarding — `02-onboarding-firstrun.md`
- Order & bill lifecycle (create, add items, variants, void, pay) — `03-order-and-bills.md`
- Payment & change calculation — `04-payment.md`
- Void item / void bill & audit — `05-void.md`
- Menu management (items, categories, variants, recipes, sold-out, hide) — `06-menu-management.md`
- Payment method settings — `07-payment-methods-settings.md`
- Day (shift) management: auto-open, manual close, rollover auto-close, Z-report, history — `08-day-management.md`
- Expense logging — `09-expenses.md`
- Stock items & batches — `10-stock.md`
- Stock opname (physical count / variance) — `11-stock-opname.md`
- Reports & dashboard & CSV export — `12-reports-dashboard.md`
- Sync status & multi-device / offline / reconnection — `13-sync-multidevice.md`
- Settings, language/i18n — `14-settings-language-i18n.md`
- Version gate (minimum version) — `15-version-gate.md`
- Navigation, app lifecycle, state restoration — `16-navigation-state-lifecycle.md`
- Regression pack (release gate) — `17-regression.md`

### Out of scope (not present / superseded)
- Firebase email/password login, owner/staff roles, custom claims (**not implemented** — see gap B).
- Dine-in tables / open running tabs (removed by AM-2).
- PDF export, per-item COGS margin report, discounts/promos, kitchen queue (Phase 2/3 — not shipped).
- Play Store distribution, in-app APK update (manual sideload only).
- Load/performance benchmarking (scenarios are *identified* per the brief, but not measured).

## 3. Test items & references

Each feature file groups its test cases and lists the exact source files that back the expected results, so an
executor can reproduce logic without re-reading the whole codebase.

## 4. Entry criteria

- Debug APK installs on the target device/emulator (`./gradlew installDebug`).
- For sync/version-gate/multi-device suites: a Firebase project with RTDB enabled, **Anonymous** sign-in
  enabled, `appConfig/minVersionCode` seeded, and `firebase/database.rules.json` deployed
  (see `docs/firebase-setup.md`). Confirm `app/google-services.json` is present.
- For local-only suites: no Firebase needed (app runs fully offline).
- A known clean state is achievable via **Settings → Apps → Warung POS → Clear storage** (wipes Room +
  EncryptedSharedPreferences + first-run flag), or reinstall.

## 5. Exit criteria (release gate)

- 100% of **Critical** and **High** priority cases executed.
- Zero open **Blocker/Critical** severity defects in Order, Payment, Void, Day-close, or Sync data-integrity.
- No crash observed in any Order or Payment path (NFR-RELIABILITY).
- All data-integrity cases in `13-sync-multidevice.md` and `16-navigation-state-lifecycle.md` pass
  (no lost/duplicated/stale records after logout-lock, restart, force-close, reconnect).
- Every documented deviation in `00-assumptions-and-gaps.md` is either accepted by the product owner or has a
  tracked defect.

## 6. Test environments / devices

| Env | Purpose |
|-----|---------|
| E1 — single emulator, **online**, Firebase configured | Functional + sync happy paths |
| E2 — single device, **airplane mode** | Offline capability, PENDING queue, reconnection |
| E3 — **two physical devices**, same Firebase project | Multi-device propagation, conflict, dedup |
| E4 — device with system clock control | Day rollover / auto-close, custom date-range reports |
| E5 — API 26 (min) device + a recent API device | minSdk compatibility, EncryptedSharedPreferences on old API |

## 7. Data setup baselines

- **BL-0 Fresh install:** no username/PIN, empty DB except 5 seeded payment methods
  (`pm_tunai` cash, `pm_qris`, `pm_gopay`, `pm_ovo`, `pm_transfer`).
- **BL-1 Registered, empty menu:** PIN set, no menu categories/items.
- **BL-2 Seeded menu:** ≥2 categories, several items (some with variant groups, one required group; one item
  with a recipe/ingredient), used across order/payment/report tests.
- **BL-3 Active trading day:** an OPEN day with a handful of PAID bills, ≥1 expense, ≥1 void, for reports/close.

Each feature file states which baseline its **Preconditions** assume.

## 8. Schedule / prioritisation

1. Critical smoke (regression pack `17-regression.md`) on every build.
2. Full functional sweep on release-candidate builds.
3. Multi-device / offline / recovery suites before any release that touches data or sync.

## 9. Risks to the test effort

- Multi-device and rollover cases are **manual** and time-sensitive (clock changes, two devices).
- Sync timing is non-deterministic (WorkManager backoff, RTDB latency) — cases specify observable end-states
  and generous settle windows rather than exact timings.
- The large PRD-vs-code gap means some "expected results" intentionally assert current (arguably wrong)
  behaviour; executors must not "fix" expectations to match the PRD.

## 10. Deliverables

- Executed results per case (Pass/Fail/Blocked) with evidence (screenshots, RTDB console snapshots, adb logs).
- Defect reports linked to Test Case IDs.
- Updated traceability matrix in `TEST_COVERAGE.md`.
