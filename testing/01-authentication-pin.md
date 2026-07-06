# 01 — Authentication (Local Username + PIN)

**Backing code:** `feature/auth/PinViewModel.kt`, `feature/auth/PinScreen.kt`, `core/common/SessionManager.kt`,
`WarungPosApp.kt` (gate: `isUnlocked` decides PIN screen vs main app), `feature/more/MoreViewModel.kt` (`lock()`).

**Model (verified):** No remote login. First run = REGISTER (username + PIN ≥ 4 digits + confirm). Later runs =
UNLOCK (PIN only). PIN is SHA-256 hashed in `EncryptedSharedPreferences` ("session_prefs"). Role is always
`OWNER`. The PIN gates the whole app on every **cold start** (`_isUnlocked` starts `false`). "Lock App" in More
returns to the PIN screen without clearing credentials.

> The PRD's `FR-AUTH` (Firebase email/password, roles) is **not implemented** — see `00-assumptions-and-gaps.md`
> §B. Cases here test the shipped PIN model. Cases TC-AUTH-090/091 explicitly verify the absence of the PRD auth.

---

### TC-AUTH-001 — First-run registration happy path
- **Feature / User Story:** As the operator, on first launch I set a username and PIN to secure the app.
- **Priority:** Critical | **Severity:** Blocker | **Type:** Functional
- **Preconditions / Baseline:** BL-0 fresh install (Clear storage or reinstall so no PIN exists).
- **Test Data:** username `Budi`, PIN `1357`, confirm `1357`.
- **Steps:**
  1. Launch the app.
  2. Observe the screen title `Warung POS` and subtitle `Set up your PIN`.
  3. Enter `Budi` in the **Username** field.
  4. Enter `1357` in the **PIN** field (rendered as dots).
  5. Enter `1357` in the **Confirm PIN** field.
  6. Tap **Create PIN**.
- **Expected Result:** The PIN screen is replaced by the main app with the **Order** tab (title "Orders") and a
  bottom navigation bar (Order / Reports / More). No error text is shown.
- **Postconditions:** `session_prefs` contains a username and a `pin_hash`; `isRegistered == true`. A Day is
  auto-opened (see Day tests).
- **Edge Case Notes:** REGISTER mode is chosen because `sessionManager.isRegistered` is false.
- **Automation Candidate:** Yes.

### TC-AUTH-002 — Registration blocked: blank username
- **Priority:** High | **Severity:** Major | **Type:** Negative / Validation
- **Preconditions:** BL-0.
- **Test Data:** username empty, PIN `1234`, confirm `1234`.
- **Steps:** 1. On the setup screen leave Username empty. 2. Enter PIN `1234` and Confirm `1234`. 3. Tap **Create PIN**.
- **Expected Result:** Inline error text `Please enter a username`. The app stays on the setup screen; no
  credentials are stored.
- **Postconditions:** Still unregistered.
- **Edge Case Notes:** `username.isBlank()` covers whitespace-only too (see TC-AUTH-003).
- **Automation Candidate:** Yes.

### TC-AUTH-003 — Registration: whitespace-only username is rejected; valid username is trimmed
- **Priority:** Medium | **Severity:** Minor | **Type:** Boundary
- **Preconditions:** BL-0.
- **Steps:**
  1. Enter Username `"   "` (3 spaces), PIN `1234`, Confirm `1234`, tap **Create PIN** → expect error `Please enter a username`.
  2. Change Username to `"  Budi  "` (leading/trailing spaces), keep PINs, tap **Create PIN**.
- **Expected Result:** Step 1 shows the blank-username error. Step 2 succeeds; the stored/displayed username is
  trimmed to `Budi` (verify on next lock screen "Hi, Budi" and in More header).
- **Edge Case Notes:** `register()` calls `username.trim()`.
- **Automation Candidate:** Yes.

### TC-AUTH-004 — Registration blocked: PIN shorter than 4 digits
- **Priority:** High | **Severity:** Major | **Type:** Boundary / Validation
- **Preconditions:** BL-0.
- **Test Data:** username `Budi`, PIN `123`, confirm `123`.
- **Steps:** 1. Enter username. 2. Enter PIN `123`. 3. Enter Confirm `123`. 4. Tap **Create PIN**.
- **Expected Result:** Error `PIN must be at least 4 digits`. No registration.
- **Edge Case Notes:** Boundary is exactly 4 (`MIN_PIN_LENGTH`). Test 4 digits passes (TC-AUTH-001), 3 fails.
- **Automation Candidate:** Yes.

