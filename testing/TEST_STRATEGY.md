# TEST STRATEGY — Warung POS

Companion to [`TEST_PLAN.md`](TEST_PLAN.md). Defines *how* we test, the conventions every case follows, and
the risk-based reasoning behind coverage depth.

---

## 1. Testing approach

Primarily **black-box, behaviour-driven E2E** against the running app, informed by **white-box knowledge** of
the code (repositories, use cases, DAOs) so that expected results are precise and observable. Where a result is
only observable in storage (Room / RTDB), the case says exactly what to inspect and how (adb `sqlite3` / RTDB
console).

We deliberately combine four viewpoints per feature:
- **Manual QA** — taps, gestures, fast/double taps, back button, mid-flow cancel.
- **Automation engineer** — deterministic steps + stable selectors (Compose text/semantics) so cases can be
  ported to Espresso/Compose UI tests.
- **Product owner** — business rules (day close blocking, void audit, cash variance).
- **End user** — real warung conditions: two operators, flaky mobile data, phone reboots, battery kills.

## 2. Test levels & where they run

| Level | Target | Tooling | Notes |
|-------|--------|---------|-------|
| L1 Unit (reference only) | Use cases, `CalculateChangeUseCase`, `ConflictResolver`, DAOs | JUnit + Turbine + fakes; Room in-memory | Already partially present under `app/src/test` & `androidTest`. This suite does **not** write unit tests but references the same rules. |
| L2 E2E UI | Whole app on device/emulator | Manual + Compose UI test candidates | The bulk of this suite. |
| L3 Multi-device / sync | 2 devices + Firebase | Manual | Data-integrity focus. |
| L4 Recovery/lifecycle | 1 device | Manual (adb: `am kill`, force-stop, reboot) | Persistence & crash-safety. |

## 3. Test case conventions

Every case in the feature files uses this exact structure:

```
### <ID> — <Title>
- **Feature / User Story:** …
- **Priority:** Critical | High | Medium | Low
- **Severity (if it fails):** Blocker | Critical | Major | Minor | Trivial
- **Type:** Functional | Negative | Boundary | State | UI | Recovery | Sync | Perf-scenario
- **Preconditions / Baseline:** … (references BL-0..BL-3 from TEST_PLAN §7)
- **Test Data:** …
- **Steps:** numbered, one action each
- **Expected Result:** observable, specific (screen, control state, value, message, persisted record)
- **Postconditions:** state the app/data is left in
- **Edge Case Notes:** boundary reasoning, PRD-deviation flags
- **Automation Candidate:** Yes | No (+ why)
```

**ID scheme:** `TC-<AREA>-<nnn>`, where AREA ∈
{AUTH, ONB, ORD, PAY, VOID, MENU, PM, DAY, EXP, STK, OPN, RPT, SYNC, SET, VER, NAV, REG}.

## 4. Determinism rules (for AI/automation execution)

- Never write "verify it works". Always name the screen, the control, and the exact value/message.
- Reference **visible text** that exists in the code (e.g. button text `Pay`, `Confirm Payment`, `Void Item`,
  `Close Day`, `Lock App`, empty-state `No open orders`). These are currently **hardcoded English** regardless
  of language setting (gap D-4), so text assertions are stable across languages **today**.
- Where a value depends on currency formatting, assert the **integer Rupiah amount** and the presence of the
  `Rp` prefix; only assert exact grouping if `CurrencyFormatter` output has been confirmed for that case.
- For storage assertions, prefer the RTDB console (online) or `adb shell run-as com.wfx.warungpos` +
  `sqlite3` on the Room DB (debug build) — state the table and predicate.

## 5. Risk-based coverage weighting

Depth is concentrated where failure is most costly for a cash-handling POS:

| Risk area | Why | Coverage depth |
|-----------|-----|----------------|
| Payment & change / money math | Direct cash loss; `Long`-only invariant | **Exhaustive** (boundaries, overpay/underpay, zero, huge, void-then-pay) |
| Day close & cash variance / Z-report immutability | Reconciliation integrity | **Exhaustive** (blocking bills, rollover, auto vs manual, attribution) |
| Void audit (item & bill) | Fraud vector (staff pocketing) | High (reason required, OTHER note, excluded from totals, PAID-bill guard) |
| Sync / multi-device data integrity | Lost/duplicated/stale records | High (append-only items, LWW, status regression guard, PENDING flush) |
| Local auth / lock | Unauthorized access, lockout | High (wrong PIN, min length, mismatch, lock/unlock persistence) |
| Order building & sold-out | Wrong orders; selling unavailable items | High (variant required, increment vs new line, sold-out gap D-1) |
| Stock / opname | Inventory correctness | Medium (deduction, variance reason, single active session, gap D-14) |
| Reports / export | Decision support | Medium (range math, attribution gap D-13, CSV content) |
| Settings / i18n | Config & language | Medium (payment toggle, language gap D-4) |
| Navigation / lifecycle | Crash-safety, state loss | High (back stack, process death, config change, force-close) |

## 6. Entry/verification for storage-level checks (debug build)

```bash
# Pull the Room DB (debug/ debuggable build only)
adb shell run-as com.wfx.warungpos sh -c 'ls /data/data/com.wfx.warungpos/databases'
adb shell run-as com.wfx.warungpos sqlite3 /data/data/com.wfx.warungpos/databases/<db> \
  "SELECT id,status,shiftId,grandTotal,paidAt,syncStatus FROM bills;"
```
RTDB inspection: Firebase console → Realtime Database → Data → `/bills`, `/orderItems`, `/payments`, `/days`
(node named `shifts` per `RtdbPaths`), `/expenses`, `/stockItems`, `/opnames`.

## 7. Defect classification

- **Blocker:** crash or data loss in Order/Payment/Day-close; app unusable.
- **Critical:** wrong money total, lost/duplicated financial record, unable to close day, security bypass.
- **Major:** business rule violated (e.g. sellable sold-out item, PAID bill not voidable when required), sync
  drops a non-financial field.
- **Minor:** cosmetic, label/i18n, non-blocking UX.
- **Trivial:** wording, spacing.

## 8. Suspension / resumption

Suspend a suite if a Blocker prevents progress (e.g. cannot pass PIN screen, cannot create a bill). Resume
after fix + targeted re-run of the affected area plus the regression pack.

## 9. Traceability

Every documented PRD requirement (FR-*/NFR-*) and every confirmed deviation (gaps B–F in
`00-assumptions-and-gaps.md`) maps to ≥1 Test Case ID in [`TEST_COVERAGE.md`](TEST_COVERAGE.md). Requirements
that are **not implemented** map to a case that asserts their **absence** (so the gap is verified, not assumed).
