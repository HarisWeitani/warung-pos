# 16 — Navigation, App Lifecycle & State Management

**Backing code:** `WarungPosApp.kt` (gate + bottom nav + Scaffold), `core/navigation/AppNavGraph.kt`,
`core/navigation/Routes.kt`, per-screen ViewModels using `stateIn(WhileSubscribed(5_000))` and
`SavedStateHandle.toRoute<>()`.

**Behaviour (verified):** Bottom nav = **Order / Reports / More** (Reports shown because role is always OWNER).
Bottom nav is shown only on the three top-level destinations; detail/settings screens hide it. Nav uses
type-safe routes; `PaymentRoute`/`BillDetailRoute`/`ZReportRoute` carry a String id. Bottom-nav clicks use
`popUpTo<OrderRoute>{saveState}` + `launchSingleTop` + `restoreState`. Screens read reactive state from Room via
`stateIn`; the cart concept does not exist (bill is persisted immediately).

---

## Navigation

### TC-NAV-001 — Bottom nav switches between top-level tabs
- **Priority:** High | **Severity:** Major | **Type:** Navigation
- **Steps:** 1. Tap Reports. 2. Tap More. 3. Tap Order.
- **Expected Result:** Each tab shows its screen; the bottom bar highlights the current tab. Order is the start
  destination.
- **Automation Candidate:** Yes.

### TC-NAV-002 — Bottom nav is hidden on detail/settings screens
- **Priority:** Medium | **Severity:** Minor | **Type:** UI
- **Steps:** 1. Open a bill (Bill Detail). 2. Open Payment. 3. Open More → Menu Management.
- **Expected Result:** No bottom nav bar on Bill Detail, Payment, Menu Management, etc. (only Order/Reports/More
  show it). Back returns appropriately.
- **Automation Candidate:** Yes.

### TC-NAV-003 — Tab state is preserved when switching tabs (saveState/restoreState)
- **Priority:** Medium | **Severity:** Major | **Type:** State
- **Steps:** 1. On Order, scroll the open-bills list. 2. Switch to More, then back to Order.
- **Expected Result:** Order restores its prior scroll/state (bottom nav uses `saveState`/`restoreState`).
- **Edge Case Notes:** `launchSingleTop` prevents stacking duplicate top-level destinations.
- **Automation Candidate:** No.

### TC-NAV-004 — Back from a top-level tab exits or returns to Order
- **Priority:** Medium | **Severity:** Minor | **Type:** Navigation
- **Steps:** 1. From Order press Back.
- **Expected Result:** The app minimises/exits (Order is the start destination; nothing to pop). No crash.
- **Automation Candidate:** Yes.

### TC-NAV-005 — Deep back stack unwinds cleanly
- **Priority:** Medium | **Severity:** Major | **Type:** Navigation
- **Steps:** 1. Order → open bill → Payment. 2. Back → Back → Back.
- **Expected Result:** Payment → Bill Detail → Order, then exit. No orphaned screens; no crash.
- **Automation Candidate:** Yes.

### TC-NAV-006 — Reports → Dashboard → Full Report → Best Sellers and back
- **Priority:** Low | **Severity:** Minor | **Type:** Navigation
- **Steps:** 1. Reports → Dashboard → Full Report → Best Sellers. 2. Back through all.
- **Expected Result:** Each screen appears; Best Sellers shares the Full Report ViewModel; Back unwinds to
  Reports. No crash.
- **Automation Candidate:** Yes.

### TC-NAV-007 — Payment success returns to Order (popUpTo), bill not re-openable from back stack
- **Priority:** High | **Severity:** Major | **Type:** Navigation / State
- **Steps:** 1. Pay a bill. 2. After returning to Order, press Back.
- **Expected Result:** After success the stack is popped up to Order (`popUpTo OrderRoute inclusive=false`).
  Pressing Back from Order exits; you cannot Back into the just-paid bill's Payment screen.
- **Automation Candidate:** Yes.

### TC-NAV-008 — Rapid tab tapping does not stack destinations or crash
- **Priority:** Medium | **Severity:** Major | **Type:** User behaviour
- **Steps:** 1. Rapidly tap Order/Reports/More repeatedly.
- **Expected Result:** No crash, no duplicated back-stack entries (`launchSingleTop`). The last-tapped tab wins.
- **Automation Candidate:** Yes.

