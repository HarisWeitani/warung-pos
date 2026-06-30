# Firebase Setup

One-time setup required before the app is usable. Do these in order — later steps depend on
earlier ones.

## 1. Create the Firebase project and Android app

1. In the [Firebase console](https://console.firebase.google.com/), create a project (or use an
   existing one).
2. Add an Android app with package name `com.wfx.warungpos`.
3. Download `google-services.json` and place it at `app/google-services.json` in this repo
   (gitignored — never commit it).
4. Enable **Authentication → Sign-in method → Email/Password**.
5. Enable **Realtime Database** (choose a region close to the store).

## 2. Create Auth accounts

In **Authentication → Users**, add one account per person who will use the app:

1. Owner account — the store owner's email + a strong password.
2. Staff account(s) — one per staff member's email + password.

Note each user's **UID** (shown in the Users table) — you'll need it for the next step.

## 3. Set role custom claims

Roles (`owner` / `staff`) are read from a Firebase Auth **custom claim**, not stored in the
database. The Android app reads `idTokenResult.claims["role"]` after sign-in
(`SessionManager.refreshRole()`); a missing or unrecognized claim resolves to `UserRole.NONE`,
which blocks access to owner-only screens (Reports, Dashboard, Menu Management, Shift Close,
Settings) and shows "Guest" in More.

Custom claims can only be set server-side. The simplest path is the Firebase Admin SDK via a
short Node.js script run locally (you need a service account key — **Project Settings → Service
Accounts → Generate new private key**):

```js
// set-role.js — run with: node set-role.js <uid> <owner|staff>
const admin = require("firebase-admin");
admin.initializeApp({ credential: admin.credential.cert(require("./serviceAccountKey.json")) });

const [uid, role] = process.argv.slice(2);
admin.auth().setCustomUserClaims(uid, { role }).then(() => {
  console.log(`Set role=${role} for ${uid}`);
  process.exit(0);
});
```

Run it once per user:
```bash
node set-role.js <owner-uid> owner
node set-role.js <staff-uid> staff
```

Claims are cached in the ID token and refresh roughly every hour, or immediately on next sign-in.
**Verify on device:** sign in as the owner account → confirm Reports/Dashboard/Settings are
visible and the More screen shows "Owner". Sign in as staff → confirm those routes redirect away
and More shows "Staff".

## 4. Seed `appConfig`

In **Realtime Database → Data**, manually add:
```
appConfig
  minVersionCode: 1
```
This unblocks the version gate (`AppViewModel.checkVersionGate()` reads
`appConfig/minVersionCode` on every app start; if the installed `BuildConfig.VERSION_CODE` is
below this value, the user sees a non-dismissable "update required" screen). If RTDB is
unreachable at startup, the gate is skipped so offline use isn't blocked. See the README's "How
to release a new version" section for how to bump this value on future releases.

## 5. Deploy RTDB security rules

The rules live in [`firebase/database.rules.json`](../firebase/database.rules.json) in this repo
— that file is the source of truth; keep the deployed rules in sync with it on every change.

```bash
firebase login
firebase use <your-project-id>
firebase deploy --only database
```

Or paste the file's contents directly into **Realtime Database → Rules** in the console and
click Publish.

**Verify the rules took effect:**
- Unauthenticated REST read should be denied:
  ```bash
  curl "https://<project-id>-default-rtdb.<region>.firebasedatabase.app/bills.json"
  # expect: {"error":"Permission denied"}
  ```
- In the Rules **Simulator** tab, test a write of `bills/{id}/status` from `"PAID"` to `"OPEN"`
  authenticated as a non-owner — the app intentionally has no status-regression rule written into
  `database.rules.json` itself (that protection currently lives client-side in `ConflictResolver`,
  which rejects incoming remote writes that would regress a bill from PAID/VOID back to OPEN). If
  you need server-side enforcement of this invariant too, extend the rules with a `.validate`
  expression on `bills/$id/status`.

## Summary checklist

- [ ] `google-services.json` placed at `app/google-services.json`
- [ ] Email/Password auth enabled
- [ ] Realtime Database enabled
- [ ] Owner + staff Auth accounts created
- [ ] Custom claims (`role: owner` / `role: staff`) set for each account
- [ ] `appConfig/minVersionCode` seeded to `1`
- [ ] `firebase/database.rules.json` deployed
- [ ] Verified: owner sees Reports/Dashboard/Settings; staff does not