### TC-AUTH-005 — Registration blocked: PIN and confirm mismatch
- **Priority:** High | **Severity:** Major | **Type:** Negative
- **Preconditions:** BL-0.
- **Test Data:** username `Budi`, PIN `1234`, confirm `1235`.
- **Steps:** 1. Enter username and PIN `1234`. 2. Enter Confirm `1235`. 3. Tap **Create PIN**.
- **Expected Result:** Error `PINs do not match`. No registration.
- **Automation Candidate:** Yes.

### TC-AUTH-006 — PIN field strips non-digit characters as typed
- **Priority:** Medium | **Severity:** Minor | **Type:** Input validation
- **Preconditions:** BL-0.
- **Steps:** 1. In the PIN field type `12ab-34#`. 2. Observe the accepted content (count the dots / reveal via confirm mismatch).
- **Expected Result:** Only digits are retained → effective PIN `1234`. Letters/symbols never enter the field
  (`onPinChange` filters `isDigit()`). Same behaviour for the Confirm field.
- **Edge Case Notes:** Keyboard is `NumberPassword`, but paste/IME could inject non-digits — filter must hold.
- **Automation Candidate:** Yes.

### TC-AUTH-007 — Very long PIN accepted (no max enforced)
- **Priority:** Low | **Severity:** Minor | **Type:** Boundary
- **Preconditions:** BL-0.
- **Test Data:** PIN = 32 digits `12345678901234567890123456789012`.
- **Steps:** 1. Register with the 32-digit PIN (username `Budi`, matching confirm). 2. Lock the app. 3. Unlock with the same 32-digit PIN.
- **Expected Result:** Registration and later unlock both succeed. No length cap beyond the ≥4 minimum.
- **Edge Case Notes:** Confirms no upper bound; hashing handles any length. Document if UX should cap length.
- **Automation Candidate:** Yes.

### TC-AUTH-010 — Unlock happy path after registration
- **Priority:** Critical | **Severity:** Blocker | **Type:** Functional
- **Preconditions:** Registered (TC-AUTH-001), app cold-started (fully closed and reopened).
- **Test Data:** PIN `1357`.
- **Steps:** 1. Cold start the app. 2. Observe subtitle `Hi, Budi` and a single **PIN** field (no username, no confirm). 3. Enter `1357`. 4. Tap **Unlock** (or press keyboard Done).
- **Expected Result:** App unlocks to the Order tab. No error.
- **Postconditions:** `isUnlocked == true` for this process.
- **Edge Case Notes:** UNLOCK mode chosen because `isRegistered` true. Done IME action triggers submit.
- **Automation Candidate:** Yes.

### TC-AUTH-011 — Unlock with wrong PIN shows error and clears field
- **Priority:** High | **Severity:** Major | **Type:** Negative
- **Preconditions:** Registered, on the UNLOCK screen.
- **Test Data:** wrong PIN `0000`.
- **Steps:** 1. Enter `0000`. 2. Tap **Unlock**.
- **Expected Result:** Error `Incorrect PIN`; the PIN field is **cleared** (`pin = ""`); app stays locked.
- **Edge Case Notes:** `unlock()` compares SHA-256 hashes. No lockout/attempt counter exists.
- **Automation Candidate:** Yes.

### TC-AUTH-012 — Repeated wrong PIN attempts: no lockout (documented risk)
- **Priority:** Medium | **Severity:** Major | **Type:** Negative / Security
- **Preconditions:** Registered, UNLOCK screen.
- **Steps:** 1. Enter a wrong PIN and tap Unlock. 2. Repeat 10 times with different wrong PINs.
- **Expected Result:** Each attempt shows `Incorrect PIN` and clears the field. **No lockout, no delay, no
  attempt cap** occurs (there is no rate limiting in `SessionManager`). App remains locked.
- **Edge Case Notes:** PRD OQ-11 says no inactivity lock, but brute-force resistance is unaddressed — raise as
  a security observation (Major). Verify it does not crash after many attempts.
- **Automation Candidate:** Yes.

### TC-AUTH-013 — Correct PIN after several wrong attempts still unlocks
- **Priority:** High | **Severity:** Major | **Type:** Negative → recovery
- **Preconditions:** Registered.
- **Steps:** 1. Enter wrong PIN ×3 (each shows error). 2. Enter the correct PIN `1357`. 3. Tap **Unlock**.
- **Expected Result:** App unlocks normally; prior failures do not block a correct entry.
- **Automation Candidate:** Yes.

