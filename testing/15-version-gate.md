# 15 — Minimum Version Gate

**Backing code:** `AppViewModel.kt` (`checkVersionGate`, `VersionGateState`), `WarungPosApp.kt`
(renders `UpdateRequiredScreen` when `UpdateRequired`), `feature/auth/UpdateRequiredScreen.kt`,
`BuildConfig.VERSION_CODE`, RTDB `appConfig/minVersionCode`.

**Behaviour (verified):** On startup, if online, the app reads `appConfig/minVersionCode` (5s timeout). If
`BuildConfig.VERSION_CODE < minVersionCode` → **UpdateRequired**; else Allowed. If offline, or the read fails/
times out, or the value is missing → **Allowed** (fails open). The check is **non-blocking**: the PIN screen
shows while it runs (state starts Loading, treated as not-UpdateRequired); if it later resolves to
UpdateRequired, the update screen takes over the whole UI. The update screen is **non-dismissable** (Back is
consumed) and shows the current version. This build: `VERSION_CODE = 1`.

Environment: E1 online with console access to set `minVersionCode`.

---

### TC-VER-001 — Up-to-date app is allowed
- **Priority:** High | **Severity:** Major | **Type:** Functional
- **Preconditions:** RTDB `appConfig/minVersionCode = 1`; app versionCode 1; online.
- **Steps:** 1. Cold-start the app.
- **Expected Result:** Normal flow (PIN → main app). No update screen (1 ≥ 1 → Allowed).
- **Automation Candidate:** No (console setup).

### TC-VER-002 — Below minimum shows the non-dismissable update screen
- **Priority:** High | **Severity:** Critical | **Type:** Functional / Negative
- **Preconditions:** Set `appConfig/minVersionCode = 2` (> app's 1); online.
- **Steps:** 1. Cold-start. 2. Wait for the version check to resolve. 3. Press Back repeatedly.
- **Expected Result:** The **Update Available** screen (`update_required_title`) replaces the app with message
  `Please ask the store owner for the latest APK.` and `v1.0 (1)`. Back does **nothing** (consumed). No access
  to data/PIN/main app.
- **Edge Case Notes:** Because the check is non-blocking, the PIN screen may flash first, then be replaced by
  the update screen once the RTDB read completes. Verify the transition and that the app cannot be used in the
  interim to mutate data (it can only sit on the PIN screen briefly).
- **Automation Candidate:** No.

### TC-VER-003 — Offline start skips the gate (Allowed)
- **Priority:** High | **Severity:** Critical | **Type:** Offline
- **Preconditions:** `minVersionCode = 2`; device **offline**.
- **Steps:** 1. Cold-start offline.
- **Expected Result:** App is **Allowed** (gate skipped when `!isOnline`). The store can keep operating offline
  even if a higher min-version is configured. (Documented fail-open behaviour.)
- **Edge Case Notes:** Security/rollout implication: a device can dodge the gate by going offline. Note for the
  owner (Info, not a defect per current design).
- **Automation Candidate:** No.

### TC-VER-004 — Missing minVersionCode defaults to allowed
- **Priority:** Medium | **Severity:** Major | **Type:** Boundary / Config
- **Preconditions:** `appConfig/minVersionCode` **absent**; online.
- **Steps:** 1. Cold-start.
- **Expected Result:** Allowed (the read returns null → default `1L`; 1 < 1 is false). No update screen.
- **Automation Candidate:** No.

### TC-VER-005 — RTDB unreachable / timeout defaults to allowed
- **Priority:** Medium | **Severity:** Major | **Type:** Reliability
- **Preconditions:** Online but RTDB slow/unreachable (e.g. wrong rules or throttled).
- **Steps:** 1. Cold-start; let the 5s timeout elapse.
- **Expected Result:** `checkVersionGate` catches the exception/timeout → Allowed. App proceeds; no crash, no
  indefinite block.
- **Automation Candidate:** No.

### TC-VER-006 — Equal version is allowed (boundary)
- **Priority:** Low | **Severity:** Minor | **Type:** Boundary
- **Preconditions:** `minVersionCode = 1`, app versionCode 1.
- **Expected Result:** Allowed (strictly-less-than comparison; equal passes).
- **Automation Candidate:** No.

### TC-VER-007 — Update screen persists across relaunch until version is raised
- **Priority:** Medium | **Severity:** Major | **Type:** Persistence
- **Preconditions:** `minVersionCode = 2`; online.
- **Steps:** 1. Cold-start (update screen). 2. Force-stop, relaunch (still online).
- **Expected Result:** Update screen shows again on every online launch while the app is below the minimum. Only
  installing a build with versionCode ≥ 2 clears it.
- **Automation Candidate:** No.
