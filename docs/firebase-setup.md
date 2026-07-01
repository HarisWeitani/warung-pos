# Firebase Setup

The app has **no user-facing login**. Access is protected by a local username + PIN chosen on
first launch (stored encrypted on the device). Firebase is still used for cross-device Realtime
Database sync; the app signs in **anonymously** behind the scenes so RTDB access passes the
`auth != null` security rule. Do these steps in order.

## 1. Create the Firebase project and Android app

1. In the [Firebase console](https://console.firebase.google.com/), create a project (or use an
   existing one).
2. Add an Android app with package name `com.wfx.warungpos`.
3. Download `google-services.json` and place it at `app/google-services.json` in this repo
   (gitignored — never commit it).
4. Enable **Realtime Database** (choose a region close to the store).

## 2. Enable Anonymous authentication

**Authentication → Sign-in method → Anonymous → Enable.**

This is the only auth provider the app needs. On startup the app calls `signInAnonymously()` so
that RTDB reads/writes are authenticated. There are no email/password accounts and no per-user
roles — the app is single-user with full access, gated locally by the PIN.

If this provider is left disabled, the app still runs and works fully **offline/local** (Room is
the source of truth), but writes stay `PENDING` and never sync to other devices, and the version
gate check falls back to "allowed".

## 3. Seed `appConfig`

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

## 4. Deploy RTDB security rules

The rules live in [`firebase/database.rules.json`](../firebase/database.rules.json) in this repo
— that file is the source of truth; keep the deployed rules in sync with it on every change.
Every path allows read/write to any authenticated (anonymous) user; there are no role checks.

```bash
firebase login
firebase use <your-project-id>
firebase deploy --only database
```

Or paste the file's contents directly into **Realtime Database → Rules** in the console and
click Publish.

**Verify the rules took effect:** an unauthenticated REST read should still be denied (the app
authenticates anonymously; a raw `curl` is not authenticated):
```bash
curl "https://<project-id>-default-rtdb.<region>.firebasedatabase.app/bills.json"
# expect: {"error":"Permission denied"}
```
The client-side `ConflictResolver` still guards against status regressions (a remote write can't
move a bill from PAID/VOID back to OPEN). If you want that invariant enforced server-side too,
add a `.validate` expression on `bills/$id/status`.

## First launch on the device

On first launch the app shows a **Set up your PIN** screen: enter a username and create a PIN
(minimum 4 digits). That username is stamped on records this device creates (`createdBy`,
`openedBy`, etc.) and shown in More. Subsequent launches show a PIN-only unlock screen. "Lock
App" in More returns to the PIN screen without clearing the stored username/PIN.

## Summary checklist

- [ ] `google-services.json` placed at `app/google-services.json`
- [ ] Realtime Database enabled
- [ ] Anonymous sign-in provider enabled
- [ ] `appConfig/minVersionCode` seeded to `1`
- [ ] `firebase/database.rules.json` deployed
- [ ] First launch: set username + PIN; relaunch prompts for PIN only