### TC-AUTH-020 — Lock App returns to PIN screen without wiping credentials
- **Priority:** High | **Severity:** Major | **Type:** Functional / State
- **Preconditions:** Unlocked, on any tab.
- **Steps:** 1. Go to **More**. 2. Tap **Lock App**. 3. In the confirm dialog (`Lock the app and return to the PIN screen?`) tap **Lock**. 4. On the PIN screen, unlock with the existing PIN.
- **Expected Result:** After step 3 the UNLOCK screen appears (`Hi, Budi`), not the REGISTER screen. Step 4
  unlocks successfully — username/PIN are unchanged.
- **Postconditions:** `isUnlocked` false then true again; credentials intact.
- **Edge Case Notes:** `lock()` sets `_isUnlocked=false` only.
- **Automation Candidate:** Yes.

### TC-AUTH-021 — Cancel the Lock confirmation dialog
- **Priority:** Low | **Severity:** Minor | **Type:** Functional
- **Preconditions:** Unlocked, More tab.
- **Steps:** 1. Tap **Lock App**. 2. In the dialog tap **Cancel**.
- **Expected Result:** Dialog dismisses; app stays unlocked on More.
- **Automation Candidate:** Yes.

### TC-AUTH-022 — Lock App dialog dismissed by outside tap / back
- **Priority:** Low | **Severity:** Trivial | **Type:** UI
- **Preconditions:** Lock dialog open.
- **Steps:** 1. Tap outside the dialog (scrim) or press system Back.
- **Expected Result:** Dialog dismisses (onDismissRequest), app stays unlocked. No lock occurs.
- **Automation Candidate:** No (scrim tap is fiddly for some runners).

### TC-AUTH-030 — Lock persists across process death but not the username
- **Priority:** High | **Severity:** Critical | **Type:** Recovery
- **Preconditions:** Registered + unlocked.
- **Steps:** 1. From More, Lock the app (PIN screen shown). 2. Force-stop the app (`adb shell am force-stop com.wfx.warungpos`). 3. Relaunch.
- **Expected Result:** App launches to the **UNLOCK** screen (`Hi, Budi`), never auto-unlocked. Requires PIN.
- **Edge Case Notes:** `_isUnlocked` is non-persistent → always locked on a fresh process. Confirms no bypass
  via process restart.
- **Automation Candidate:** Yes (adb-driven).

### TC-AUTH-031 — Background → foreground does NOT re-lock (same process)
- **Priority:** Medium | **Severity:** Minor | **Type:** State / Lifecycle
- **Preconditions:** Unlocked, Order tab.
- **Steps:** 1. Press Home (app to background). 2. Wait 2 minutes. 3. Reopen from Recents.
- **Expected Result:** App resumes **unlocked** on the Order tab (no PIN re-prompt) because the process/VM
  survived. (There is intentionally no inactivity lock — PRD OQ-11.)
- **Edge Case Notes:** Contrast with TC-AUTH-030 (process death → locked). If the OS kills the process while
  backgrounded, reopening shows the PIN screen — acceptable.
- **Automation Candidate:** No (timing/OS-dependent).

### TC-AUTH-032 — Configuration change (rotation) does not drop the unlocked state
- **Priority:** Low | **Severity:** Minor | **Type:** State
- **Preconditions:** Unlocked. (App is portrait-only per PRD; if rotation is not locked in the manifest this
  still exercises recomposition.)
- **Steps:** 1. Trigger a configuration change (rotate device, or toggle dark mode, or change font size in system settings). 2. Return to the app.
- **Expected Result:** App remains unlocked; current screen state preserved.
- **Edge Case Notes:** If the app is truly portrait-locked, use font-size/dark-mode change to force config change.
- **Automation Candidate:** No.

### TC-AUTH-040 — Registration on the same device after Clear storage starts fresh
- **Priority:** Medium | **Severity:** Major | **Type:** Recovery / Data
- **Preconditions:** Previously registered as `Budi`.
- **Steps:** 1. System Settings → Apps → Warung POS → **Clear storage**. 2. Launch the app.
- **Expected Result:** REGISTER screen (`Set up your PIN`) appears again — the old username/PIN are gone
  (EncryptedSharedPreferences wiped). A brand-new `deviceId` will be generated on next write.