### TC-NAV-009 — Navigate to Bill Detail for a bill that gets voided/paid elsewhere auto-pops
- **Priority:** Medium | **Severity:** Major | **Type:** State / Sync
- **Preconditions:** E3 or a second flow. Bill open in detail on this device.
- **Steps:** 1. Have the bill become PAID or VOID (another device or by paying it). 2. Observe the detail screen.
- **Expected Result:** The detail screen auto-pops back (LaunchedEffect on `isBillPaid || billVoided`). No
  further edits possible.
- **Automation Candidate:** No.

## App lifecycle & recovery

### TC-NAV-020 — Backgrounding releases Room Flows after 5s; foregrounding re-subscribes
- **Priority:** Low | **Severity:** Minor | **Type:** State / Performance-scenario
- **Steps:** 1. On Order, press Home for >5s. 2. Reopen.
- **Expected Result:** State re-collects and shows current data (WhileSubscribed(5_000) unsubscribes then
  re-subscribes). No stale/blank list; no crash.
- **Automation Candidate:** No.

### TC-NAV-021 — Process death restores the current screen's data from Room
- **Priority:** High | **Severity:** Critical | **Type:** Recovery
- **Preconditions:** On a Bill Detail with 2 items.
- **Steps:** 1. Background the app. 2. `adb shell am kill com.wfx.warungpos`. 3. Reopen from Recents. 4. Unlock.
- **Expected Result:** App relaunches to the PIN screen (locked), then after unlock the nav restores; navigating
  to the bill shows the same items (billId restored from route/SavedStateHandle, data from Room). No data loss.
- **Automation Candidate:** No.

### TC-NAV-022 — Configuration change mid-flow preserves in-progress input where expected
- **Priority:** Medium | **Severity:** Major | **Type:** State
- **Preconditions:** On the Payment screen with a tender typed.
- **Steps:** 1. Force a config change (font size / dark mode / rotation if allowed).
- **Expected Result:** The screen survives recomposition. Persisted data (bill, methods) is intact. Note whether
  the transient tender text survives — it lives in ViewModel `_uiState`, which survives config change, so it
  should persist. If it resets, log Minor.
- **Automation Candidate:** No.

### TC-NAV-023 — Force-close mid-order does not lose already-added items
- **Priority:** High | **Severity:** Critical | **Type:** Recovery / Data integrity
- **Preconditions:** A bill with 3 items added.
- **Steps:** 1. Force-stop the app while on the bill. 2. Relaunch, unlock, open the bill from the Order list.
- **Expected Result:** All 3 items are present (each was persisted to Room on add — there is no unsaved cart).
  Total correct. No loss.
- **Edge Case Notes:** This is a key advantage of the bill-first (no-cart) design — items are never held only in
  memory.
- **Automation Candidate:** No.

### TC-NAV-024 — Reopen from Recents (warm start) keeps the session unlocked
- **Priority:** Low | **Severity:** Minor | **Type:** Lifecycle
- **Steps:** 1. Home out, reopen from Recents quickly (process alive).
- **Expected Result:** App resumes unlocked on the same screen (see TC-AUTH-031).
- **Automation Candidate:** No.

### TC-NAV-025 — Long idle then interact (no session timeout)
- **Priority:** Low | **Severity:** Minor | **Type:** Lifecycle
- **Steps:** 1. Leave the app foregrounded and idle for 30+ minutes. 2. Interact.
- **Expected Result:** App is still usable and unlocked (no inactivity lock per PRD OQ-11); data current. If the
  OS killed the process, reopening shows the PIN screen (acceptable).
- **Automation Candidate:** No.

### TC-NAV-026 — Empty/loading/success/error states render per screen
- **Priority:** Medium | **Severity:** Major | **Type:** State
- **Steps:** 1. Observe: Order empty ("No open orders"); Bill Detail empty ("No items yet…"); Dashboard empty
  (zeros); Payment loading spinner on confirm; a void error dialog.
- **Expected Result:** Each state renders its intended UI without flicker to a wrong state. No screen shows a
  perpetual spinner or a blank white screen.
- **Automation Candidate:** Yes (per-state).

### TC-NAV-027 — No ANR on the main thread during DB writes
- **Priority:** Medium | **Severity:** Major | **Type:** Performance-scenario / Reliability
- **Steps:** 1. Perform heavy actions (add many items rapidly, close a busy day). 2. Watch for jank/ANR.
- **Expected Result:** UI stays responsive; Room work runs off the main thread (NFR-PERF). No ANR dialog.
- **Automation Candidate:** No.