- **Edge Case Notes:** This is the deterministic reset used by other suites. Confirms no residual credential.
- **Automation Candidate:** No (system UI).

### TC-AUTH-041 — Reinstall (uninstall + install) requires re-registration
- **Priority:** Medium | **Severity:** Major | **Type:** Recovery / Multi-device
- **Preconditions:** Registered build installed.
- **Steps:** 1. `adb uninstall com.wfx.warungpos`. 2. `./gradlew installDebug` (or reinstall APK). 3. Launch.
- **Expected Result:** REGISTER screen appears; local credentials do not survive uninstall. (Room data also
  gone — data must be recovered via RTDB sync if configured; see `13-sync-multidevice.md`.)
- **Automation Candidate:** No.

### TC-AUTH-050 — Fast double-tap on Create PIN does not double-register or crash
- **Priority:** Medium | **Severity:** Major | **Type:** Negative / User behaviour
- **Preconditions:** BL-0, valid inputs entered.
- **Steps:** 1. Fill valid username + matching PIN. 2. Rapidly double-tap **Create PIN**.
- **Expected Result:** App transitions to the main app exactly once; no crash, no duplicate user, no stuck
  state. (Second tap lands after `_isUnlocked` becomes true → PIN screen already gone.)
- **Automation Candidate:** Yes.

### TC-AUTH-051 — Fast double-tap Unlock with correct PIN
- **Priority:** Low | **Severity:** Minor | **Type:** Negative
- **Preconditions:** Registered.
- **Steps:** 1. Enter correct PIN. 2. Double-tap **Unlock**.
- **Expected Result:** Unlocks once; no crash; no double navigation.
- **Automation Candidate:** Yes.

### TC-AUTH-052 — Back button on the PIN screen does not bypass the gate
- **Priority:** High | **Severity:** Critical | **Type:** Negative / Security
- **Preconditions:** Registered, on the UNLOCK screen (e.g. after Lock App).
- **Steps:** 1. Press the system Back button repeatedly.
- **Expected Result:** Back either does nothing on the gate or minimises/closes the app; it **never** reveals
  the main app without a correct PIN. (The PIN screen is rendered *instead of* the NavHost when locked.)
- **Edge Case Notes:** Confirms the lock is a composition gate, not a navigable route that Back can pop.
- **Automation Candidate:** Yes.

### TC-AUTH-090 — Verify no email/password login exists (PRD FR-AUTH absence)
- **Priority:** Medium | **Severity:** Major | **Type:** Negative / Gap verification
- **Preconditions:** BL-0.
- **Steps:** 1. Complete first-run. 2. Inspect all auth-related screens and Settings/More.
- **Expected Result:** There is **no** email field, password field, "Login", "Logout", or account screen for
  end users. (`LoginRoute` renders only placeholder text and is not reachable from nav.) Confirms FR-AUTH is
  superseded by the local-PIN model.
- **Edge Case Notes:** Cross-reference gap B. Product owner must accept this scope change.
- **Automation Candidate:** No.

### TC-AUTH-091 — Verify single role: no owner/staff distinction, all features visible
- **Priority:** Medium | **Severity:** Major | **Type:** Gap verification
- **Preconditions:** Registered + unlocked.
- **Steps:** 1. Inspect the bottom nav and the More list.
- **Expected Result:** **Reports** tab is present (role is OWNER), and **all** owner-only sections (Menu
  Management, Payment Methods, Expense Categories, Language, Close Day, About) are visible. There is no way to
  become a staff/restricted user. Confirms the PRD permission matrix is not enforceable in-app.
- **Edge Case Notes:** The More header chip reads `Owner`.
- **Automation Candidate:** Yes.

### TC-AUTH-092 — EncryptedSharedPreferences works on minSdk (API 26)
- **Priority:** Medium | **Severity:** Critical | **Type:** Compatibility
- **Preconditions:** E5 API 26 device/emulator, BL-0.
- **Steps:** 1. Register a PIN. 2. Cold start. 3. Unlock.
- **Expected Result:** Registration, hashing, and unlock all succeed on API 26 (no `MasterKey`/keystore crash).
  App does not crash creating `session_prefs` or `app_prefs`.
- **Edge Case Notes:** `security-crypto` alpha on old APIs is a known crash source — explicitly covered.
- **Automation Candidate:** No (device-specific).
